package name.blockrooms.block.inventory;

import name.blockrooms.block.BaseCraftingTableBlock;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class BaseCraftingMenu extends CraftingMenu {
    protected final ContainerLevelAccess access;
    protected final Player player;
    protected boolean placingRecipe;

    public BaseCraftingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
        this.access = access;
        this.player = playerInventory.player;
    }

    public Optional<RecipeHolder<CraftingRecipe>> getHolder(RecipeManager manager, CraftingInput input, ServerLevel level, @Nullable RecipeHolder<CraftingRecipe> recipe) {
        return manager.getRecipeFor(RecipeType.CRAFTING, input, level, recipe);
    }

    protected static void slotChangedCraftingGrid(BaseCraftingMenu menu, ServerLevel level, Player player, CraftingContainer craftSlots, ResultContainer resultSlots, @Nullable RecipeHolder<CraftingRecipe> recipe) {
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer serverplayer = (ServerPlayer) player;
        RecipeManager manager = level.getServer().getRecipeManager();

        Optional<RecipeHolder<CraftingRecipe>> holder = menu.getHolder(manager, input, level, recipe);
        ItemStack result = holder.filter(recipeHolder -> resultSlots.setRecipeUsed(serverplayer, recipeHolder))
                .map(RecipeHolder::value)
                .map(craftingRecipe -> craftingRecipe.assemble(input))
                .filter(recipeResult -> recipeResult.isItemEnabled(level.enabledFeatures()))
                .orElse(ItemStack.EMPTY);

        resultSlots.setItem(0, result);
        menu.setRemoteSlot(0, result);
        serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, result));
    }

    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> {
            Block block = level.getBlockState(pos).getBlock();
            return (block instanceof CraftingTableBlock || block instanceof BaseCraftingTableBlock) && player.isWithinBlockInteractionRange(pos, 4.0);
        }, true);
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (!this.placingRecipe) {
            this.access.execute((level, pos) -> {
                if (level instanceof ServerLevel serverlevel) {
                    slotChangedCraftingGrid(this, serverlevel, this.player, this.craftSlots, this.resultSlots, null);
                }
            });
        }
    }

    public void beginPlacingRecipe() {
        this.placingRecipe = true;
    }

    @Override
    public void finishPlacingRecipe(ServerLevel level, RecipeHolder<CraftingRecipe> holder) {
        this.placingRecipe = false;
        slotChangedCraftingGrid(this, level, this.player, this.craftSlots, this.resultSlots, holder);
    }
}
