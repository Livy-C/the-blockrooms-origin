package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.world.structure.BlockLevel1BetaBasePiece;
import name.blockrooms.world.structure.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;


public class BlockLevel1Generator extends SimpleGenerator {
    public static final MapCodec<BlockLevel1Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel1Generator::getBiomeSource)
            ).apply(instance, BlockLevel1Generator::new)
    );
    private static final int BASE_RANGE = 128 * 16;
    private static final long BASE_SEED_XOR = 0x7F3C9E21A4B5D6E7L;
    private static final int HEIGHT_MIN = 3;
    private static final int HEIGHT_MAX = 6;
    private static final int FLOOR_HEIGHT = 8;
    private static final int FLOOR_COUNT = 32;
    private static final int DOOR_MIN = 6;
    private static final int DOOR_MAX = 8;

    private static final int[] PILLAR_ANCHORS = {2, 10};

    private static final double PAINTING_CHANCE = 0.02;
    private static final double FAKE_FRAME_CHANCE = 0.01;

    private static final int SHAFT_GRID = 4;

    private static final int FLOORS_PER_SHAFT = 3;

    private static final ResourceKey<LootTable> BL1_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "chests/blocklevel1"));

    private static final Identifier LAYOUT_RANDOM =
            Identifier.fromNamespaceAndPath(Blockrooms.MODID, "blocklevel1_layout");

    public BlockLevel1Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** 房间类型：石英大厅（主体）/ 石质密室 / 通廊 */
    public enum RoomType { QUARTZ_HALL, STONE_VAULT, CORRIDOR }

    /** 由 (RandomState, 楼层, 区块坐标) 确定房间类型：大厅 70%、密室 10%、通廊 20% */
    public static RoomType roomType(RandomState randomState, int floor, int chunkX, int chunkZ) {
        double r = bl1Random(randomState, floor, chunkX, chunkZ, 1).nextDouble();
        if (r < 0.70) return RoomType.QUARTZ_HALL;
        if (r < 0.80) return RoomType.STONE_VAULT;
        return RoomType.CORRIDOR;
    }

    /** 由 (RandomState, 楼层, 区块坐标) 确定房间内部高度（3~6 格） */
    private static int roomHeight(RandomState randomState, int floor, int chunkX, int chunkZ) {
        return 5;
    }

    /** 区块级确定性随机：同一世界（同一 RandomState）内一致，跨世界随种子不同 */
    private static RandomSource bl1Random(RandomState randomState, int floor, int chunkX, int chunkZ, int salt) {
        return randomState.getOrCreateRandomFactory(LAYOUT_RANDOM)
                .fromSeed(salt ^ (chunkX * 0x9e3779b97f4a7c15L) ^ (chunkZ * 0xdefacedddeedbeefL) ^ (floor * 0x2545f4914f6cdd1dL));
    }
    private static BlockPos elevatorShaftCenter(RandomState randomState, int segment, int chunkX, int chunkZ) {
        int gridX = Math.floorDiv(chunkX, SHAFT_GRID);
        int gridZ = Math.floorDiv(chunkZ, SHAFT_GRID);
        RandomSource random = randomState.getOrCreateRandomFactory(LAYOUT_RANDOM)
                .fromSeed(gridX * 0x9e3779b97f4a7c15L ^ gridZ * 0xdefacedddeedbeefL ^ (segment * 0x1b8735934954b1a1L));
        int floor0 = segment * FLOORS_PER_SHAFT;
        BlockPos candidate = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            int cx = gridX * SHAFT_GRID + random.nextInt(SHAFT_GRID);
            int cz = gridZ * SHAFT_GRID + random.nextInt(SHAFT_GRID);
            candidate = new BlockPos(cx * 16 + 2 + random.nextInt(12), 0, cz * 16 + 2 + random.nextInt(12));
            boolean inCorridor = false;
            for (int f = 0; f < FLOORS_PER_SHAFT; f++) {
                int fl = floor0 + f;
                if (fl < FLOOR_COUNT && roomType(randomState, fl, cx, cz) == RoomType.CORRIDOR) {
                    inCorridor = true;
                    break;
                }
            }
            if (!inCorridor) {
                return candidate;
            }
        }
        // 兜底：3 层通廊覆盖过广、重掷未能避开时，直接用最后一次候选
        return candidate;
    }

    /**
     * 竖井入口方向：3×3 井壁的直边格留一口（全高空气），玩家从房间内部走进井里站上电梯方块。
     * 方向由 (超格, 层段) 种子独立决定（与中心位置加盐区分），每根竖井固定一个入口。
     */
    private static Direction elevatorShaftEntrance(RandomState randomState, int segment, int chunkX, int chunkZ) {
        int gridX = Math.floorDiv(chunkX, SHAFT_GRID);
        int gridZ = Math.floorDiv(chunkZ, SHAFT_GRID);
        RandomSource random = randomState.getOrCreateRandomFactory(LAYOUT_RANDOM)
                .fromSeed(gridX * 0x9e3779b97f4a7c15L ^ gridZ * 0xdefacedddeedbeefL ^ (segment * 0x1b8735934954b1a1L) ^ 0x51ab73d9c4e2f08bL);
        return Direction.from2DDataValue(random.nextInt(4));
    }

    /** 由维度种子派生的唯一「石英β基地」位置，限定在世界中心 128 个区块（2048 格）内 */
    public static BlockPos betaBaseCenter(long seed) {
        RandomSource random = RandomSource.create(seed ^ BASE_SEED_XOR);
        int x = random.nextInt(BASE_RANGE * 2) - BASE_RANGE;
        int z = random.nextInt(BASE_RANGE * 2) - BASE_RANGE;
        return new BlockPos(x, 1, z);
    }

    /** 基地占据的生成范围（y 0..FLOOR_HEIGHT 与第 0 层封顶范围一致），供 fillFromNoise / spawnOriginalMobs 避开基地内部 */
    public static BoundingBox betaBaseBox(long seed) {
        BlockPos center = betaBaseCenter(seed);
        return BoundingBox.fromCorners(center.offset(-BlockLevel1BetaBasePiece.HALF, 0, -BlockLevel1BetaBasePiece.HALF),
                center.offset(BlockLevel1BetaBasePiece.HALF, FLOOR_HEIGHT, BlockLevel1BetaBasePiece.HALF));
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();

        // 基地结构 box：structure starts 在 fillFromNoise（NOISE 阶段）之前已生成，
        // 直接向结构系统查询。普通区块只查中心一点快速失败；基地附近区块再逐格补查
        // （13×13 基地可能只与本区块相交一小条，中心点不在 box 内）。
        BoundingBox baseBox = null;
        StructureStart baseStart = structureManager.getStructureWithPieceAt(
                new BlockPos(minX + 8, 0, minZ + 8),(a) -> a.value().equals(ModStructures.BETA_BASE_TYPE.get()));
        if (baseStart.isValid()) {
            baseBox = baseStart.getBoundingBox();
        } else {
            for (int x = 0; x < 16 && baseBox == null; x++) {
                for (int z = 0; z < 16 && baseBox == null; z++) {
                    StructureStart start = structureManager.getStructureWithPieceAt(
                            new BlockPos(minX + x, 0, minZ + z),(a) -> a.value().equals(ModStructures.BETA_BASE_TYPE.get()));
                    if (start.isValid()) {
                        baseBox = start.getBoundingBox();
                    }
                }
            }
        }
        final BoundingBox box = baseBox;

        int floors = getGenDepth() / FLOOR_HEIGHT;
        int segments = (floors + FLOORS_PER_SHAFT - 1) / FLOORS_PER_SHAFT;
        for (int segment = 0; segment < segments; segment++) {
            int segY = segment * FLOORS_PER_SHAFT * FLOOR_HEIGHT;

            // 本段电梯竖井：位置由 (超格, 层段) 决定，段与段之间位置不同——
            // 玩家用电梯穿越本段 2~3 层后，必须水平移动去找下一段的竖井
            BlockPos shaftCenter = elevatorShaftCenter(random, segment, pos.x(), pos.z());
            boolean hasShaft = shaftCenter.getX() >= minX - 1 && shaftCenter.getX() <= minX + 16
                    && shaftCenter.getZ() >= minZ - 1 && shaftCenter.getZ() <= minZ + 16;
            final int ex = shaftCenter.getX();
            final int ez = shaftCenter.getZ();
            Direction shaftEntrance = elevatorShaftEntrance(random, segment, pos.x(), pos.z());
            final int entranceX = ex + shaftEntrance.getStepX();
            final int entranceZ = ez + shaftEntrance.getStepZ();

            for (int f = 0; f < FLOORS_PER_SHAFT; f++) {
                int floor = segment * FLOORS_PER_SHAFT + f;
                if (floor >= floors) break;
                int baseY = floor * FLOOR_HEIGHT;
                boolean topFloor = baseY + FLOOR_HEIGHT >= getGenDepth();
                RoomType type = roomType(random, floor, pos.x(), pos.z());
                int h = roomHeight(random, floor, pos.x(), pos.z());
                boolean corridor = type == RoomType.CORRIDOR;
                boolean vault = type == RoomType.STONE_VAULT;

                // 门洞高度取同层两侧房间高度的较小值：低矮房间不开到天花板，高处留实心
                int doorW = Math.min(h, roomHeight(random, floor, pos.x() - 1, pos.z()));
                int doorE = Math.min(h, roomHeight(random, floor, pos.x() + 1, pos.z()));
                int doorN = Math.min(h, roomHeight(random, floor, pos.x(), pos.z() - 1));
                int doorS = Math.min(h, roomHeight(random, floor, pos.x(), pos.z() + 1));

                List<BlockPos> pillars = type == RoomType.QUARTZ_HALL ? pillarAnchors(random, floor, pos.x(), pos.z()) : List.of();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (hasShaft && Math.abs(minX + x - ex) <= 1 && Math.abs(minZ + z - ez) <= 1) continue;
                        for (int y = baseY; y <= Math.min(baseY + FLOOR_HEIGHT, getGenDepth() - 1); y++) {
                            if (box != null && box.isInside(minX + x, y, minZ + z)) continue;
                            BlockState state = resolveState(x, z, y - baseY, h, type, corridor, vault,
                                    doorW, doorE, doorN, doorS, pillars, topFloor);
                            if (state != null) {
                                chunk.setBlockState(new BlockPos(x, y, z), state, Block.UPDATE_NONE);
                            }
                        }
                    }
                }

                if (type == RoomType.QUARTZ_HALL) {
                    placeTorches(chunk, random, floor, pos.x(), pos.z());
                }
                if (type == RoomType.CORRIDOR) {
                    placeCorridorDoors(chunk, random, floor, pos.x(), pos.z(), h);
                }
            }

            // 本段竖井柱体：3×3 石英砖井壁 + 中心 1 格空气柱，段内每层地板（y % 8 == 0）嵌一块石英电梯方块，
            // 站上去跳跃/潜行即可传送到正上/正下方最近的一块（即本段内相邻层）。
            // 入口：井壁直边格留一口（每层地板同样嵌电梯块），玩家从房间走进井口即踩上电梯；
            // 入口格每层地板都有实心方块，不会整列悬空、最底层也不会露出虚空。
            // 段顶（中心格与入口格）用石英封住——井到此为止，穿完本段就得换地方找下一根。
            if (hasShaft) {
                int shaftTop = Math.min(segY + FLOORS_PER_SHAFT * FLOOR_HEIGHT, getGenDepth() - 1);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int wx = minX + x;
                        int wz = minZ + z;
                        if (Math.abs(wx - ex) > 1 || Math.abs(wz - ez) > 1) continue;
                        for (int y = segY; y <= shaftTop; y++) {
                            if (box != null && box.isInside(wx, y, wz)) continue;
                            boolean center = wx == ex && wz == ez;
                            boolean entrance = wx == entranceX && wz == entranceZ;
                            BlockState state;
                            if ((center || entrance) && y == shaftTop) {
                                state = Blocks.QUARTZ_BLOCK.defaultBlockState();
                            } else if ((center || entrance) && y % FLOOR_HEIGHT == 0) {
                                state = ModBlocks.QUARTZ_ELEVATOR.get().defaultBlockState();
                            } else if (center || entrance) {
                                state = Blocks.CAVE_AIR.defaultBlockState();
                            } else {
                                state = Blocks.QUARTZ_BRICKS.defaultBlockState();
                            }
                            chunk.setBlockState(new BlockPos(x, y, z), state, Block.UPDATE_NONE);
                        }
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * 骨架方块决策（y 为楼层内相对坐标 0..FLOOR_HEIGHT）：
     * <ul>
     *   <li>y=0 地板（大厅石英块 / 密室石头 / 通廊雕纹石英）</li>
     *   <li>y=1..h 内部空间：房间边缘墙壁 + 固定门洞（6..8），通廊中央走廊两侧实心，大厅石英柱，
     *       密室地面铺青色地毯（只铺房间内部，不覆盖墙壁）</li>
     *   <li>y=h+1 天花板（嵌灯 3×3 网格：大厅昼夜灯，密室常亮红石灯）</li>
     *   <li>y=h+2..FLOOR_HEIGHT 层顶填充：石英块充当上层地板（最顶层用基岩封顶防虚空）</li>
     * </ul>
     * 返回 null 表示该格不写。
     */
    private static @Nullable BlockState resolveState(int x, int z, int y, int h, RoomType type,
                                                     boolean corridor, boolean vault,
                                                     int doorW, int doorE, int doorN, int doorS,
                                                     List<BlockPos> pillars, boolean topFloor) {
        // 地板
        if (y == 0) {
            if (vault) return Blocks.STONE.defaultBlockState();
            if (corridor) return Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
            return Blocks.QUARTZ_BLOCK.defaultBlockState();
        }

        if (y >= 1 && y <= h) {
            // 通廊：中央 x=6..8 走廊贯通（与四边门洞坐标一致，两端自然开口），两侧实心；
            // 南北两端门洞由 placeCorridorDoors 装入口/出口门与压力板
            if (corridor) {
                if (x >= DOOR_MIN && x <= DOOR_MAX) {
                    return Blocks.CAVE_AIR.defaultBlockState();
                }
                return Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
            }

            // 房间边缘墙壁 + 门洞（固定坐标 6..8，相邻房间门洞必然对齐）
            boolean wallX = x == 0 || x == 15;
            boolean wallZ = z == 0 || z == 15;
            if (wallX || wallZ) {
                int doorHeight = wallX ? (x == 0 ? doorW : doorE) : (z == 0 ? doorN : doorS);
                boolean inDoor = wallX ? (z >= DOOR_MIN && z <= DOOR_MAX) : (x >= DOOR_MIN && x <= DOOR_MAX);
                if (inDoor && y <= doorHeight) return Blocks.CAVE_AIR.defaultBlockState();
                return vault ? Blocks.STONE.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState();
            }

            // 大厅石英柱（2×2，从地板到天花板）
            if (type == RoomType.QUARTZ_HALL) {
                for (BlockPos anchor : pillars) {
                    if (x >= anchor.getX() && x <= anchor.getX() + 1
                            && z >= anchor.getZ() && z <= anchor.getZ() + 1) {
                        return Blocks.QUARTZ_PILLAR.defaultBlockState();
                    }
                }
            }

            // 密室地面铺青色地毯（墙壁与柱子之后：地毯只铺在房间内部，绝不覆盖墙壁）
            if (y == 1 && vault) return Blocks.CARPET.cyan().defaultBlockState();

            return Blocks.CAVE_AIR.defaultBlockState();
        }

        // 天花板（嵌灯 3×3 网格）：大厅用昼夜感应灯，密室用常亮红石灯
        if (y == h + 1) {
            if (vault) {
                return (x % 3 == 0 && z % 3 == 0) ? constantLamp() : Blocks.STONE.defaultBlockState();
            }
            if (corridor) {
                if (x >= DOOR_MIN && x <= DOOR_MAX) {
                    return (x + z) % 3 == 0 ? constantLamp() : Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
                }
                return Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
            }
            if (x % 3 == 0 && z % 3 == 0) {
                return ModBlocks.DETECTOR_REDSTONE_LAMP_BLOCK.get().defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
            }
            return Blocks.QUARTZ_BLOCK.defaultBlockState();
        }

        // 层顶填充：石英块充当上层地板（挖穿下层天花板即可进入上层）；
        // 最顶层用基岩封顶，防止玩家挖穿天花板进入上方虚空
        if (y <= FLOOR_HEIGHT) return topFloor ? Blocks.BEDROCK.defaultBlockState() : Blocks.QUARTZ_BLOCK.defaultBlockState();
        return null;
    }

    /** 密室/通廊常亮红石灯 */
    private static BlockState constantLamp() {
        return Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
    }

    /** 大厅石英柱锚点（本地坐标，2×2 柱的角落），1~2 根，候选为四角区域 */
    private static List<BlockPos> pillarAnchors(RandomState randomState, int floor, int chunkX, int chunkZ) {
        RandomSource random = bl1Random(randomState, floor, chunkX, chunkZ, 3);
        List<BlockPos> anchors = new ArrayList<>();
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            anchors.add(new BlockPos(PILLAR_ANCHORS[random.nextInt(2)], 0, PILLAR_ANCHORS[random.nextInt(2)]));
        }
        return anchors;
    }

    /** 大厅墙壁内侧挂昼夜火把（y=baseY+2，间隔 3 格、约半数概率，避开门洞区域） */
    private static void placeTorches(ChunkAccess chunk, RandomState randomState, int floor, int chunkX, int chunkZ) {
        RandomSource random = bl1Random(randomState, floor, chunkX, chunkZ, 5);
        int y = floor * FLOOR_HEIGHT + 2;
        for (int axis = 1; axis <= 14; axis++) {
            if (axis >= DOOR_MIN && axis <= DOOR_MAX) continue;
            if (axis % 3 != 0) continue;
            if (random.nextDouble() > 0.5) continue;
            torch(chunk, 1, y, axis, Direction.EAST);    // 西墙内侧
            torch(chunk, 14, y, axis, Direction.WEST);   // 东墙内侧
            torch(chunk, axis, y, 1, Direction.SOUTH);   // 北墙内侧
            torch(chunk, axis, y, 14, Direction.NORTH);  // 南墙内侧
        }
    }

    private static void torch(ChunkAccess chunk, int x, int y, int z, Direction facing) {
        BlockState state = ModBlocks.DETECTOR_WALL_TORCH.get().defaultBlockState()
                .setValue(BlockStateProperties.LIT, true)
                .setValue(WallTorchBlock.FACING, facing);
        chunk.setBlockState(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
    }

    private static void placeCorridorDoors(ChunkAccess chunk, RandomState randomState, int floor, int chunkX, int chunkZ, int roomHeight) {
        RandomSource random = bl1Random(randomState, floor, chunkX, chunkZ, 7);
        boolean entranceSouth = random.nextBoolean();
        boolean ironDoor = random.nextBoolean();
        int baseY = floor * FLOOR_HEIGHT;
        int wallZ = entranceSouth ? 1 : 14;
        door(chunk, 7, baseY + 1, wallZ, ironDoor ? Blocks.IRON_DOOR : Blocks.OAK_DOOR,
                entranceSouth ? Direction.SOUTH : Direction.NORTH);
        for (int x = DOOR_MIN; x <= DOOR_MAX; x++) {
            for (int y = 1; y <= roomHeight; y++) {
                if (x == 7 && (y == 1 || y == 2)) continue;
                chunk.setBlockState(new BlockPos(x, baseY + y, wallZ),
                        Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        plate(chunk, 7, baseY + 1, wallZ - 1);
        plate(chunk, 7, baseY + 1, wallZ + 1);
    }

    private static void door(ChunkAccess chunk, int x, int y, int z, Block doorBlock, Direction facing) {
        BlockState lower = doorBlock.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        chunk.setBlockState(new BlockPos(x, y, z), lower, Block.UPDATE_NONE);
        chunk.setBlockState(new BlockPos(x, y + 1, z), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_NONE);
    }

    private static void plate(ChunkAccess chunk, int x, int y, int z) {
        chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), Block.UPDATE_NONE);
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        long seed = worldGenRegion.getSeed();
        ChunkPos chunkPos = worldGenRegion.getCenter();
        BoundingBox baseBox = betaBaseBox(seed);
        RandomState randomState = worldGenRegion.getLevel().getChunkSource().randomState();

        int floors = getGenDepth() / FLOOR_HEIGHT;
        for (int floor = 0; floor < floors; floor++) {
            int baseY = floor * FLOOR_HEIGHT;
            RoomType type = roomType(randomState, floor, chunkPos.x(), chunkPos.z());
            if (!baseBox.isInside(chunkPos.getMinBlockX() + 8, baseY, chunkPos.getMinBlockZ() + 8)) {
                RandomSource random = bl1Random(randomState, floor, chunkPos.x(), chunkPos.z(), 4);
                for (BlockPos local : chestPositions(type, randomState, floor, chunkPos.x(), chunkPos.z())) {
                    placeChest(worldGenRegion, random,
                            new BlockPos(chunkPos.getMinBlockX() + local.getX(), baseY + local.getY(), chunkPos.getMinBlockZ() + local.getZ()));
                }
            }
            if (floor % 4 == 0 && type == RoomType.QUARTZ_HALL) {
                placeHallArt(worldGenRegion, randomState, baseBox, floor);
            }
        }
    }

    /** 补给箱位置（本地坐标，与 fillFromNoise 同源确定性随机，生成阶段独立可复算） */
    private static List<BlockPos> chestPositions(RoomType type, RandomState randomState, int floor, int chunkX, int chunkZ) {
        RandomSource random = bl1Random(randomState, floor, chunkX, chunkZ, 4);
        List<BlockPos> chests = new ArrayList<>();
        if (type == RoomType.QUARTZ_HALL) {
            // wiki：补给箱通常刷新在柱子旁
            for (BlockPos anchor : pillarAnchors(randomState, floor, chunkX, chunkZ)) {
                if (random.nextDouble() < 0.5) {
                    chests.add(new BlockPos(anchor.getX() + 2, 1, anchor.getZ()));
                }
            }
        } else if (type == RoomType.STONE_VAULT) {
            // wiki：石质密室的补给箱生成概率更大
            int count = 1 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                if (random.nextDouble() < 0.8) {
                    chests.add(new BlockPos(2 + random.nextInt(12), 1, 2 + random.nextInt(12)));
                }
            }
        } else {
            // 通廊走廊中央（宽 3，两侧可绕过）
            if (random.nextDouble() < 0.8) {
                chests.add(new BlockPos(7, 1, 3 + random.nextInt(10)));
            }
        }
        return chests;
    }

    private static void placeChest(WorldGenRegion level, RandomSource random, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos);
        BlockState state = Blocks.CHEST.defaultBlockState();
        chunk.setBlockState(pos, state, Block.UPDATE_CLIENTS);
        ChestBlockEntity chest = (ChestBlockEntity) ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
        chest.setLootTable(BL1_LOOT, random.nextLong());
        chunk.setBlockEntity(chest);
    }

    /**
     * 大厅墙壁上随机生成 1×1 画（小概率为伪画框：物品展示框挂画布）。
     * 逻辑与 BlockLevel 0 一致：画必须以贴墙空气格为位置（面朝房间），墙材质为石英砖。
     */
    private static void placeHallArt(WorldGenRegion region, RandomState randomState, BoundingBox baseBox, int floor) {
        ChunkAccess chunk = region.getChunk(region.getCenter().x(), region.getCenter().z());
        List<Holder<PaintingVariant>> smallVariants = StreamSupport.stream(region.registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT)
                .getTagOrEmpty(PaintingVariantTags.PLACEABLE)
                        .spliterator()
                , false)
                .filter(variant -> variant.value().width() == 1 && variant.value().height() == 1)
                .toList();
        if (smallVariants.isEmpty()) return;

        int minBlockX = region.getCenter().getMinBlockX();
        int minBlockZ = region.getCenter().getMinBlockZ();
        RandomSource random = bl1Random(randomState, floor, region.getCenter().x(), region.getCenter().z(), 6);
        int baseY = floor * FLOOR_HEIGHT;

        for (int y = baseY + 2; y <= baseY + 3; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).isAir()) continue;

                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        // 墙必须在当前区块内：本地坐标越界会被 chunk.getBlockState 按 &15 环绕成对面的方块
                        int wx = x + dir.getStepX();
                        int wz = z + dir.getStepZ();
                        if (wx < 0 || wx > 15 || wz < 0 || wz > 15) continue;
                        if (!chunk.getBlockState(new BlockPos(wx, y, wz)).is(Blocks.QUARTZ_BRICKS)) continue;
                        // 墙后方也必须是空气，避免画挂在墙的夹缝里
                        int bx = wx + dir.getStepX();
                        int bz = wz + dir.getStepZ();
                        if (bx < 0 || bx > 15 || bz < 0 || bz > 15) continue;
                        if (!chunk.getBlockState(new BlockPos(bx, y, bz)).isAir()) continue;

                        double roll = random.nextDouble();
                        if (roll >= PAINTING_CHANCE + FAKE_FRAME_CHANCE) continue;

                        BlockPos worldPos = new BlockPos(minBlockX + x, y, minBlockZ + z);
                        if (baseBox.isInside(worldPos)) continue;

                        if (roll < FAKE_FRAME_CHANCE) {
                            ItemFrame frame = new ItemFrame(region.getLevel(), worldPos, dir.getOpposite());
                            region.addFreshEntity(frame);
                            region.getLevel().getServer().execute(() -> {
                                if (frame.isAlive()) {
                                    frame.setItem(new ItemStack(Items.PAINTING));
                                }
                            });
                        } else {
                            Optional<Holder<PaintingVariant>> variant = Util.getRandomSafe(smallVariants, region.getRandom());
                            if (variant.isEmpty()) continue;
                            Painting painting = new Painting(region.getLevel(), worldPos, dir.getOpposite(), variant.get());
                            region.addFreshEntity(painting);
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return 0;
    }
}
