package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.entity.TeleporterBlockEntity;
import name.blockrooms.mixin.StructureTemplateMixin;
import name.blockrooms.util.ModLevels;
import name.blockrooms.world.structure.BlockLevel1BetaBasePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * BlockLevel 1 generator ported from the 1.21.11 branch ({@code the-blockrooms}
 * 0.1.12): the level is a giant stack of small template rooms placed at chunk
 * resolution, with rare 2×2 / 3×3 themed regions, elevator shafts and exit
 * teleporter rooms.
 * <p>
 * 移植说明：1.21.11 用 {@code BaseBlockLevelGenerator}，26.2 等价骨架是
 * {@link SimpleGenerator}（getGenDepth=256 / getSeaLevel=0 / getMinY=0）；模板放置改在
 * {@link #applyBiomeDecoration} 阶段（与结构同阶段、同一写入路径），每区块只写落在本区块内的
 * 方块，避免改动相邻区块。模板的 palettes / entityInfoList 在 26.2 是私有字段，
 * 通过 {@link StructureTemplateMixin} 访问。
 * <p>
 * 26.2 特有的「石英 β 基地」（quartz_beta_base 结构 + {@link BlockLevel1BetaBasePiece}）
 * 不属于 1.21.11 内容，但结构和存档兼容需要保留：生成器在基地包围盒内跳过所有模板方块，
 * 避免新生成的房间把基地覆盖掉。
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
    public static @Nullable BlockPos shaftCenter(long seed, int segment, int chunkX, int chunkZ) {
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
    private static void fillGallerySide(WorldGenLevel level, BlockPos origin, int baseY, boolean xAxis,
                                        BoundingBox baseBox) {
        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        int base = (xAxis ? origin.getX() : origin.getZ()) - 5; // 模板偏移前的起点
        for (int strip : new int[]{0, 1, 2, 3, 4, 11, 12, 13, 14, 15}) {
            for (int d = 0; d < 16; d++) {
                for (int y = 0; y < 7; y++) {
                    BlockPos p = xAxis
                            ? new BlockPos(base + strip, baseY + y, origin.getZ() + d)
                            : new BlockPos(origin.getX() + d, baseY + y, base + strip);
                    if (baseBox.isInside(p)) continue;
                    level.setBlock(p, calcite, Block.UPDATE_NONE);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 26.2 石英 β 基地：位置必须与原 26.2 版本完全一致（结构/存档兼容）
    // ------------------------------------------------------------------

    private static final int BASE_RANGE = 128 * 16;
    private static final long BASE_SEED_XOR = 0x7F3C9E21A4B5D6E7L;

    /** 由维度种子派生的唯一「石英β基地」位置，限定在世界中心 128 个区块（2048 格）内 */
    public static BlockPos betaBaseCenter(long seed) {
        var random = net.minecraft.util.RandomSource.create(seed ^ BASE_SEED_XOR);
        int x = random.nextInt(BASE_RANGE * 2) - BASE_RANGE;
        int z = random.nextInt(BASE_RANGE * 2) - BASE_RANGE;
        return new BlockPos(x, 1, z);
    }

    /** 基地占据的生成范围（y 0..FLOOR_HEIGHT），模板房间在此范围内跳过，避免覆盖基地 */
    public static BoundingBox betaBaseBox(long seed) {
        BlockPos center = betaBaseCenter(seed);
        return BoundingBox.fromCorners(
                center.offset(-BlockLevel1BetaBasePiece.HALF, 0, -BlockLevel1BetaBasePiece.HALF),
                center.offset(BlockLevel1BetaBasePiece.HALF, 8, BlockLevel1BetaBasePiece.HALF));
    }

    // ------------------------------------------------------------------
    // 模板放置
    // ------------------------------------------------------------------

    private StructureTemplate loadTemplate(WorldGenLevel level, Identifier tpl) {
        try {
            StructureTemplate t = level.getServer().getStructureManager().get(tpl).orElse(null);
            if (t == null) {
                Blockrooms.LOGGER.warn("BL1-TPL: template {} NOT FOUND", tpl);
            }
            return t;
        } catch (Exception e) {
            Blockrooms.LOGGER.error("BL1-TPL: failed to load template {}: {}", tpl, e.toString());
            return null;
        }
    }

    /**
     * 手工放置一个模板：只写落在当前区块内的方块（避免跨区块写入导致的 Stage 冲突），
     * 方块实体 NBT 无条件加载（空容器补 room loot），实体按模板 NBT 生成。
     */
    private void placeTemplateLocal(WorldGenLevel level, ChunkAccess chunk, Identifier tplId,
                                    BlockPos origin, Rotation rotation) {
        StructureTemplate tpl = loadTemplate(level, tplId);
        if (tpl == null) return;
        StructureTemplateMixin access = (StructureTemplateMixin) (Object) tpl;
        List<Palette> palettes = access.getPalettes();
        if (palettes.isEmpty()) return;

        ChunkPos cp = chunk.getPos();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        BlockPos zero = tpl.getZeroPositionWithTransform(origin, Mirror.NONE, rotation);
        List<StructureBlockInfo> infos = palettes.getFirst().blocks();
        BoundingBox baseBox = betaBaseBox(level.getSeed());
        int placed = 0;
        for (StructureBlockInfo info : infos) {
            BlockPos p = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(zero);
            if (p.getX() >> 4 != cp.x() || p.getZ() >> 4 != cp.z()) continue;
            if (p.getY() < 0 || p.getY() >= getGenDepth()) continue;
            if (baseBox.isInside(p)) continue;
            BlockState state = info.state().rotate(rotation);
            level.setBlock(p, state, Block.UPDATE_NONE);
            placed++;
            if (info.nbt() != null && state.getBlock() instanceof EntityBlock eb) {
                // 无条件加载模板 NBT：即使 setBlock 已创建空 BE 也覆盖加载，避免楼层/顺序差异导致容器为空
                BlockEntity be = level.getBlockEntity(p);
                if (be == null) {
                    be = eb.newBlockEntity(p, state);
                    if (be != null) {
                        chunk.setBlockEntity(be);
                    }
                }
                if (be != null) {
                    be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                            level.getLevel().registryAccess(), info.nbt()));
                    // 模板容器带空 NBT（如 48 展厅的箱子）：按房间类型补 loot
                    if (be instanceof RandomizableContainerBlockEntity container
                            && info.nbt().getListOrEmpty("Items").isEmpty()) {
                        container.setLootTable(isRareTemplate(tplId) ? BL1_LOOT_RARE : BL1_LOOT_COMMON);
                        container.setLootTableSeed(level.getSeed() ^ p.asLong());
                    }
                }
            }
        }
        Blockrooms.LOGGER.debug("BL1-TPL: {} origin={} zero={} rot={} infos={} placed={}",
                tplId, origin, zero, rotation, infos.size(), placed);

        for (StructureEntityInfo e : access.getEntityInfoList()) {
            // 1.21.11 原实现漏了 zero 偏移（transformedVec3d 只做旋转/镜像），导致模板实体几乎都生成在原点附近；
            // 这里补上模板原点偏移，实体才会落在模板实际位置上。
            Vec3 world = StructureTemplate.transform(e.pos, settings.getMirror(), settings.getRotation(),
                            settings.getRotationPivot())
                    .add(zero.getX(), zero.getY(), zero.getZ());
            int ex = Mth.floor(world.x) >> 4;
            int ez = Mth.floor(world.z) >> 4;
            if (ex != cp.x() || ez != cp.z()) continue;
            if (world.y < 0 || world.y >= getGenDepth()) continue;
            // 模板 NBT 里保存了原实体的 UUID；同一模板被多次放置时会重复 UUID（服务器报
            // "UUID of added entity already exists"），这里去掉后由实体自身生成新 UUID。
            CompoundTag entityNbt = e.nbt.copy();
            entityNbt.remove("UUID");
            // 模板 NBT 里的 Pos 也是保存结构时的绝对坐标；而 BlockAttachedEntity 会在加载时
            // 用 block_pos 与自身 Pos 做距离校验，所以 Pos 必须先覆盖成变换后的世界坐标。
            ListTag posTag = new ListTag();
            posTag.add(DoubleTag.valueOf(world.x));
            posTag.add(DoubleTag.valueOf(world.y));
            posTag.add(DoubleTag.valueOf(world.z));
            entityNbt.put("Pos", posTag);
            // 模板 NBT 里 BlockAttachedEntity（物品展示框/画）的 block_pos 是保存结构时的绝对坐标，
            // 直接放到别处会被判定为 "Block-attached entity at invalid position" 并丢弃挂接信息；
            // 这里用模板内的相对 blockPos 经旋转/镜像/原点变换后覆盖它。
            if (entityNbt.contains("block_pos")) {
                BlockPos attached = StructureTemplate.transform(e.blockPos, settings.getMirror(),
                        settings.getRotation(), settings.getRotationPivot()).offset(zero);
                BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, attached).result()
                        .ifPresent(tag -> entityNbt.put("block_pos", tag));
            }
            EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.getLevel().registryAccess(), entityNbt),
                            level.getLevel(), new EntitySpawnRequest(EntitySpawnReason.STRUCTURE, false))
                    .ifPresent(entity -> {
                        // 与 StructureTemplate#addEntitiesToWorld 一致：按模板旋转/镜像调整朝向后再落位
                        float yRot = entity.rotate(settings.getRotation());
                        yRot += entity.mirror(settings.getMirror()) - entity.getYRot();
                        entity.snapTo(world.x, world.y, world.z, yRot, entity.getXRot());
                        entity.setYBodyRot(yRot);
                        entity.setYHeadRot(yRot);
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
        BoundingBox baseBox = betaBaseBox(seed);
        for (int floor = 0; floor < FLOORS; floor++) {
            int baseY = floor * FLOOR_HEIGHT;
            try {
                RoomType type = roomType(seed, floor, cp.x(), cp.z());
                if (type == RoomType.EXIT) {
                    placeExitRoom(level, chunk, cp, baseY, baseBox);
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
                    fillGallerySide(level, origin, baseY, rotation == Rotation.CLOCKWISE_90, baseBox);
                }
                placeTemplateLocal(level, chunk, tpl, origin, rotation);
            } catch (Exception e) {
                Blockrooms.LOGGER.error("BL1-GEN: chunk {},{} floor {} EXCEPTION: {}", cp.x(), cp.z(), floor, e.toString());
                for (StackTraceElement el : e.getStackTrace()) {
                    if (el.getClassName().startsWith("name.blockrooms")) {
                        Blockrooms.LOGGER.error("  at {}", el);
                    }
                }
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

    private void placeExitRoom(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, int baseY, BoundingBox baseBox) {
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        int top = baseY + TPL_HEIGHT - 1;
        Blockrooms.LOGGER.debug("BL1-GEN: EXIT room chunk {},{} y={}", cp.x(), cp.z(), baseY);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos floorPos = new BlockPos(minX + x, baseY, minZ + z);
                BlockPos topPos = new BlockPos(minX + x, top, minZ + z);
                if (!baseBox.isInside(floorPos)) {
                    level.setBlock(floorPos, Blocks.QUARTZ_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
                }
                if (!baseBox.isInside(topPos)) {
                    level.setBlock(topPos, Blocks.QUARTZ_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
                }
                boolean wall = x == 0 || x == 15 || z == 0 || z == 15;
                for (int y = 1; y < TPL_HEIGHT - 1; y++) {
                    BlockPos p = new BlockPos(minX + x, baseY + y, minZ + z);
                    if (baseBox.isInside(p)) continue;
                    level.setBlock(p,
                            wall ? Blocks.QUARTZ_BRICKS.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState(),
                            Block.UPDATE_NONE);
                }
            }
        }
        placeTemplateLocal(level, chunk, TPL_EXIT, new BlockPos(minX, baseY, minZ + 4), Rotation.NONE);
        for (int y = 1; y <= 3; y++) {
            for (int z = 6; z <= 7; z++) {
                BlockPos tp = new BlockPos(minX, baseY + y, minZ + z);
                if (baseBox.isInside(tp)) continue;
                // 模板 NBT 里的传送器 targets 是空的，这里统一指向 BLOCKLEVEL_2（出口）。
                TeleporterBlockEntity tpBe = teleporterAt(level, tp);
                if (tpBe != null) {
                    tpBe.setTargets(List.of(new TeleporterBlockEntity.Target(ModLevels.BLOCKLEVEL_2, null)));
                    tpBe.setChanged();
                }
            }
        }
    }

    /**
     * 取（或创建）某位置的传送器方块实体。worldgen 阶段区块还是 ProtoChunk，
     * 必须通过当前区块取 BE；直接调 ServerLevel.getBlockEntity(pos) 会因为该区块尚未加入
     * 服务器而拿到 null（或另建一个新 chunk），targets 写不进去。
     */
    private static @Nullable TeleporterBlockEntity teleporterAt(WorldGenLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TeleporterBlockEntity tpBe) {
            return tpBe;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EntityBlock)) {
            state = ModBlocks.TELEPORTER_BLOCK.get().defaultBlockState();
            level.setBlock(pos, state, Block.UPDATE_NONE);
        }
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            return null;
        }
        BlockEntity created = entityBlock.newBlockEntity(pos, state);
        if (created == null) {
            return null;
        }
        level.getChunk(pos).setBlockEntity(created);
        return created instanceof TeleporterBlockEntity tpBe ? tpBe : null;
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
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(net.minecraft.world.level.levelgen.blending.Blender blender,
                                                        RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }
}
