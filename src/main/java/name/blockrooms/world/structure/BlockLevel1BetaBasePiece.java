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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.NonNull;

public class BlockLevel1BetaBasePiece extends StructurePiece {

    public static final int HALF = 6;

    private static final ResourceKey<LootTable> BASE_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "gameplay/blocklevel1"));

    public BlockLevel1BetaBasePiece(BlockPos center) {
        super(ModStructures.BETA_BASE_PIECE_TYPE.get(), 0,
                BoundingBox.fromCorners(center.offset(-HALF, 0, -HALF), center.offset(HALF, 8, HALF)));
    }

    public BlockLevel1BetaBasePiece(CompoundTag tag) {
        super(ModStructures.BETA_BASE_PIECE_TYPE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        int cx = this.boundingBox.minX() + HALF;
        int cz = this.boundingBox.minZ() + HALF;

        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                boolean wall = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
                boolean doorSouth = dz == HALF && (dx == -1 || dx == 0); // 南墙门洞
                boolean doorEast = dx == HALF && (dz == -1 || dz == 0);  // 东墙门洞

                for (int y = 0; y <= 8; y++) {
                    BlockState state = getBlockState(y, wall, doorSouth, doorEast);
                    setBlock(level, box, cx + dx, y, cz + dz, state);
                }

                // 屋顶中央的常亮红石灯（嵌在石英屋顶里，与上方红石块相连）
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    setBlock(level, box, cx + dx, 5, cz + dz,
                            Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
                    setBlock(level, box, cx + dx, 6, cz + dz, Blocks.REDSTONE_BLOCK.defaultBlockState());
                }
            }
        }

        // 铁门（门洞底部两格）
        ironDoor(level, box, cx - 1, cz + HALF);
        ironDoor(level, box, cx + HALF, cz - 1);

        // 内部摆设：两角箱子、工作台、床与四面墙内侧火把
        this.createChest(level, box, random, cx - HALF + 1, 1, cz - HALF + 1, BASE_LOOT);
        this.createChest(level, box, random, cx + HALF - 1, 1, cz - HALF + 1, BASE_LOOT);
        setBlock(level, box, cx - HALF + 3, 1, cz + HALF - 1, Blocks.CRAFTING_TABLE.defaultBlockState());
        setBlock(level, box, cx + 3, 1, cz + 3, Blocks.BED.red().defaultBlockState());
        // 火把放在墙面内侧的空气格里（原实现放在墙方块里，会被判定为无支撑而掉落）
        setBlock(level, box, cx, 3, cz - HALF + 1, wallTorch(Direction.SOUTH));
        setBlock(level, box, cx + 2, 3, cz + HALF - 1, wallTorch(Direction.NORTH));
        setBlock(level, box, cx - HALF + 1, 3, cz, wallTorch(Direction.EAST));
        setBlock(level, box, cx + HALF - 1, 3, cz, wallTorch(Direction.WEST));
    }

    private static @NonNull BlockState getBlockState(int y, boolean wall, boolean doorSouth, boolean doorEast) {
        BlockState state;
        if (y < 1) {
            // 地板
            state = Blocks.QUARTZ_BLOCK.defaultBlockState();
        } else if (wall) {
            if ((doorSouth || doorEast) && y <= 3) {
                // 门洞
                state = Blocks.CAVE_AIR.defaultBlockState();
            } else if (y >= 6) {
                // 墙顶封顶基岩
                state = Blocks.BEDROCK.defaultBlockState();
            } else {
                state = Blocks.QUARTZ_BRICKS.defaultBlockState();
            }
        } else if (y <= 4) {
            // 内部空间
            state = Blocks.CAVE_AIR.defaultBlockState();
        } else if (y == 5) {
            // 屋顶
            state = Blocks.QUARTZ_BLOCK.defaultBlockState();
        } else {
            // 封顶基岩
            state = Blocks.BEDROCK.defaultBlockState();
        }
        return state;
    }

    private static void ironDoor(WorldGenLevel level, BoundingBox box, int x, int z) {
        BlockState lower = Blocks.IRON_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        setBlock(level, box, x, 1, z, lower);
        setBlock(level, box, x, 2, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
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
