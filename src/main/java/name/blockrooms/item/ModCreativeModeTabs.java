package name.blockrooms.item;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Blockrooms.MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKROOMS = TABS.register("blockrooms",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blockrooms"))
                    .icon(Items.SANDSTONE::getDefaultInstance)
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ALMOND_MILK_BUCKET);
                        output.accept(ModItems.RUBY);
                        output.accept(ModItems.RUBY_SWORD);
                        output.accept(ModItems.RUBY_SHOVEL);
                        output.accept(ModItems.RUBY_PICKAXE);
                        output.accept(ModItems.RUBY_AXE);
                        output.accept(ModItems.RUBY_HOE);
                        output.accept(ModItems.RUBY_SPEAR);
                        output.accept(ModItems.HEATED_IRON_BLOCK);
                        output.accept(ModItems.ERROR_CRAFTING_TABLE);
                        output.accept(ModItems.STONE_CRAFTING_TABLE);
                        output.accept(ModItems.STRING_AXE);
                        output.accept(ModItems.DETECTOR_TORCH);
                        output.accept(ModItems.DETECTOR_REDSTONE_LAMP);
                        output.accept(ModItems.QUARTZ_ELEVATOR);
                        output.accept(ModItems.TELEPORTER_BLOCK);
                        output.accept(ModItems.GUNBOW);
                        output.accept(ModItems.GLOWSTONE_LANTERN);
                        output.accept(ModItems.SOFT_COBBLESTONE);
                        output.accept(ModItems.PROCESSED_SOFT_COBBLESTONE);
                        output.accept(ModItems.STICK_BUNDLE);
                        output.accept(ModItems.BLOOD_ZOMBIE_SPAWN_EGG);
                        output.accept(ModItems.BLACKSTONE_SHULKER_SPAWN_EGG);
                    })
                    .build());
    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
