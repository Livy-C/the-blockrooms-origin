package name.blockrooms.event.level;

import name.blockrooms.util.ModLevels;
import net.minecraft.tags.EntityTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public class BlockLevel1IgniteHandler {
    static long tickCount = 0;
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isDarkOutside()) return;
        if (!event.getEntity().level().dimension().equals(ModLevels.BLOCKLEVEL_1)) return;
        tickCount++;
        if (tickCount == 20) {
            tickCount = 0;
            if (event.getEntity().is(EntityTypeTags.BURN_IN_DAYLIGHT)) {
                event.getEntity().igniteForSeconds(5.0f);
            }
        }
    }
}
