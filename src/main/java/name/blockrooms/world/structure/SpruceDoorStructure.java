package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * BlockLevel 2 的云杉木门：随机散落在隧道中。
 * 门后是传送方块，目标暂时留空（BlockLevel ! / The Void 尚未实现）。
 */
public class SpruceDoorStructure extends Structure {
    public static final MapCodec<SpruceDoorStructure> CODEC = simpleCodec(SpruceDoorStructure::new);

    /** 每个区块生成一扇门的概率为 1 / DOOR_SPACING。 */
    private static final long DOOR_SPACING = 8;

    public SpruceDoorStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        long hash = doorHash(context.seed(), chunkPos.x(), chunkPos.z(), 0x9E3779B97F4A7C15L);
        if (Math.floorMod(hash, DOOR_SPACING) != 0) {
            return Optional.empty();
        }
        int dx = 2 + (int) Math.floorMod(hash >>> 8, 12);
        int dz = 2 + (int) Math.floorMod(hash >>> 16, 12);
        Direction facing = Direction.from2DDataValue((int) Math.floorMod(hash >>> 24, 4));
        // y=1：结构写在 y=1..3（3 格高）。不能从 y=0 开始——postProcess 的 writable area
        // 是 minY+1..maxY（最底层留给基岩），y=0 的方块会被 box.isInside 静默丢弃。
        BlockPos anchor = new BlockPos(chunkPos.getMinBlockX() + dx, 1, chunkPos.getMinBlockZ() + dz);
        return Optional.of(new GenerationStub(anchor,
                builder -> builder.addPiece(new BlockLevel2DoorPiece(anchor, facing, false, null))));
    }

    /** 由种子+区块坐标+盐决定的稳定伪随机数。 */
    protected static long doorHash(long seed, int chunkX, int chunkZ, long salt) {
        long h = seed ^ salt ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L);
        h = (h ^ (h >>> 33)) * 0xBF58476D1CE4E5B9L;
        return h ^ (h >>> 29);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SPRUCE_DOOR_TYPE.get();
    }
}
