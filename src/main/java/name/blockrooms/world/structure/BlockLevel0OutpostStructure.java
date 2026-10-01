package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.world.generator.BlockLevel0Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
public class BlockLevel0OutpostStructure extends Structure {
    public static final MapCodec<BlockLevel0OutpostStructure> CODEC = simpleCodec(BlockLevel0OutpostStructure::new);

    public BlockLevel0OutpostStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        BlockPos outpost = BlockLevel0Generator.outpostCenter(context.seed());
        ChunkPos chunkPos = context.chunkPos();
        if (SectionPos.blockToSectionCoord(outpost.getX()) != chunkPos.x()
                || SectionPos.blockToSectionCoord(outpost.getZ()) != chunkPos.z()) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(outpost, builder -> builder.addPiece(new BlockLevel0OutpostPiece(outpost))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BMEG_OUTPOST_TYPE.get();
    }
}
