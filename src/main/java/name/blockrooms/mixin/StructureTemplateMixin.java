package name.blockrooms.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes the private fields of {@link StructureTemplate} needed by the
 * BlockLevel 1 template generator: the palette list (templates are placed
 * manually so that only the current chunk is written) and the entity list
 * (template entity NBT, e.g. item frames / armor stands).
 */
@Mixin(StructureTemplate.class)
public interface StructureTemplateMixin {
    @Accessor("palettes")
    List<StructureTemplate.Palette> getPalettes();

    @Accessor("entityInfoList")
    List<StructureTemplate.StructureEntityInfo> getEntityInfoList();
}
