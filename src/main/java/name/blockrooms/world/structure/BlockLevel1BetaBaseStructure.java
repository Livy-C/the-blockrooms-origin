package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.world.generator.BlockLevel1Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class BlockLevel1BetaBaseStructure extends Structure {
    public static final MapCodec<BlockLevel1BetaBaseStructure> CODEC = simpleCodec(BlockLevel1BetaBaseStructure::new);

    public BlockLevel1BetaBaseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        BlockPos base = BlockLevel1Generator.betaBaseCenter(context.seed());
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
