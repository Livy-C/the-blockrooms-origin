package name.blockrooms.event.client;

import name.blockrooms.util.ModLevels;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber
public class BlockroomsTitleHandler {
    private static final String BLOCKROOMS_TITLE = "Minecraft 1.ψ.5* | The Blockrooms";

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().player != null
            && ModLevels.isInBlockrooms(Minecraft.getInstance().player)) {
            Minecraft.getInstance().getWindow().setTitle(BLOCKROOMS_TITLE);
        } else Minecraft.getInstance().updateTitle();
    }
}
