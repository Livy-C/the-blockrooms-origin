package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;

public class BlockLevel3Generator extends SimpleGenerator {
    public static final int LAYER_HEIGHT = 5;
    public static final int TUNNEL_MIN = 7;
    public static final int TUNNEL_MAX = 9;
    public static final int TRAP_MIN_Y = 60;

    public static final MapCodec<BlockLevel3Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel3Generator::getBiomeSource)
            ).apply(instance, BlockLevel3Generator::new)
    );

    public BlockLevel3Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    private record EdgeOpening(boolean east, boolean west, boolean south, boolean north) {
        boolean xTunnel() { return east || west; }
        boolean zTunnel() { return south || north; }
        boolean cross() { return xTunnel() && zTunnel(); }
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        for (int y = this.getMinY(); y < this.getMinY() + this.getGenDepth(); y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
        }

        int maxLayer = (this.getGenDepth() - 1) / LAYER_HEIGHT;
        for (int layer = 0; layer <= maxLayer; layer++) {
            var xRandom = randomState.getOrCreateRandomFactory(Identifier.fromNamespaceAndPath(Blockrooms.MODID, "edge_x"));
            var zRandom = randomState.getOrCreateRandomFactory(Identifier.fromNamespaceAndPath(Blockrooms.MODID, "edge_z"));
            this.generateLayer(chunk, layer, xRandom, zRandom);
        }

        return CompletableFuture.completedFuture(chunk);
    }

    private static boolean edgeOpen(PositionalRandomFactory factory, ChunkPos chunkPos, int layer, Direction direction) {
        if (direction.getAxis().isVertical()) throw new IllegalArgumentException("Direction must be horizontal");
        BlockPos pos = new BlockPos(chunkPos.x(), layer * 25, chunkPos.z());
        if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            pos = pos.relative(direction);
        }
        return factory.at(pos).nextDouble() < 0.55;
    }

    private void generateLayer(ChunkAccess chunk, int layer, PositionalRandomFactory xRandom, PositionalRandomFactory zRandom) {
        int baseY = this.getMinY() + layer * LAYER_HEIGHT;

        EdgeOpening edges = new EdgeOpening(
                edgeOpen(xRandom, chunk.getPos(), layer, Direction.EAST),
                edgeOpen(xRandom, chunk.getPos(), layer, Direction.WEST),
                edgeOpen(zRandom, chunk.getPos(), layer, Direction.SOUTH),
                edgeOpen(zRandom, chunk.getPos(), layer, Direction.NORTH)
        );

        this.generateTunnel(chunk, baseY, edges);
        this.generateStructures(chunk, baseY, edges, false);
    }

    private void generateTunnel(ChunkAccess chunk, int baseY, EdgeOpening edges) {
        if (edges.xTunnel()) {
            for (int x = 0; x < 16; x++) {
                for (int z = TUNNEL_MIN; z <= TUNNEL_MAX; z++) {
                    if (x == 0 && !edges.west()) continue;
                    if (x == 15 && !edges.east()) continue;
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(x, baseY + y, z), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
            }
        }

        if (edges.zTunnel()) {
            for (int z = 0; z < 16; z++) {
                for (int x = TUNNEL_MIN; x <= TUNNEL_MAX; x++) {
                    if (z == 0 && !edges.north()) continue;
                    if (z == 15 && !edges.south()) continue;
                    for (int y = 1; y <= 3; y++) {
                        chunk.setBlockState(new BlockPos(x, baseY + y, z), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                    }
                }
            }
        }
    }

    private void generateStructures(ChunkAccess chunk, int baseY, EdgeOpening edges, boolean isDark) {
        final BlockState PLANKS = isDark ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
        final BlockState FENCE = isDark ? Blocks.DARK_OAK_FENCE.defaultBlockState() : Blocks.OAK_FENCE.defaultBlockState();
        final BlockState TORCH = isDark ? Blocks.SOUL_WALL_TORCH.defaultBlockState() : Blocks.WALL_TORCH.defaultBlockState();

        if (edges.cross()) {
            for (int y = 1; y <= 3; y++) {
                for (int x = 7; x <= 9; x++) {
                    for (int z = 7; z <= 9; z++) {
                        BlockPos pos = new BlockPos(x, baseY + y, z);
                        if (x == 8 || z == 8) {
                            chunk.setBlockState(pos, Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                        } else {
                            chunk.setBlockState(pos, PLANKS, Block.UPDATE_NONE);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < 16; i++) {
            if (edges.xTunnel()) {
                if ((chunk.getPos().getMinBlockX() + i) % 5 == 0 &&
                        (chunk.getBlockState(new BlockPos(i + 1, baseY + 1, TUNNEL_MIN)).isAir()
                                && chunk.getBlockState(new BlockPos(i - 1, baseY + 1, TUNNEL_MIN)).isAir())) {
                    for (int z = TUNNEL_MIN; z <= TUNNEL_MAX; z++) {
                        chunk.setBlockState(new BlockPos(i, baseY + 3, z), PLANKS, Block.UPDATE_NONE);
                    }
                    for (int y = 1; y <= 2; y++) {
                        chunk.setBlockState(new BlockPos(i, baseY + y, TUNNEL_MIN), FENCE, Block.UPDATE_NONE);
                        chunk.setBlockState(new BlockPos(i, baseY + y, TUNNEL_MAX), FENCE, Block.UPDATE_NONE);
                    }
                    chunk.setBlockState(new BlockPos(i + 1, baseY + 3, (TUNNEL_MIN + TUNNEL_MAX) / 2),
                            TORCH.setValue(WallTorchBlock.FACING, Direction.EAST), Block.UPDATE_NONE);
                }
            }
            if (edges.zTunnel()) {
                if ((chunk.getPos().getMinBlockZ() + i) % 5 == 0 &&
                        (chunk.getBlockState(new BlockPos(TUNNEL_MIN, baseY + 1, i + 1)).isAir()
                                && chunk.getBlockState(new BlockPos(TUNNEL_MIN, baseY + 1, i - 1)).isAir())) {
                    for (int x = TUNNEL_MIN; x <= TUNNEL_MAX; x++) {
                        chunk.setBlockState(new BlockPos(x, baseY + 3, i), PLANKS, Block.UPDATE_NONE);
                    }
                    for (int y = 1; y <= 2; y++) {
                        chunk.setBlockState(new BlockPos(TUNNEL_MIN, baseY + y, i), FENCE, Block.UPDATE_NONE);
                        chunk.setBlockState(new BlockPos(TUNNEL_MAX, baseY + y, i), FENCE, Block.UPDATE_NONE);
                    }
                    chunk.setBlockState(new BlockPos((TUNNEL_MIN + TUNNEL_MAX) / 2, baseY + 3, i + 1),
                            TORCH.setValue(WallTorchBlock.FACING, Direction.SOUTH), Block.UPDATE_NONE);
                }
            }
        }
    }

    private boolean canPlacePlanks(int i, EdgeOpening edges, Direction direction) {

        return false;
    }

    /* @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cp = chunk.getPos();
        long seed = level.getSeed();
        int maxLayer = (this.getGenDepth() - 1) / LAYER_HEIGHT;
        for (int layer = 0; layer <= maxLayer; layer++) {
            int baseY = layer * LAYER_HEIGHT;
            generateLayer(level, chunk, cp, seed, layer, baseY);
        }
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    private void generateLayer(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed, int layer, int baseY) {
        boolean east = edgeOpen(seed, cp.x(), cp.z(), layer, Direction.EAST);
        boolean west = edgeOpen(seed, cp.x(), cp.z(), layer, Direction.WEST);
        boolean south = edgeOpen(seed, cp.x(), cp.z(), layer, Direction.SOUTH);
        boolean north = edgeOpen(seed, cp.x(), cp.z(), layer, Direction.NORTH);
        boolean xTunnel = east || west;
        boolean zTunnel = south || north;

        // 墙内矿石（每层少量）
        placeOres(chunk, cp, seed, layer, baseY, xTunnel, zTunnel);

        // 运输矿车（约 1/28 层）
        if (hash(seed, cp, layer, 0, 0x33) % 28 == 0 && (xTunnel || zTunnel)) {
            int mx, mz;
            if (xTunnel) {
                mx = 2 + (int) (hash(seed, cp, layer, 1, 0x44) % 12);
                mz = 7;
            } else {
                mx = 7;
                mz = 2 + (int) (hash(seed, cp, layer, 2, 0x44) % 12);
            }
            BlockPos railPos = new BlockPos(cp.getMinBlockX() + mx, baseY + 1, cp.getMinBlockZ() + mz);
            level.setBlock(railPos, Blocks.RAIL.defaultBlockState(), Block.UPDATE_NONE);
            MinecartChest cart = new MinecartChest(EntityTypes.CHEST_MINECART, level.getLevel());
            cart.setPos(railPos.getX() + 0.5, railPos.getY(), railPos.getZ() + 0.5);
            level.addFreshEntity(cart);
        }
        placeLayerTemplates(level, chunk, cp, seed, layer, baseY, xTunnel, zTunnel);
        if (hash(seed, cp, layer, 3, 0x55) % 100 < 30 && (xTunnel || zTunnel)) {
            int sx = 2 + (int) (hash(seed, cp, layer, 4, 0x66) % 12);
            int sz = 7;
            if (!xTunnel) {
                sx = 7;
                sz = 2 + (int) (hash(seed, cp, layer, 5, 0x66) % 12);
            }
            for (int y = baseY + 1; y <= baseY + 5; y++) {
                chunk.setBlockState(new BlockPos(sx, y, sz), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
            }
            for (int y = baseY + 1; y <= baseY + 4; y++) {
                chunk.setBlockState(new BlockPos(sx, y, sz - 1),
                        Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH),
                        Block.UPDATE_NONE);
            }
        }
    }

    private void placeOres(ChunkAccess chunk, ChunkPos cp, long seed, int layer, int baseY, boolean xTunnel, boolean zTunnel) {
        BlockState[] ores = {
                Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.REDSTONE_ORE.defaultBlockState(),
                Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.COAL_ORE.defaultBlockState(),
                Blocks.COAL_ORE.defaultBlockState(), Blocks.LAPIS_ORE.defaultBlockState(),
                Blocks.IRON_ORE.defaultBlockState(), Blocks.EMERALD_ORE.defaultBlockState()
        };
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (hash(seed, cp, layer, x * 16 + z, 0x77) % 100 >= 10) continue;
                boolean xWall = xTunnel && (z == TUNNEL_MIN - 1 || z == TUNNEL_MAX + 1);
                boolean zWall = zTunnel && (x == TUNNEL_MIN - 1 || x == TUNNEL_MAX + 1);
                if (!xWall && !zWall) continue;
                int y = 1 + (int) (hash(seed, cp, layer, x * 16 + z, 0x88) % 3);
                BlockState ore = ores[(int) (hash(seed, cp, layer, x * 16 + z, 0x99) % ores.length)];
                chunk.setBlockState(new BlockPos(x, baseY + y, z), ore, Block.UPDATE_NONE);
            }
        }
    }

    // ---------- 模板放置 ----------

    private void placeLayerTemplates(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp, long seed,
                                     int layer, int baseY, boolean xTunnel, boolean zTunnel) {
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();

        if (xTunnel && zTunnel && hash(seed, cp, layer, 2, 0xAA) % 3 == 0) {
            placeTemplate(manager, level, JUNCTION, new BlockPos(minX + TUNNEL_MIN, baseY, minZ + TUNNEL_MIN));
        }

        if (xTunnel && hash(seed, cp, layer, 3, 0xBB) % 100 < 40) {
            Identifier tpl = DRIVE_TEMPLATES[(int) (hash(seed, cp, layer, 4, 0xCC) % DRIVE_TEMPLATES.length)];
            placeTemplate(manager, level, tpl, new BlockPos(minX, baseY, minZ + TUNNEL_MIN));
        }

        if (zTunnel && hash(seed, cp, layer, 5, 0xDD) % 100 < 40) {
            Identifier tpl = ACROSS_TEMPLATES[(int) (hash(seed, cp, layer, 6, 0xEE) % ACROSS_TEMPLATES.length)];
            placeTemplate(manager, level, tpl, new BlockPos(minX + TUNNEL_MIN, baseY, minZ));
        }
        if (hash(seed, cp, layer, 7, 0x11) % 30 == 0) {
            int pick = (int) (hash(seed, cp, layer, 8, 0x22) % 4);
            if (pick == 0 && xTunnel) {
                placeTemplate(manager, level, SPECIAL_X[0], new BlockPos(minX, baseY, minZ + TUNNEL_MIN)); // 假出口
            } else if (pick == 1 && zTunnel) {
                placeTemplate(manager, level, SPECIAL_Z, new BlockPos(minX + TUNNEL_MIN, baseY, minZ + 3)); // 激光奖励房
            } else if (pick == 2 && xTunnel) {
                placeTemplate(manager, level, SPECIAL_X[1], new BlockPos(minX + 3, baseY, minZ + TUNNEL_MIN)); // 音乐房间
            } else if (pick == 3 && baseY >= TRAP_MIN_Y) {
                placeTemplate(manager, level, TRAP, new BlockPos(minX + 4, baseY, minZ + TUNNEL_MIN));
            }
        }
    }

    // ---------- 边哈希 ----------
    private static boolean edgeOpen(long seed, int chunkX, int chunkZ, int layer, Direction dir) {
        int ex = chunkX;
        int ez = chunkZ;
        if (dir == Direction.WEST) {
            ex = chunkX - 1;
            dir = Direction.EAST;
        } else if (dir == Direction.NORTH) {
            ez = chunkZ - 1;
            dir = Direction.SOUTH;
        }
        long h = seed ^ (ex * 0x9E3779B97F4A7C15L) ^ (ez * 0xC2B2AE3D27D4EB4FL)
                ^ (layer * 0x165667B19E3779F9L) ^ (dir.get2DDataValue() * 0x94D049BB133111EBL);
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return ((h ^ (h >>> 32)) & 0xFFFF) / 65536.0 < 0.65;
    }

    private static long hash(long seed, ChunkPos cp, int layer, int a, long salt) {
        long h = seed ^ (cp.x() * 0x9E3779B97F4A7C15L) ^ (cp.z() * 0xBF58476D1CE4E5B9L)
                ^ (layer * 0x165667B19E3779F9L) ^ (a * 0x94D049BB133111EBL) ^ salt;
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return (h ^ (h >>> 32)) & Long.MAX_VALUE;
    } */

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
