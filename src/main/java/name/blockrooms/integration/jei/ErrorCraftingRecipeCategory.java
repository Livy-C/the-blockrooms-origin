package name.blockrooms.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.recipe.ErrorCraftingRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;

public class ErrorCraftingRecipeCategory extends AbstractRecipeCategory<ErrorCraftingRecipe> {
    public static final int width = 116;
    public static final int height = 54;
    private final ICraftingGridHelper craftingGridHelper;

    public ErrorCraftingRecipeCategory(IGuiHelper guiHelper) {
        super(BlockroomsJeiPlugin.ERROR_CRAFTING, Component.translatable("gui.jei.category.error_crafting"), guiHelper.createDrawableItemLike(ModBlocks.ERROR_CRAFTING_TABLE), width, height);
        this.craftingGridHelper = guiHelper.createCraftingGridHelper();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ErrorCraftingRecipe recipe, IFocusGroup focuses) {
        var display = recipe.display().getFirst();
        craftingGridHelper.createAndSetIngredientsFromDisplays(builder, getIngredients(display), getWidth(display), getHeight(display));
        craftingGridHelper.createAndSetOutputs(builder, display.result());
    }

    private static List<SlotDisplay> getIngredients(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay) {
            return shapedCraftingRecipeDisplay.ingredients();
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay) {
            return shapelessCraftingRecipeDisplay.ingredients();
        } else {
            return List.of();
        }
    }

    private static int getWidth(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay) {
            return shapedCraftingRecipeDisplay.width();
        } else return 0;
    }

    private static int getHeight(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay) {
            return shapedCraftingRecipeDisplay.height();
        } else return 0;
    }
}