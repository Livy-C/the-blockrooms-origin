package name.blockrooms.mixin;

import name.blockrooms.block.recipe.ModRecipeTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(RecipeCraftingHolder.class)
public interface RecipeCraftingHolderMixin extends RecipeCraftingHolder {
    @Redirect(
            method = "awardUsedRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;awardRecipes(Ljava/util/Collection;)I"
            )
    )
    default int awardRecipes(Player player, Collection<RecipeHolder<?>> recipes) {
        if (this.getRecipeUsed().value().getType() != ModRecipeTypes.ERROR_CRAFTING.get()) {
            player.awardRecipes(recipes);
        }
        return 0;
    }
}
