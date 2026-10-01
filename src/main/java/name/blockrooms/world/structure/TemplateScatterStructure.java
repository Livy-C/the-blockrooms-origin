package name.blockrooms.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * 基于 NBT 模板的散布结构基类：为每个模板派生一个子类，只需提供模板 ID、
 * 尺寸、锚点 Y 偏移与盐值。
 *
 * <p>锚点 X/Z 取区块内 [2, 13] 的确定性随机偏移（种子+区块坐标+盐值推导，
 * 跨区块/重载一致），朝向随机 0~3；锚点 Y = OCEAN_FLOOR 高度图 + 子类偏移，
 * 使结构底部贴合地面/河床。</p>
 *
 * <p>默认 {@link #spacing()} 为 1（不门控，每个区块都返回生成点），保证
 * {@code /place structure blockrooms:<id>} 一定可以放置；需要自然生成的结构
 * 请覆盖 {@link #spacing()} 并添加对应 structure_set JSON。</p>
 */
public abstract class TemplateScatterStructure extends Structure {

    private static final int MIN_OFFSET = 2;
    private static final int MAX_OFFSET = 13;

    protected TemplateScatterStructure(StructureSettings settings) {
        super(settings);
    }

    /** 模板 ID，对应 {@code data/blockrooms/structure/<id>.nbt}。 */
    protected abstract Identifier templateId();

    /** 模板尺寸（x/y/z，格）。用于生成部件的包围盒。 */
    protected abstract int sizeX();

    protected abstract int sizeY();

    protected abstract int sizeZ();

    /** 锚点 Y = OCEAN_FLOOR 高度图 + 该偏移。 */
    protected abstract int anchorYOffset();

    /** 散布哈希的盐值（每个结构唯一）。 */
    protected abstract long salt();

    /**
     * 生成间距（区块）：只有 {@code hash % spacing == 0} 的区块才生成。
     * 默认 1 = 每个区块都生成（保证 {@code /place structure} 一定成功）；
     * 需要自然生成的结构请覆盖为更大值（参考 {@link OakExitStructure} 的 16）。
     */
    protected long spacing() {
        return 1;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        long hash = scatterHash(context.seed(), chunkPos.x(), chunkPos.z(), salt());
        if (Math.floorMod(hash, spacing()) != 0) {
            return Optional.empty();
        }
        int dx = MIN_OFFSET + (int) ((hash >>> 8) % (MAX_OFFSET - MIN_OFFSET + 1));
        int dz = MIN_OFFSET + (int) ((hash >>> 16) % (MAX_OFFSET - MIN_OFFSET + 1));
        Rotation rotation = Rotation.values()[(int) ((hash >>> 24) % 4)];

        int x = chunkPos.getMinBlockX() + dx;
        int z = chunkPos.getMinBlockZ() + dz;
        int y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState()) + anchorYOffset();
        BlockPos anchor = new BlockPos(x, y, z);

        return Optional.of(new GenerationStub(anchor, builder ->
                builder.addPiece(new NbtTemplatePiece(templateId(), anchor, rotation, sizeX(), sizeY(), sizeZ()))));
    }

    private static long scatterHash(long seed, int chunkX, int chunkZ, long salt) {
        long h = seed ^ salt ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L);
        h = (h ^ (h >>> 33)) * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 29)) * 0xBF58476D1CE4E5B9L;
        return (h ^ (h >>> 32)) & Long.MAX_VALUE;
    }
}
