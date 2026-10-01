package name.blockrooms.client.key;

import name.blockrooms.Blockrooms;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Blockrooms.MODID, value = Dist.CLIENT)
public class ModKeyBindings {
    public static KeyMapping noclippingKey = new KeyMapping(
            "key.blockrooms.noclipping",
            GLFW.GLFW_KEY_N,
            KeyMapping.Category.GAMEPLAY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(noclippingKey);
    }
}
