package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.concurrent.CompletableFuture;

/**
 * BlockLevel 2「隧道」——<b>结构化生成</b>版本。
 *
 * <p>世界 = <b>16×16×6 结构模板拼块</b>（每个区块一个模板，y=0..5）：
 * <ul>
 *   <li>模板（放在 {@code data/blockrooms/structure/}，需搭建）：{@code bl2_corridor_x}（东西走廊）、
 *       {@code bl2_corner}（东南拐角）、{@code bl2_tjunction}（西/东/南 T 字）、
 *       {@code bl2_crossroads}（十字）、{@code bl2_dead_end}（西口死路）；
 *       朝向由代码按 Rotation 0/90/180/270 覆盖；</li>
 *   <li>四边隧道口的开/闭由<b>边哈希</b>决定（每边约 70% 开口）：边以「西/北区块坐标 + 东/南方向」
 *       唯一标识，相邻区块读同一条边 → 开口状态必然一致，迷宫自动连通、不会错位；</li>
 *   <li>模板在 <b>FEATURES 阶段</b>（{@link #applyBiomeDecoration}，与结构同一阶段、同一写入路径）放置，
 *       随后 super 继续生成门结构（云杉门/石英门，覆盖在模板之上）；</li>
 *   <li>模板内的箱子/潜影盒由代码统一写入 {@code blockrooms:gameplay/blocklevel2} 战利品表。</li>
 * </ul>
 *
 * <p>旧版 fillFromNoise 逐块迷宫逻辑已废弃移除；spawnOriginalMobs 置空（模板自包含）。</p>
 */
public class BlockLevel2Generator extends SimpleGenerator {

    public static final int FLOOR_Y = 0;
    public static final int CEILING_Y = 4;
    public static final int CAP_Y = CEILING_Y + 1;
    public static final int INTERIOR_MIN_Y = 1;
    public static final int INTERIOR_MAX_Y = 3;

    /** 模板尺寸：16×16×6（y=0..5，y=5 为基岩顶） */
    private static final int TPL_SIZE = 16;
    private static final int TPL_HEIGHT = 6;

    /** 每边隧道口的开口概率 */
    private static final double EDGE_OPEN_CHANCE = 0.7;

    /** 模板 id（用户搭建，data/blockrooms/structure/） */
    private static final Identifier TPL_CORRIDOR_X = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_corridor_x");
    private static final Identifier TPL_CORNER = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_corner");
    private static final Identifier TPL_TJUNCTION = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_tjunction");
    private static final Identifier TPL_CROSSROADS = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_crossroads");
    private static final Identifier TPL_DEAD_END = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl2_deadend");

