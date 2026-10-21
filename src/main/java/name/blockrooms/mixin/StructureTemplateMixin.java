package name.blockrooms.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplate.class)
public interface StructureTemplateMixin {
    @Accessor("entityInfoList")
    List<StructureTemplate.StructureEntityInfo> getEntityInfoList();

    /**
     * 26.2 起 {@code StructureTemplate.palettes} 是 private（1.21.11 时还是 public），
     * BL1 生成器需要按调色板 0 逐格手动放置，因此加一个 accessor。
     */
    @Accessor("palettes")
    List<StructureTemplate.Palette> getPalettes();
}
