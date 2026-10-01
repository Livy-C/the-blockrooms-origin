package name.blockrooms.item.components;

import name.blockrooms.Blockrooms;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Blockrooms.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> CHARGED_ITEMS = DATA_COMPONENTS.register("charged_items", () -> new DataComponentType.Builder<List<ItemStack>>().persistent(ItemStack.CODEC.listOf()).build());

    public static void register(IEventBus bus){
        DATA_COMPONENTS.register(bus);
    }
}
