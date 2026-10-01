package name.blockrooms.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.*;
import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.inventory.BaseCraftingMenu;
import name.blockrooms.block.inventory.ErrorCraftingMenu;
import name.blockrooms.block.recipe.ErrorCraftingRecipe;
import name.blockrooms.block.recipe.ModRecipeTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.NonNull;

import java.util.List;

@JeiPlugin
public class BlockroomsJeiPlugin implements IModPlugin {
    protected static RecipeMap recipes;
    public static final IRecipeType<ErrorCraftingRecipe> ERROR_CRAFTING =
            IRecipeType.create(Blockrooms.MODID, "crafting_error", ErrorCraftingRecipe.class);

    public static boolean isJEIAvailable() {
        return ModList.get().isLoaded("jei");
    }

    @Override
    public @NonNull Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ErrorCraftingRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ErrorCraftingRecipe> errorCraftingRecipes = recipes
                .byType(ModRecipeTypes.ERROR_CRAFTING.get()).stream()
                .map(holder -> (ErrorCraftingRecipe) holder.value())
                .toList();

        registration.addRecipes(ERROR_CRAFTING, errorCraftingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(RecipeTypes.CRAFTING, ModBlocks.ERROR_CRAFTING_TABLE, ModBlocks.STONE_CRAFTING_TABLE);
        registration.addCraftingStation(ERROR_CRAFTING, ModBlocks.ERROR_CRAFTING_TABLE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(BaseCraftingMenu.class, MenuType.CRAFTING, RecipeTypes.CRAFTING, 1, 9, 10, 36);
        registration.addRecipeTransferHandler(ErrorCraftingMenu.class, MenuType.CRAFTING, ERROR_CRAFTING, 1, 9, 10, 36);
    }
}