    public static final MapCodec<BlockLevel2Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel2Generator::getBiomeSource)
            ).apply(instance, BlockLevel2Generator::new)
    );

    public static final ResourceKey<LootTable> BLOCKLEVEL2_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "chests/blocklevel2"));

    public BlockLevel2Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        placeWorldTemplate(level, chunk);
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    private void placeWorldTemplate(WorldGenLevel level, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();

        boolean north = edgeOpen(seed, cp.x(), cp.z(), Direction.NORTH);
        boolean east = edgeOpen(seed, cp.x(), cp.z(), Direction.EAST);
        boolean south = edgeOpen(seed, cp.x(), cp.z(), Direction.SOUTH);
        boolean west = edgeOpen(seed, cp.x(), cp.z(), Direction.WEST);

        int count = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);
        Identifier tpl;
        Rotation rotation = Rotation.NONE;
        switch (count) {
            case 4 -> tpl = TPL_CROSSROADS;
            case 3 -> {
                tpl = TPL_TJUNCTION;
                rotation = Rotation.values()[missingSide(north, east, south, west)];
            }
            case 2 -> {
                if (north && south) {
                    tpl = TPL_CORRIDOR_X;
                    rotation = Rotation.CLOCKWISE_90;
                } else if (east && west) {
                    tpl = TPL_CORRIDOR_X;
                } else {
                    // 拐角：模板基准为东南口（东=1、南=2），a 为两口中顺时针序靠前的一个
                    tpl = TPL_CORNER;
                    int a = north ? (east ? 0 : 3) : (east ? 1 : 2);
                    rotation = Rotation.values()[Math.floorMod(a - 1, 4)];
                }
            }
            case 1 -> {
                tpl = TPL_DEAD_END;
                rotation = Rotation.values()[Math.floorMod(singleSide(north, east, south, west) - 3, 4)];
            }
            default -> {
                tpl = TPL_DEAD_END;
                int d = Math.floorMod(hashChunk(seed, cp.x(), cp.z()), 4);
                rotation = Rotation.values()[Math.floorMod(d - 3, 4)];
            }
        }

        StructureTemplate template;
        try {
            template = level.getLevel().getServer().getStructureManager().get(tpl).orElse(null);
        } catch (Exception e) {
            Blockrooms.LOGGER.error("BL2-TPL: chunk {} failed to load template {}: {}", cp, tpl, e.toString());            template = null;
        }
        BlockPos origin = new BlockPos(cp.getMinBlockX(), 0, cp.getMinBlockZ());
        if (template == null) {
            // 模板缺失（尚未搭建/文件名不符）：实心石砖保底，避免玩家掉进虚空
            Blockrooms.LOGGER.warn("BL2-TPL: chunk {} template {} not found -> solid fallback", cp, tpl);
            fillFallback(chunk);
            return;
        }
        try {
            int placed = placeTemplateRotated(level, template, origin, rotation);
            Blockrooms.LOGGER.debug("BL2-TPL: chunk {} template {} rot={} placed={}", cp, tpl, rotation, placed);
        } catch (Exception e) {
            Blockrooms.LOGGER.error("BL2-TPL: chunk {} template {} placement failed", cp, tpl, e);
            fillFallback(chunk);
        }
        applyLoot(level, origin);
    }

    /**
     * 放置模板：placeInWorld 的旋转绕世界原点 (0,0,0)，旋转后模板坐标变负、整体偏移到
     * 相邻区块。这里按旋转方向补偿 offset（+15），使旋转后的 16×16 区域仍精确覆盖
     * [origin, origin+15]；方块状态旋转、方块实体、空气清理由 placeInWorld 统一处理。
     * 返回放置的方块数（placeInWorld 返回值）。
     */
    private static int placeTemplateRotated(WorldGenLevel level, StructureTemplate template, BlockPos origin, Rotation rotation) {
        BlockPos offset = switch (rotation) {
            case CLOCKWISE_90 -> origin.offset(15, 0, 0);
            case CLOCKWISE_180 -> origin.offset(15, 0, 15);
            case COUNTERCLOCKWISE_90 -> origin.offset(0, 0, 15);
            default -> origin;
        };
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        boolean placed = template.placeInWorld(level, offset, origin, settings, level.getRandom(), Block.UPDATE_NONE);
        return placed ? 1 : 0;
    }

    /**
     * 边开口：边由「西/北区块坐标 + 东/南方向」唯一标识——本区块的西边 = 西邻区块的东边，
     * 北边 = 北邻区块的南边，两侧区块读同一条边 → 开口状态一致。
     */
    private static boolean edgeOpen(long seed, int chunkX, int chunkZ, Direction dir) {
        int ex = chunkX;
        int ez = chunkZ;
        if (dir == Direction.WEST) {
            ex = chunkX - 1;
            dir = Direction.EAST;
        } else if (dir == Direction.NORTH) {
            ez = chunkZ - 1;
            dir = Direction.SOUTH;
        }
        long h = seed ^ (ex * 0x9E3779B97F4A7C15L) ^ (ez * 0xC2B2AE3D27D4EB4FL) ^ (dir.get2DDataValue() * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return ((h ^ (h >>> 32)) & 0xFFFF) / 65536.0 < EDGE_OPEN_CHANCE;
    }

    private static int missingSide(boolean north, boolean east, boolean south, boolean west) {
        if (!north) return 0;
        if (!east) return 1;
        if (!south) return 2;
        return 3;
    }

    private static int singleSide(boolean north, boolean east, boolean south, boolean west) {
        if (north) return 0;
        if (east) return 1;
        if (south) return 2;
        return 3;
    }

    private static long hashChunk(long seed, int chunkX, int chunkZ) {
        long h = seed ^ (chunkX * 0x9E3779B97F4A7C15L) ^ (chunkZ * 0xBF58476D1CE4E5B9L);
        h = (h ^ (h >>> 33)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 29);
    }

    /** 模板内的箱子/潜影盒统一写入 BL2 战利品表；空隧道列按哈希小概率补生成物资容器 */
    private static void applyLoot(WorldGenLevel level, BlockPos origin) {
        long seed = level.getSeed();
        for (int x = 0; x < TPL_SIZE; x++) {
            for (int z = 0; z < TPL_SIZE; z++) {
                BlockPos p = origin.offset(x, INTERIOR_MIN_Y, z);
                if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity container) {
                    // 模板自带的容器：写入战利品表
                    container.setLootTable(BLOCKLEVEL2_LOOT);
                    container.setLootTableSeed(seed ^ p.asLong());
                    continue;
                }
                // 兜底：空隧道列（y=1 空气、y=0 石砖地面）小概率生成箱子/潜影盒
                if (level.getBlockState(p).isAir()
                        && level.getBlockState(p.below()).is(Blocks.STONE_BRICKS)) {
                    long h = seed ^ (p.getX() * 0x9E3779B97F4A7C15L) ^ (p.getZ() * 0xBF58476D1CE4E5B9L);
                    h = (h ^ (h >>> 33)) * 0x94D049BB133111EBL;
                    int roll = (int) ((h ^ (h >>> 29)) & 0xFF);
                    BlockState state = null;
                    if (roll < 6) {
                        state = Blocks.CHEST.defaultBlockState();
                    } else if (roll < 8) {
                        state = Blocks.SHULKER_BOX.defaultBlockState();
                    }
                    if (state != null) {
                        placeLootContainer(level, p, state, seed ^ p.asLong());
                    }
                }
            }
        }
    }

    private static void placeLootContainer(WorldGenLevel level, BlockPos pos, BlockState state, long lootSeed) {
        level.setBlock(pos, state, Block.UPDATE_NONE);
        if (state.getBlock() instanceof EntityBlock entityBlock) {
            if (entityBlock.newBlockEntity(pos, state) instanceof RandomizableContainerBlockEntity container) {
                container.setLootTable(BLOCKLEVEL2_LOOT);
                container.setLootTableSeed(lootSeed);
                level.getChunk(pos).setBlockEntity(container);
            }
        }
    }

    /** 模板缺失时的保底：y=0..4 石砖 + y=5 基岩 */
    private static void fillFallback(ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= CAP_Y; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z),
                            y == CAP_Y ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(),
                            Block.UPDATE_NONE);
                }
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // 结构模板自包含（含箱子/装饰）；此阶段无需额外生成
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }
}
