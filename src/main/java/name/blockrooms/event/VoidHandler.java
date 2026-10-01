package name.blockrooms.event;

import name.blockrooms.util.ModLevels;
import name.blockrooms.util.TeleportUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class VoidHandler {
    @SubscribeEvent
    public static void onEntityTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (ModLevels.isInBlockrooms(sp.level().dimension())) {
                if (sp.getY() < sp.level().getMinY() - 32) {
                    if (!sp.level().dimension().equals(ModLevels.BLOCKLEVEL_NULL)) {
                        TeleportUtils.teleportPlayer(sp, ModLevels.BLOCKLEVEL_NULL);
                    }
                    else {
                        if (sp.getItemBySlot(EquipmentSlot.BODY).is(Items.ELYTRA)) {
                            TeleportUtils.teleportPlayer(sp, ModLevels.BLOCKLEVEL_1);
                        } else {
                            TeleportUtils.teleportPlayer(sp, ModLevels.BLOCKLEVEL_0);
                        }
                    }
                }
            }
        }
    }
}
