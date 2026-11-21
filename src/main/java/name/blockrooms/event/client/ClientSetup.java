package name.blockrooms.event.client;

import name.blockrooms.Blockrooms;
import name.blockrooms.client.hud.TemperatureSensorLayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = Blockrooms.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(Blockrooms.MODID, "temperature_sensor"),
                TemperatureSensorLayer.instance());
    }
}
