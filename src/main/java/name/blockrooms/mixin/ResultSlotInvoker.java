package name.blockrooms.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ResultSlot.class)
public interface ResultSlotInvoker {
    @Invoker("copyAllInputItems")
    NonNullList<ItemStack> invokeCopyAllInputItems(CraftingInput input);
}
