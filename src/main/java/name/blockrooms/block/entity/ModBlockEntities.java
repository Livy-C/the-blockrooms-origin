package name.blockrooms.block.entity;

import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, Blockrooms.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeleporterBlockEntity>> TELEPORTER =
            BLOCK_ENTITY_TYPES.register("teleporter",
                    () -> new BlockEntityType<>(TeleporterBlockEntity::new, ModBlocks.TELEPORTER_BLOCK.get()));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
