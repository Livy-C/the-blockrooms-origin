package name.blockrooms.network;

import name.blockrooms.Blockrooms;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Blockrooms.MODID)
public class ModNetwork {
    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(NoclipPayload.TYPE, NoclipPayload.STREAM_CODEC, NoclipPayload::handleServer);
        registrar.playToServer(ElevatorTeleportPayload.TYPE, ElevatorTeleportPayload.STREAM_CODEC, ElevatorTeleportPayload::handleServer);
    }
}
