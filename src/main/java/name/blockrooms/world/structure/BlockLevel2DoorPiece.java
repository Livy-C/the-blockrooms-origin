package name.blockrooms.world.structure;

import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.entity.TeleporterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jspecify.annotations.Nullable;

/**
 * BlockLevel 2 的门结构：3×3×2 的门洞墙（宽 3、高 3、深 2），
 * 生成在 y=1..3（anchor y=1——writable area 从 minY+1 开始，y=0 的方块会被丢弃）。
 *
 * <p>墙面上开一个 1×2 的门洞：云杉门结构放一扇云杉木门（spruce door），
 * 石英门结构不放门板（石英门框的通道）；门洞后方一格（墙背面）底部放一个
 * {@link name.blockrooms.block.TeleporterBlock}——玩家开门/走进门洞即被传送。</p>
 *
 * <p>传送目标由构造参数决定：云杉门 → 暂无目标（BlockLevel ! / The Void 留空，传送方块无数据 = 无效果）；
 * 石英门 → BlockLevel 1。</p>
 */
public class BlockLevel2DoorPiece extends StructurePiece {
    private static final String ANCHOR_X_TAG = "AX";
    private static final String ANCHOR_Y_TAG = "AY";
    private static final String ANCHOR_Z_TAG = "AZ";
    private static final String FACING_TAG = "FACING";
    private static final String QUARTZ_TAG = "QUARTZ";
    private static final String TARGET_TAG = "TARGET";

    /** 门洞墙尺寸 */
    private static final int WALL_WIDTH = 3;
    private static final int WALL_HEIGHT = 3;
    private static final int DEPTH = 2;

    private final BlockPos anchor;
    private final Direction facing;
    private final boolean quartz;
    @Nullable
    private final ResourceKey<Level> targetDimension;

    public BlockLevel2DoorPiece(BlockPos anchor, Direction facing, boolean quartz, @Nullable ResourceKey<Level> targetDimension) {
        super(ModStructures.BL2_DOOR_PIECE_TYPE.get(), 0,
                BoundingBox.fromCorners(anchor, anchor.offset(WALL_WIDTH - 1, WALL_HEIGHT - 1, DEPTH - 1)));
        this.anchor = anchor;
        this.facing = facing;
        this.quartz = quartz;
        this.targetDimension = targetDimension;
    }

