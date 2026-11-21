package name.blockrooms.event;

import name.blockrooms.environment.BlockLevel2Temperature;
import name.blockrooms.util.ModLevels;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class BlockLevel2TemperatureHandler {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide() || !player.level().dimension().equals(ModLevels.BLOCKLEVEL_2)) {
            return;
        }
        BlockLevel2Temperature.onPlayerTick(player);
    }
}