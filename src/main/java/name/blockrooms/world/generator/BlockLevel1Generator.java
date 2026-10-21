package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.entity.TeleporterBlockEntity;
import name.blockrooms.mixin.StructureTemplateMixin;
import name.blockrooms.util.ModLevels;
import name.blockrooms.util.TeleporterApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * BlockLevel 1「石英回廊」生成器 —— 模板拼块式。
 *
 * <p>本文件从旧版（Minecraft 1.21.11 分支 `the-blockrooms`）的
 * {@code BlockLevel1Generator} 移植而来：世界由 NBT 结构模板按「区域(16/32/48) + 楼层(每层 7 格)
 * + 房间类型(大厅/金库/通廊/画廊/出口)」确定性拼装，另有一套 4×4 区块网格的电梯竖井。
 * 移植到 26.2 时的改动见各处的 {@code [PORT]} 注释。</p>
 */
public class BlockLevel1Generator extends SimpleGenerator {
    public static final MapCodec<BlockLevel1Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel1Generator::getBiomeSource)
            ).apply(instance, BlockLevel1Generator::new)
    );

    /** BL1 普通房间补给 loot（常见为主，混少量罕见/稀有） */
    public static final ResourceKey<LootTable> BL1_LOOT_COMMON = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "gameplay/blocklevel1_common"));
    /** BL1 稀有房间 loot（只出稀有及以上：铁锭/金粒/杏仁奶桶/金胡萝卜/陶片/红宝石） */
    public static final ResourceKey<LootTable> BL1_LOOT_RARE = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "gameplay/blocklevel1_rare"));

    public static final int FLOOR_HEIGHT = 7;
    public static final int TPL_HEIGHT = 7;
    public static final int TPL_HEIGHT_48 = 8;
    public static final int FLOORS = 256 / FLOOR_HEIGHT;
    public static final int FLOORS_48 = 256 / TPL_HEIGHT_48;

    public static final int REGION_SIZE = 24;
    /** 48 区域概率 1/4096（原 1/64 的平方）；32 区域约 1/64（原 1/8 的平方） */
    public static final int REGION_DENOM_48 = 4096;
    public static final int REGION_DENOM_32 = 64;

    public static final int EXIT_DENOM = 2048;
    public static final double LOBBY_CHANCE = 0.70;
    public static final double VAULT_CHANCE = 0.80;
    /** 通廊中约 1/9 为画廊（原 1/3 的平方，成对出现） */
    public static final int GALLERY_DENOM = 9;

    public static final int SHAFT_GRID = 4;
    public static final int FLOORS_PER_SHAFT = 3;
    public static final int SHAFT_SEG_HEIGHT = FLOORS_PER_SHAFT * FLOOR_HEIGHT; // 21

    private static final Identifier TPL_LOBBY_PILLAR = id("bl1_quartz_lobby_pillar");
    private static final Identifier TPL_LOBBY_FOUNTAIN = id("bl1_quartz_lobby_fountain");
    private static final Identifier TPL_LOBBY_LIBRARY = id("bl1_quartz_lobby_library");
    private static final Identifier TPL_PLANT_ROOM = id("bl1_plant_room");
    private static final Identifier TPL_LOBBY_PILLAR_32 = id("bl1_quartz_lobby_pillar_32");
    private static final Identifier TPL_LOBBY_ARMOR_STAND_32 = id("bl1_quartz_lobby_armor_stand_32");
    private static final Identifier TPL_LOBBY_EMPTY_32 = id("bl1_quartz_lobby_empty_32");
    private static final Identifier TPL_CORRIDOR_SPRUCE = id("bl1_quartz_corridor_spruce");
    private static final Identifier TPL_CORRIDOR_IRON = id("bl1_quartz_corridor_iron");
    private static final Identifier TPL_GALLERY = id("bl1_calcite_gallery");
    private static final Identifier TPL_CAMP = id("bl1_abandoned_camp");
    private static final Identifier TPL_UNMANNED = id("bl1_unmanned_quartz_base");
    private static final Identifier TPL_ELEVATOR = id("bl1_elevator_room");
    private static final Identifier TPL_EXIT = id("bl1_concrete_corridor");
    private static final Identifier TPL_EXHIBIT = id("bl1_quartz_exhibition_hall");

    private static final Identifier[] LOBBY_16 = {TPL_LOBBY_PILLAR, TPL_LOBBY_FOUNTAIN, TPL_LOBBY_LIBRARY, TPL_PLANT_ROOM};
    private static final Identifier[] LOBBY_32 = {TPL_LOBBY_PILLAR_32, TPL_LOBBY_ARMOR_STAND_32, TPL_LOBBY_EMPTY_32};
    private static final Identifier[] CORRIDOR = {TPL_CORRIDOR_SPRUCE, TPL_CORRIDOR_IRON};
    private static final Identifier[] VAULT = {TPL_CAMP, TPL_UNMANNED};

    public enum RoomType { LOBBY, VAULT, CORRIDOR, GALLERY, EXIT }

    public enum RegionType { R16, R32, R48 }

    public BlockLevel1Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, path);
    }

    /** 稀有房间模板（空容器用稀有 loot）：画廊、植物房、喷泉、图书馆、32 变种、48 展厅 */
    private static boolean isRareTemplate(Identifier tpl) {
        return tpl.equals(TPL_GALLERY) || tpl.equals(TPL_PLANT_ROOM)
                || tpl.equals(TPL_LOBBY_FOUNTAIN) || tpl.equals(TPL_LOBBY_LIBRARY)
                || tpl.equals(TPL_LOBBY_PILLAR_32) || tpl.equals(TPL_LOBBY_ARMOR_STAND_32)
                || tpl.equals(TPL_LOBBY_EMPTY_32) || tpl.equals(TPL_EXHIBIT);
    }

    private static long hash(long seed, int a, int b, long salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xBF58476D1CE4E5B9L) ^ salt;
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return (h ^ (h >>> 32)) & Long.MAX_VALUE;
    }

    private static long hash3(long seed, int a, int b, int c, long salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xBF58476D1CE4E5B9L) ^ (c * 0x165667B19E3779F9L) ^ salt;
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return (h ^ (h >>> 32)) & Long.MAX_VALUE;
    }


    public static RegionType regionType(long seed, int regionX, int regionZ) {
        long h = hash(seed, regionX, regionZ, 0x1B1L);
        if (h % REGION_DENOM_48 == 0) return RegionType.R48;
        if (h % REGION_DENOM_32 == 0) return RegionType.R32;
        return RegionType.R16;
    }

    public static RoomType roomType(long seed, int floor, int chunkX, int chunkZ) {
        if (hash(seed, chunkX, chunkZ, 0x1B2L) % EXIT_DENOM == 0) return RoomType.EXIT;
        long h = hash3(seed, floor, chunkX, chunkZ, 0x1B4L);
        double r = (h & 0xFFFF) / 65536.0;
        if (r < LOBBY_CHANCE) return RoomType.LOBBY;
        if (r < VAULT_CHANCE) return RoomType.VAULT;
        if (isGallery(seed, floor, chunkX, chunkZ)
                && isGallery(seed, floor, chunkX + 1, chunkZ)) {
            return RoomType.GALLERY;
        }
        return RoomType.CORRIDOR;
    }

    private static boolean isGallery(long seed, int floor, int chunkX, int chunkZ) {
        return hash3(seed, floor, chunkX, chunkZ, 0x1B5L) % GALLERY_DENOM == 0;
    }

    private static Identifier pickTemplate(long seed, RoomType type, int floor, int chunkX, int chunkZ) {
        return switch (type) {
            case LOBBY -> {
                // 柱厅为普通大厅（13/16）；喷泉/图书馆/植物房为稀有变种（各 1/16，原 1/4 的平方）
                long h = hash3(seed, floor, chunkX, chunkZ, 0x1B6L);
                int r = (int) (h % 16);
                if (r < 13) yield TPL_LOBBY_PILLAR;
                yield LOBBY_16[1 + (r - 13)]; // fountain / library / plant_room
            }
            case VAULT -> VAULT[(int) (hash3(seed, floor, chunkX, chunkZ, 0x1B7L) % VAULT.length)];
            case CORRIDOR -> CORRIDOR[(int) (hash3(seed, floor, chunkX, chunkZ, 0x1B8L) % CORRIDOR.length)];
            case GALLERY -> TPL_GALLERY;
            case EXIT -> TPL_EXIT;
        };
    }


    /**
     * 电梯竖井宿主只选大厅（LOBBY，且非植物房）。
     * 竖井模板 9×22×9 会覆盖房间中央：营地/无人基地为实心或摆满家具的房间，
     * 被挖穿会呈现"只生成一部分"；大厅中庭被竖井替换则是预期设计（电梯在大厅中央）。
     * 植物房只有东西向开口，竖井会截断通道，也不选。
     * 4×4 网格内若无可用大厅（极罕见）返回 null，该段不放电梯。
     */
    public static BlockPos shaftCenter(long seed, int segment, int chunkX, int chunkZ) {
        int gx = Math.floorDiv(chunkX, SHAFT_GRID);
        int gz = Math.floorDiv(chunkZ, SHAFT_GRID);
        long base = hash3(seed, gx, gz, segment, 0x1B9L);
        for (int attempt = 0; attempt < SHAFT_GRID * SHAFT_GRID; attempt++) {
            long h = base + attempt * 0x9E3779B97F4A7C15L;
            int cx = gx * SHAFT_GRID + (int) ((h >>> 16) % SHAFT_GRID);
            int cz = gz * SHAFT_GRID + (int) ((h >>> 32) % SHAFT_GRID);
            boolean usable = true;
            for (int f = 0; f < FLOORS_PER_SHAFT; f++) {
                int fl = segment * FLOORS_PER_SHAFT + f;
                if (fl >= FLOORS) break;
                RoomType t = roomType(seed, fl, cx, cz);
                if (t != RoomType.LOBBY
                        || pickTemplate(seed, t, fl, cx, cz).equals(TPL_PLANT_ROOM)) {
                    usable = false;
                    break;
                }
            }
            if (usable) return new BlockPos(cx, 0, cz);
        }
        return null;
    }

    /**
     * 2 向房间（通廊/画廊/植物房/营地）的朝向选择：
     * 1) 自然轴上同型邻居成链 → 保持自然朝向（链条贯通，开口相对）；
     * 2) 否则按四邻可开口性打分，选连通面更多的朝向；
     * 3) 平局回退自然朝向。
     * 自然朝向：通廊/画廊/植物房为东西向（NONE），营地（实心+南北门）为南北向（NONE）。
     * 注意：旋转映射按 naturalX 区分——营地要开东西向需 CLOCKWISE_90，开南北向保持 NONE。
     */
    private static Rotation pickRotation(long seed, int floor, int chunkX, int chunkZ,
                                         RoomType type, boolean naturalX) {
        if (type != RoomType.LOBBY && chainOnAxis(seed, floor, chunkX, chunkZ, type, naturalX)) {
            return Rotation.NONE; // 链保持自然朝向（所有模板自然朝向均为 NONE）
        }
        int scoreX = axisScore(seed, floor, chunkX, chunkZ, true);
        int scoreZ = axisScore(seed, floor, chunkX, chunkZ, false);
        if (scoreX > scoreZ) return naturalX ? Rotation.NONE : Rotation.CLOCKWISE_90;     // 开东西向
        if (scoreZ > scoreX) return naturalX ? Rotation.CLOCKWISE_90 : Rotation.NONE;     // 开南北向
        return Rotation.NONE; // 平局回退自然朝向
    }

    /** 自然轴上（东西方向传 xAxis=true，南北传 false）是否有同型房间邻居可成链 */
    private static boolean chainOnAxis(long seed, int floor, int chunkX, int chunkZ,
                                       RoomType type, boolean xAxis) {
        for (int sign = -1; sign <= 1; sign += 2) {
            int nx = chunkX + (xAxis ? sign : 0);
            int nz = chunkZ + (xAxis ? 0 : sign);
            if (roomType(seed, floor, nx, nz) == type) return true;
        }
        return false;
    }

    /**
     * 若本房间取某朝向（xAxis=true 为东西向），该轴两端邻居中有多少能对上开口。
     * 只给确定性得分：4 向房间（大厅/无人基地）必连通 +2；
     * 2 向房间只在其自然朝向与该轴匹配时 +2（走廊/画廊/植物房自然东西向，营地自然南北向）。
     * 不给"异向邻居可能旋转过来"的不确定分——模拟显示那反而增加死路。
     * 出口房间(实心盒)不计分。
     */
    private static int axisScore(long seed, int floor, int chunkX, int chunkZ, boolean xAxis) {
        int score = 0;
        for (int sign = -1; sign <= 1; sign += 2) {
            int nx = chunkX + (xAxis ? sign : 0);
            int nz = chunkZ + (xAxis ? 0 : sign);
            RoomType t = roomType(seed, floor, nx, nz);
            switch (t) {
                case LOBBY -> {
                    // 植物房 2 向（自然东西向）；其余大厅 4 向
                    if (!pickTemplate(seed, t, floor, nx, nz).equals(TPL_PLANT_ROOM)) {
                        score += 2;
                    } else if (xAxis) {
                        score += 2;
                    }
                }
                case VAULT -> {
                    // 无人基地 4 向；营地南北向自然
                    if (pickTemplate(seed, t, floor, nx, nz).equals(TPL_UNMANNED)) {
                        score += 2;
                    } else if (!xAxis) {
                        score += 2;
                    }
                }
                case CORRIDOR, GALLERY -> {
                    if (xAxis) score += 2;
                }
                default -> { }
            }
        }
        return score;
    }

    /**
     * 画廊模板只有 6 格深（16×6×6），居中偏移后把区块内两侧空隙
     * （z=0..4 与 z=11..15，或旋转后 x 方向）补成方解石，避免侧面漏空成虚空。
     */
    private static void fillGallerySide(WorldGenLevel level, BlockPos origin, int baseY, boolean xAxis) {
        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        int base = (xAxis ? origin.getX() : origin.getZ()) - 5; // 模板偏移前的起点
        for (int strip : new int[]{0, 1, 2, 3, 4, 11, 12, 13, 14, 15}) {
            for (int d = 0; d < 16; d++) {
                for (int y = 0; y < 7; y++) {
                    BlockPos p = xAxis
                            ? new BlockPos(base + strip, baseY + y, origin.getZ() + d)
                            : new BlockPos(origin.getX() + d, baseY + y, base + strip);
                    level.setBlock(p, calcite, Block.UPDATE_NONE);
                }
            }
        }
    }


    private StructureTemplate loadTemplate(WorldGenLevel level, Identifier tpl) {
        try {
            StructureTemplate t = level.getLevel().getServer().getStructureManager().get(tpl).orElse(null);
            if (t == null) {
                Blockrooms.LOGGER.warn("BL1-TPL: template {} NOT FOUND", tpl);
            }
            return t;
        } catch (Exception e) {
            Blockrooms.LOGGER.error("BL1-TPL: failed to load template {}: {}", tpl, e.toString());
            return null;
        }
    }

    private void placeTemplateLocal(WorldGenLevel level, ChunkAccess chunk, Identifier tplId, BlockPos origin, Rotation rotation) {
        StructureTemplate tpl = loadTemplate(level, tplId);
        if (tpl == null) return;
        // [PORT] 26.2 起 StructureTemplate.palettes 为 private（1.21.11 是 public），改走 accessor。
        List<StructureTemplate.Palette> palettes = ((StructureTemplateMixin) tpl).getPalettes();
        if (palettes.isEmpty()) return;
        ChunkPos cp = chunk.getPos();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation);
        BlockPos zero = tpl.getZeroPositionWithTransform(origin, Mirror.NONE, rotation);
        List<StructureBlockInfo> infos = palettes.getFirst().blocks();
        Blockrooms.LOGGER.debug("BL1-TPL: {} origin={} zero={} rot={} infos={}",
                tplId, origin, zero, rotation, infos.size());
        int placed = 0;
        for (StructureBlockInfo info : infos) {
            BlockPos p = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(zero);
            if (p.getX() >> 4 != cp.x() || p.getZ() >> 4 != cp.z()) continue;
            if (p.getY() < 0 || p.getY() >= 256) continue;
            BlockState state = info.state().rotate(rotation);
            level.setBlock(p, state, Block.UPDATE_NONE);
            placed++;
            if (info.nbt() != null && state.getBlock() instanceof EntityBlock eb) {
                // 无条件加载模板 NBT：即使 setBlock 已创建空 BE 也覆盖加载，避免楼层/顺序差异导致容器为空
                BlockEntity be = level.getBlockEntity(p);
                if (be == null) {
                    be = eb.newBlockEntity(p, state);
                    if (be != null) {
                        level.getChunk(p).setBlockEntity(be);
                    }
                }
                if (be != null) {
                    be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), info.nbt()));
                    // 模板容器带空 NBT（如 48 展厅的箱子）：按房间类型补 loot
                    if (be instanceof RandomizableContainerBlockEntity container
                            && info.nbt().getListOrEmpty("Items").isEmpty()) {
                        container.setLootTable(isRareTemplate(tplId) ? BL1_LOOT_RARE : BL1_LOOT_COMMON);
                        container.setLootTableSeed(level.getSeed() ^ p.asLong());
                    }
                }
            }
        }
        Blockrooms.LOGGER.debug("BL1-TPL: {} placed in chunk {},{} = {}", tplId, cp.x(), cp.z(), placed);
        var s = (StructureTemplateMixin) tpl;
        for (StructureEntityInfo e : s.getEntityInfoList()) {
            Vec3 world = StructureTemplate.transformedVec3d(settings, e.pos);
            int ex = (int) Math.floor(world.x) >> 4;
            int ez = (int) Math.floor(world.z) >> 4;
            if (ex != cp.x() || ez != cp.z()) continue;
            if (world.y < 0 || world.y >= 256) continue;
            // [PORT] 26.2 的 3 参重载签名由 (ValueInput, Level, EntitySpawnReason)
            //        改为 (ValueInput, Level, EntitySpawnRequest)。
            EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), e.nbt),
                            level.getLevel(), new EntitySpawnRequest(EntitySpawnReason.STRUCTURE, false))
                    .ifPresent(entity -> {
                        entity.setPos(world.x, world.y, world.z);
                        level.addFreshEntity(entity);
                    });
        }
    }


    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();
        RegionType region = regionType(seed, Math.floorDiv(cp.x(), REGION_SIZE), Math.floorDiv(cp.z(), REGION_SIZE));
        Blockrooms.LOGGER.debug("BL1-GEN: chunk {},{} region={}", cp.x(), cp.z(), region);
        switch (region) {
            case R48 -> placeRegion48(level, chunk, cp, seed);
            case R32 -> placeRegion32(level, chunk, cp, seed);
            case R16 -> placeRegion16(level, chunk, cp, seed);
        }
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    private void placeRegion16(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed) {
        for (int floor = 0; floor < FLOORS; floor++) {
            try {
                int baseY = floor * FLOOR_HEIGHT;
                RoomType type = roomType(seed, floor, cp.x(), cp.z());
                if (type == RoomType.EXIT) {
                    placeExitRoom(level, chunk, cp, baseY);
                    continue;
                }
                Identifier tpl = pickTemplate(seed, type, floor, cp.x(), cp.z());
                // 朝向由四邻开口情况决定（见 pickRotation），避免"门对墙"死路。
                Rotation rotation = Rotation.NONE;
                boolean twoWay = type == RoomType.CORRIDOR || type == RoomType.GALLERY
                        || (type == RoomType.VAULT && tpl.equals(TPL_CAMP))
                        || (type == RoomType.LOBBY && tpl.equals(TPL_PLANT_ROOM));
                if (twoWay) {
                    boolean naturalX = !tpl.equals(TPL_CAMP);
                    rotation = pickRotation(seed, floor, cp.x(), cp.z(), type, naturalX);
                }
                BlockPos origin = new BlockPos(cp.getMinBlockX(), baseY, cp.getMinBlockZ());
                if (tpl.equals(TPL_GALLERY)) {
                    // 画廊模板 16×6×6，通道开口在 z=2,3：偏移 +5 使通道居中(z=7,8)与邻居开口对齐；
                    // 旋转 90° 后开口落在 x=2,3，偏移 +5 在 X。
                    origin = rotation == Rotation.NONE ? origin.offset(0, 0, 5) : origin.offset(5, 0, 0);
                    fillGallerySide(level, origin, baseY, rotation == Rotation.CLOCKWISE_90);
                }
                placeTemplateLocal(level, chunk, tpl, origin, rotation);
            } catch (Exception e) {
                Blockrooms.LOGGER.error("BL1-GEN: chunk {},{} floor {} EXCEPTION: {}", cp.x(), cp.z(), floor, e.toString());
                for (StackTraceElement el : e.getStackTrace()) {
                    if (el.getClassName().startsWith("name.blockrooms")) {
                        Blockrooms.LOGGER.error("  at {}", el);
                    }
                }
                throw e;
            }
        }
        int segments = (FLOORS + FLOORS_PER_SHAFT - 1) / FLOORS_PER_SHAFT;
        for (int segment = 0; segment < segments; segment++) {
            BlockPos center = shaftCenter(seed, segment, cp.x(), cp.z());
            if (center != null && center.getX() == cp.x() && center.getZ() == cp.z()) {
                int segY = segment * SHAFT_SEG_HEIGHT;
                placeTemplateLocal(level, chunk, TPL_ELEVATOR,
                        new BlockPos(cp.getMinBlockX() + 3, segY, cp.getMinBlockZ() + 3), Rotation.NONE);
            }
        }
        sealTop(level, cp, FLOORS * FLOOR_HEIGHT);
    }

    private void placeRegion32(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed) {
        int gx = Math.floorDiv(cp.x(), 2);
        int gz = Math.floorDiv(cp.z(), 2);
        BlockPos groupOrigin = new BlockPos(gx * 32, 0, gz * 32);
        for (int floor = 0; floor < FLOORS; floor++) {
            int baseY = floor * FLOOR_HEIGHT;
            Identifier tpl = LOBBY_32[(int) (hash3(seed, gx, gz, floor, 0x1BBL) % LOBBY_32.length)];
            placeTemplateLocal(level, chunk, tpl, groupOrigin.offset(0, baseY, 0), Rotation.NONE);
        }
        sealTop(level, cp, FLOORS * FLOOR_HEIGHT);
    }


    private void placeRegion48(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed) {
        int gx = Math.floorDiv(cp.x(), 3);
        int gz = Math.floorDiv(cp.z(), 3);
        BlockPos groupOrigin = new BlockPos(gx * 48, 0, gz * 48);
        for (int floor = 0; floor < FLOORS_48; floor++) {
            int baseY = floor * TPL_HEIGHT_48;
            placeTemplateLocal(level, chunk, TPL_EXHIBIT, groupOrigin.offset(0, baseY, 0), Rotation.NONE);
        }
        sealTop(level, cp, FLOORS_48 * TPL_HEIGHT_48);
    }

    private void placeExitRoom(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, int baseY) {
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        int top = baseY + TPL_HEIGHT - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(minX + x, baseY, minZ + z),
                        Blocks.QUARTZ_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
                level.setBlock(new BlockPos(minX + x, top, minZ + z),
                        Blocks.QUARTZ_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
                boolean wall = x == 0 || x == 15 || z == 0 || z == 15;
                for (int y = 1; y < TPL_HEIGHT - 1; y++) {
                    level.setBlock(new BlockPos(minX + x, baseY + y, minZ + z),
                            wall ? Blocks.QUARTZ_BRICKS.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState(),
                            Block.UPDATE_NONE);
                }
            }
        }
        placeTemplateLocal(level, chunk, TPL_EXIT, new BlockPos(minX, baseY, minZ + 4), Rotation.NONE);
        for (int y = 1; y <= 3; y++) {
            for (int z = 6; z <= 7; z++) {
                BlockPos tp = new BlockPos(minX, baseY + y, minZ + z);
                if (!(level.getBlockEntity(tp) instanceof TeleporterBlockEntity)) {
                    BlockState state = ModBlocks.TELEPORTER_BLOCK.get().defaultBlockState();
                    level.getChunk(tp).setBlockEntity(new TeleporterBlockEntity(tp, state));
                }
                TeleporterApi.setTarget(level.getLevel(), tp,
                        new TeleporterBlockEntity.Target(ModLevels.BLOCKLEVEL_2, null));
            }
        }
    }

    private static void sealTop(WorldGenLevel level, ChunkPos cp, int y) {
        if (y >= 256) return;
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        for (int yy = y; yy < 256; yy++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    level.setBlock(new BlockPos(cp.getMinBlockX() + x, yy, cp.getMinBlockZ() + z), bedrock, Block.UPDATE_NONE);
                }
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }
}