    /** 反序列化 */
    public BlockLevel2DoorPiece(CompoundTag tag) {
        super(ModStructures.BL2_DOOR_PIECE_TYPE.get(), tag);
        this.anchor = new BlockPos(tag.getIntOr(ANCHOR_X_TAG, 0), tag.getIntOr(ANCHOR_Y_TAG, 0), tag.getIntOr(ANCHOR_Z_TAG, 0));
        this.facing = Direction.from2DDataValue(tag.getIntOr(FACING_TAG, 0));
        this.quartz = tag.getIntOr(QUARTZ_TAG, 0) != 0;
        String dim = tag.getStringOr(TARGET_TAG, "");
        this.targetDimension = dim.isEmpty()
                ? null
                : ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt(ANCHOR_X_TAG, anchor.getX());
        tag.putInt(ANCHOR_Y_TAG, anchor.getY());
        tag.putInt(ANCHOR_Z_TAG, anchor.getZ());
        tag.putInt(FACING_TAG, facing.get2DDataValue());
        tag.putInt(QUARTZ_TAG, quartz ? 1 : 0);
        if (targetDimension != null) {
            tag.putString(TARGET_TAG, targetDimension.identifier().toString());
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        int ax = anchor.getX();
        int ay = anchor.getY();
        int az = anchor.getZ();

        // 位置校验：门框（除门洞/传送方块位）必须嵌在实心墙里，门洞两侧至少一侧接触空气（隧道）。
        // anchor 处不满足时，在本区块内搜索最近的可嵌墙位置，尽量保证门总是生成
        // （否则 /locate 会指向没有门的空位，调试困难）；全区块都找不到才跳过。
        BlockPos spot = findWallSpot(level, chunkPos, ax, ay, az);
        if (spot == null) {
            return;
        }
        ax = spot.getX();
        ay = spot.getY();
        az = spot.getZ();

        BlockState frame = quartz ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();

        // 放置范围 [ax, ax+2] × [ay, ay+2] × [az, az+1]，与构造的 boundingBox 完全一致；
        // 门洞/传送方块在 dx==1（正中）。box 是当前 chunk 的包围盒：跨 chunk 的部分由
        // 对应 chunk 生成时再放置（WorldGenRegion.ensureCanWrite 会静默丢弃中心 chunk 外的方块）。
        for (int dx = 0; dx <= WALL_WIDTH - 1; dx++) {
            for (int dy = 0; dy <= WALL_HEIGHT - 1; dy++) {
                for (int dz = 0; dz <= DEPTH - 1; dz++) {
                    BlockState state;
                    if (dz == 0) {
                        // 墙面层：中间 1x2 是门洞
                        if (dx == 1 && dy <= 1) {
                            if (!quartz && dy == 0) {
                                state = doorBlock(DoubleBlockHalf.LOWER);
                            } else if (!quartz && dy == 1) {
                                state = doorBlock(DoubleBlockHalf.UPPER);
                            } else {
                                state = Blocks.CAVE_AIR.defaultBlockState();
                            }
                        } else {
                            state = frame;
                        }
                    } else {
                        // 墙后一层：门洞正后方底部放传送方块（无碰撞，玩家走进即传送）
                        if (dx == 1 && dy == 0 || dx == 1 && dy == 1) {
                            state = ModBlocks.TELEPORTER_BLOCK.get().defaultBlockState();

                        } else {
                            state = frame;
                        }
                    }
                    setBlock(level, box, ax + dx, ay + dy, az + dz, state);
                }
            }
        }

        // 写入传送目标（石英门 → BlockLevel 1；云杉门目标留空 = 无效果）。
        // 注意：worldgen 阶段绝不能调用 level.getLevel()（ServerLevel）的 getBlockEntity，
        // 那会强制要求区块达到 FULL 状态，而当前区块的 FEATURES 阶段（放置结构）正跑在
        // 本 worker 线程上 → 互相等待 → 死锁、世界生成卡死。这里只操作当前生成中的区块。
        // 手动 setBlockEntity 会覆盖 setBlock 时 WorldGenRegion 自动创建的空 BE。
        if (targetDimension != null) {
            BlockPos tp = new BlockPos(ax + 1, ay, az + 1);
            if (box.isInside(tp) || box.isInside(tp.above())) {
                TeleporterBlockEntity blockEntity = new TeleporterBlockEntity(tp, level.getBlockState(tp));
                TeleporterBlockEntity blockEntity1 = new TeleporterBlockEntity(tp.above(), level.getBlockState(tp.above()));
                blockEntity.getTargets().add(new TeleporterBlockEntity.Target(targetDimension, null));
                blockEntity1.getTargets().add(new TeleporterBlockEntity.Target(targetDimension, null));
                level.getChunk(tp).setBlockEntity(blockEntity);
                level.getChunk(tp.above()).setBlockEntity(blockEntity);
            }
        }
    }

    /**
     * 在 anchor 处找嵌墙门位；不满足时在本区块内（z 避开边缘 1 格、x 留出 3 宽门框）线性搜索
     * 第一个可嵌墙位置。返回 null 表示全区块无合适位置（极少数，此时门跳过）。
     */
    private @Nullable BlockPos findWallSpot(WorldGenLevel level, ChunkPos chunkPos, int ax, int ay, int az) {
        if (fitsInWall(level, ax, ay, az)) {
            return new BlockPos(ax, ay, az);
        }
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        for (int nz = minZ + 1; nz <= minZ + 14; nz++) {
            for (int nx = minX; nx <= minX + 13; nx++) {
                if (nx == ax && nz == az) continue;
                if (fitsInWall(level, nx, ay, nz)) {
                    return new BlockPos(nx, ay, nz);
                }
            }
        }
        return null;
    }

    /** 门框区域（除门洞/传送方块位）必须全部是实心方块，且门洞两侧（az±1）至少一侧是空气。 */
    private boolean fitsInWall(WorldGenLevel level, int ax, int ay, int az) {
        for (int dx = 0; dx <= WALL_WIDTH - 1; dx++) {
            for (int dy = 0; dy <= WALL_HEIGHT - 1; dy++) {
                for (int dz = 0; dz <= DEPTH - 1; dz++) {
                    if (dx == 1 && dy <= 1) continue;            // 门洞列（空气/门板）
                    if (dx == 1 && dz == 1 && dy <= 1) continue; // 传送方块位
                    BlockState s = level.getBlockState(new BlockPos(ax + dx, ay + dy, az + dz));
                    if (s.isAir()) {
                        return false;
                    }
                }
            }
        }
        // 门洞两侧（az±1）至少一侧是空气（通道/隧道）——门必须接触空气，
        // 且不会生成在实心间隔区中间（两侧都实心则跳过）
        BlockPos front = new BlockPos(ax + 1, ay, az - 1);
        BlockPos back = new BlockPos(ax + 1, ay, az + 1);
        return level.getBlockState(front).isAir() || level.getBlockState(back).isAir();
    }

    private BlockState doorBlock(DoubleBlockHalf half) {
        return Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, half);
    }

    private static void setBlock(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (box.isInside(p)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }
}
