package name.blockrooms.mixin;

import name.blockrooms.util.ModLevels;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(StructureManager.class)
public class StructureManagerMixin {
    @Shadow
    @Final
    private LevelAccessor level;

    @Unique
    private static final ResourceKey<Structure> VILLAGE_PLAINS =
            ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("village_plains"));

    @Inject(method = "startsForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Ljava/util/List;", at = @At(value = "RETURN"), cancellable = true)
    private void filter(SectionPos sectionPos, Structure structure, CallbackInfoReturnable<List<StructureStart>> cir) {
        if (level instanceof WorldGenLevel l) {
            if (l.getLevel().dimension().equals(ModLevels.BLOCKLEVEL_4)) {
                Identifier key = l.getLevel().registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .getKey(structure);
                if (!VILLAGE_PLAINS.identifier().equals(key)) {
                    cir.setReturnValue(List.of());
                }
            }
        }
    }
}
