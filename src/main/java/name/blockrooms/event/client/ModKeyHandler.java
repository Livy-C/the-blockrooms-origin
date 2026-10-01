package name.blockrooms.event.client;

import name.blockrooms.block.ModBlocks;
import name.blockrooms.client.key.ModKeyBindings;
import name.blockrooms.network.ElevatorTeleportPayload;
import name.blockrooms.network.NoclipPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT)
public class ModKeyHandler {
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        /* if (ModKeyBindings.noclippingKey.consumeClick()) {
            ClientPacketDistributor.sendToServer(new NoclipPayload());
        } */
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || !player.getBlockStateOn().is(ModBlocks.QUARTZ_ELEVATOR)) {
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (event.getKey() == mc.options.keyJump.getKey().getValue()) {
            ClientPacketDistributor.sendToServer(new ElevatorTeleportPayload(true));
        } else if (event.getKey() == mc.options.keyShift.getKey().getValue()) {
            ClientPacketDistributor.sendToServer(new ElevatorTeleportPayload(false));
        }
    }
}
