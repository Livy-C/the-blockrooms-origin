package name.blockrooms.event.client;

import name.blockrooms.Config;
import name.blockrooms.util.ModLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.widget.ModsButton;

@EventBusSubscriber(value = Dist.CLIENT)
public class BlockroomsScreenHandler {
    private static final String BLOCKROOMS_TITLE = "Minecraft 1.ψ.5* | The Blockrooms";
    private static final Component RETURN_TO_GAME = Component.translatable("menu.returnToGame");

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().player != null
            && ModLevels.isInBlockrooms(Minecraft.getInstance().player)) {
            Minecraft.getInstance().getWindow().setTitle(BLOCKROOMS_TITLE);
        } else Minecraft.getInstance().updateTitle();
    }

    /* @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Post event) {
        if (!Config.DO_DISABLE_PAUSE_MENU_BUTTON.getAsBoolean()) return;
        if (!(event.getScreen() instanceof PauseScreen)) return;
        if (Minecraft.getInstance().player != null && !ModLevels.isInBlockrooms(Minecraft.getInstance().player)) return;

        boolean temp = event.getKeyEvent().hasAltDown() && event.getKeyEvent().hasControlDown();
        for (var listener : event.getScreen().children()) {
            if (listener instanceof Button button && button.getMessage().equals(CommonComponents.disconnectButtonLabel(Minecraft.getInstance().isLocalServer()))) {
                button.active = temp;
            }
        }
    } */

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!Config.DO_DISABLE_PAUSE_MENU_BUTTON.getAsBoolean()) return;
        if (!(event.getScreen() instanceof PauseScreen)) return;
        if (Minecraft.getInstance().player != null && !ModLevels.isInBlockrooms(Minecraft.getInstance().player)) return;

        for (var listener : event.getListenersList()) {
            if (listener instanceof Button button) {
                if (!(button.getMessage().equals(RETURN_TO_GAME))) {
                    button.active = false;
                }
                if (button instanceof ModsButton && !Config.DO_DISABLE_MOD_LIST_BUTTON.getAsBoolean()) {
                    button.active = true;
                }
            }
        }
    }
}
