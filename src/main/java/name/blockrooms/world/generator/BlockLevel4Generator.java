package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

public class BlockLevel4Generator extends NoiseBasedChunkGenerator {
    public static final MapCodec<BlockLevel4Generator> CODEC = RecordCodecBuilder.mapCodec(
            p_255585_ -> p_255585_.group(
                            BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel4Generator::getBiomeSource),
                            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(BlockLevel4Generator::generatorSettings)
                    )
                    .apply(p_255585_, p_255585_.stable(BlockLevel4Generator::new))
    );

    public BlockLevel4Generator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        super.buildSurface(worldGenRegion, structureManager, randomState, chunk);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for(int y = -64;y < -50;y++){
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.VOID_AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
                }
                chunk.setBlockState(new BlockPos(x, -50, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
            }
        }

    }

    @Override
    public void applyCarvers(WorldGenRegion p_224224_, long p_224225_, RandomState p_224226_, BiomeManager p_224227_, StructureManager p_224228_, ChunkAccess p_224229_) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
    }
}
