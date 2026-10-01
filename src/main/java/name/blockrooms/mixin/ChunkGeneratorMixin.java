package name.blockrooms.mixin;

import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin{

    @Redirect(method = "applyBiomeDecoration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;placeWithBiomeCheck(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean injected(PlacedFeature instance, WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos) {
        if(level.getLevel().dimension().equals(ModLevels.BLOCKLEVEL_4)){
            if (instance.feature().is(CaveFeatures.AMETHYST_GEODE) || instance.feature().is(MiscOverworldFeatures.LAKE_LAVA)){
                return false;
            }
        }
        return instance.placeWithBiomeCheck(level, generator, random, pos);
    }

}
