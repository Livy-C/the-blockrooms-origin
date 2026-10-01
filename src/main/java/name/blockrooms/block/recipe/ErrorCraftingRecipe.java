package name.blockrooms.block.recipe;

import net.minecraft.world.item.crafting.*;
import org.jspecify.annotations.Nullable;

public abstract class ErrorCraftingRecipe implements CraftingRecipe {
    private @Nullable PlacementInfo placementInfo;

    @Override
    public RecipeType<CraftingRecipe> getType() {
        return ModRecipeTypes.ERROR_CRAFTING.get();
    }

    @Override
    public abstract RecipeSerializer<? extends ErrorCraftingRecipe> getSerializer();

    @Override
    public String group() {
        return "";
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    protected abstract PlacementInfo createPlacementInfo();

    @Override
    public final PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = this.createPlacementInfo();
        }

        return this.placementInfo;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }
}
