package name.blockrooms.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;

public class ModToolMaterials {
    public static final ToolMaterial RUBY = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10, ItemTags.DIAMOND_TOOL_MATERIALS
    );
    public static final ToolMaterial STRING = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 16.0F, 0.0F, 15, ItemTags.WOODEN_TOOL_MATERIALS
    );
}
