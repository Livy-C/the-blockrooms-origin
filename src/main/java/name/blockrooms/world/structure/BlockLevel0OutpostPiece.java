package name.blockrooms.world.structure;

import name.blockrooms.Blockrooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.NonNull;

public class BlockLevel0OutpostPiece extends StructurePiece {
    private static final int OUTPOST_HALF = 5;
    private static final ResourceKey<LootTable> OUTPOST_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "gameplay/bmeg_outpost"));

    public BlockLevel0OutpostPiece(BlockPos center) {
        super(ModStructures.BMEG_OUTPOST_PIECE_TYPE.get(), 0,
                BoundingBox.fromCorners(center.offset(-OUTPOST_HALF, 0, -OUTPOST_HALF), center.offset(OUTPOST_HALF, 6, OUTPOST_HALF)));
    }

    public BlockLevel0OutpostPiece(CompoundTag tag) {
        super(ModStructures.BMEG_OUTPOST_PIECE_TYPE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        int cx = this.boundingBox.minX() + OUTPOST_HALF;
        int cz = this.boundingBox.minZ() + OUTPOST_HALF;

        for (int dx = -OUTPOST_HALF; dx <= OUTPOST_HALF; dx++) {
            for (int dz = -OUTPOST_HALF; dz <= OUTPOST_HALF; dz++) {
                boolean wall = Math.abs(dx) == OUTPOST_HALF || Math.abs(dz) == OUTPOST_HALF;
                boolean door = dz == OUTPOST_HALF && (dx == -1 || dx == 0);

                for (int y = 0; y <= 5; y++) {
                    BlockState state = getBlockState(y, wall, door);
                    setBlock(level, box, cx + dx, y, cz + dz, state);
                }

                // 屋顶中心的红石灯（嵌在石砖天花板里，与外部红石块相连）
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    setBlock(level, box, cx + dx, 5, cz + dz,
                            Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
                    setBlock(level, box, cx + dx, 6, cz + dz, Blocks.REDSTONE_BLOCK.defaultBlockState());
                }
            }
        }

        // 内部摆设：北墙两角的箱子、工作台与四面墙内侧的火把
        this.createChest(level, box, random, cx - OUTPOST_HALF + 1, 1, cz - OUTPOST_HALF + 1, OUTPOST_LOOT);
        this.createChest(level, box, random, cx + OUTPOST_HALF - 1, 1, cz - OUTPOST_HALF + 1, OUTPOST_LOOT);
        setBlock(level, box, cx + 3, 1, cz + 3, Blocks.CRAFTING_TABLE.defaultBlockState());
        // 火把放在墙面内侧的空气格里（原实现放在墙方块里，会被判定为无支撑而掉落）
        setBlock(level, box, cx, 3, cz - OUTPOST_HALF + 1, wallTorch(Direction.SOUTH));
        setBlock(level, box, cx + 2, 3, cz + OUTPOST_HALF - 1, wallTorch(Direction.NORTH));
        setBlock(level, box, cx - OUTPOST_HALF + 1, 3, cz, wallTorch(Direction.EAST));
        setBlock(level, box, cx + OUTPOST_HALF - 1, 3, cz, wallTorch(Direction.WEST));
    }

    private static @NonNull BlockState getBlockState(int y, boolean wall, boolean door) {
        BlockState state;
        if (y < 1) {
            // 地板
            state = Blocks.STONE_BRICKS.defaultBlockState();
        } else if (wall) {
            if (door && y <= 3) {
                // 南墙门洞
                state = Blocks.CAVE_AIR.defaultBlockState();
            } else {
                state = Blocks.STONE_BRICKS.defaultBlockState();
            }
        } else if (y <= 4) {
            // 内部空间
            state = Blocks.CAVE_AIR.defaultBlockState();
        } else {
            // 屋顶
            state = Blocks.STONE_BRICKS.defaultBlockState();
        }
        return state;
    }

    private static BlockState wallTorch(Direction facing) {
        return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
    }

    private static void setBlock(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (box.isInside(pos)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }
}
