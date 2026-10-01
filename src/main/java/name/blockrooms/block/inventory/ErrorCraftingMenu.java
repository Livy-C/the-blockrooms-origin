package name.blockrooms.block.inventory;

import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.recipe.ModRecipeTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.crafting.*;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ErrorCraftingMenu extends BaseCraftingMenu {
    public ErrorCraftingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    public Optional<RecipeHolder<CraftingRecipe>> getHolder(RecipeManager manager, CraftingInput input, ServerLevel level, @Nullable RecipeHolder<CraftingRecipe> recipe) {
        return manager.getRecipeFor(ModRecipeTypes.ERROR_CRAFTING.get(), input, level, recipe)
                .or(() -> manager.getRecipeFor(RecipeType.CRAFTING, input, level, recipe));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ERROR_CRAFTING_TABLE.get());
    }
}

