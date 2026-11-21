package name.blockrooms.item;

import name.blockrooms.environment.BlockLevel2Temperature;
import name.blockrooms.network.TemperaturePayload;
import name.blockrooms.util.ModLevels;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

public class TemperatureSensorItem extends Item {

    private static final int PUSH_INTERVAL = 20;

    public TemperatureSensorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (slot == null || !(entity instanceof ServerPlayer sp)
                || (!slot.equals(EquipmentSlot.MAINHAND) && !slot.equals(EquipmentSlot.OFFHAND))
                || !sp.level().dimension().equals(ModLevels.BLOCKLEVEL_2)) {
            return;
        }
        if (sp.tickCount % PUSH_INTERVAL != 0) {
            return;
        }
        float temperature = BlockLevel2Temperature.temperatureAt(level, sp.blockPosition());
        PacketDistributor.sendToPlayer(sp, new TemperaturePayload(temperature));
    }
}
