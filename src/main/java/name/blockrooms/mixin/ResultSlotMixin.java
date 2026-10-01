package name.blockrooms.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import name.blockrooms.Config;
import name.blockrooms.block.inventory.ErrorCraftingMenu;
import name.blockrooms.block.recipe.ModRecipeTypes;
import name.blockrooms.effect.ModMobEffects;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin extends Slot implements ResultSlotInvoker {
    public ResultSlotMixin(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @ModifyReturnValue(method = "getRemainingItems", at = @At("RETURN"))
    public NonNullList<ItemStack> getRemainingItems(NonNullList<ItemStack> list, CraftingInput input, Level level) {
        NonNullList<ItemStack> remainingItems;
        if (level instanceof ServerLevel serverLevel) {
            remainingItems = serverLevel.recipeAccess()
                    .getRecipeFor(ModRecipeTypes.ERROR_CRAFTING.get(), input, serverLevel)
                    .or(() -> serverLevel.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, serverLevel))
                    .map(recipe -> recipe.value().getRemainingItems(input))
                    .orElseGet(() -> invokeCopyAllInputItems(input));
        } else {
            remainingItems = CraftingRecipe.defaultCraftingReminder(input);
        }
        return remainingItems;
    }

    @Inject(method = "onTake", at = @At("TAIL"), remap = false)
    public void onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player != null && player.containerMenu instanceof ErrorCraftingMenu
                && player.getRandom().nextDouble() <= Config.TREMBLING_CHANCE.getAsDouble()) {
            player.addEffect(new MobEffectInstance(ModMobEffects.TREMBLING, 300 * 20, 2));
        }
    }
}
