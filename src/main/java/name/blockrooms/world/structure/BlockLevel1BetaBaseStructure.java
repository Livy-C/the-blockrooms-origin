package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class BlockLevel1BetaBaseStructure extends Structure {
    public static final MapCodec<BlockLevel1BetaBaseStructure> CODEC = simpleCodec(BlockLevel1BetaBaseStructure::new);

    /** 基地中心取点范围：限定在世界中心 128 个区块（2048 格）内 */
    private static final int BASE_RANGE = 128 * 16;
    private static final long BASE_SEED_XOR = 0x7F3C9E21A4B5D6E7L;

    public BlockLevel1BetaBaseStructure(StructureSettings settings) {
        super(settings);
    }

    /**
     * 由维度种子派生的唯一「石英β基地」位置。
     * （原为 {@code BlockLevel1Generator.betaBaseCenter}；BL1 生成器换回模板式后不再需要它，
     * 遂移入本类，取值逻辑与数值保持不变。）
     */
    public static BlockPos betaBaseCenter(long seed) {
        RandomSource random = RandomSource.create(seed ^ BASE_SEED_XOR);
        int x = random.nextInt(BASE_RANGE * 2) - BASE_RANGE;
        int z = random.nextInt(BASE_RANGE * 2) - BASE_RANGE;
        return new BlockPos(x, 1, z);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        BlockPos base = betaBaseCenter(context.seed());
        ChunkPos chunkPos = context.chunkPos();
        if (SectionPos.blockToSectionCoord(base.getX()) != chunkPos.x()
                || SectionPos.blockToSectionCoord(base.getZ()) != chunkPos.z()) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(base, builder -> builder.addPiece(new BlockLevel1BetaBasePiece(base))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BETA_BASE_TYPE.get();
    }
}
