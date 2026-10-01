package name.blockrooms.event;

import name.blockrooms.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

@EventBusSubscriber
public class TremblingEffectHandler {
    private static final double MAX_OFFSET = 4.0;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen().isPauseScreen()) return;
        Player player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ModMobEffects.TREMBLING)
                && Minecraft.getInstance().level != null) {
            int amplifier = Objects.requireNonNull(player.getEffect(ModMobEffects.TREMBLING)).getAmplifier();
            RandomSource random = Minecraft.getInstance().level.getRandom();
            long window = Minecraft.getInstance().getWindow().handle();
            double[] xPos = new double[1], yPos = new double[1];
            GLFW.glfwGetCursorPos(window, xPos, yPos);
            GLFW.glfwSetCursorPos(window,
                    xPos[0] + (random.nextDouble() - 0.5) * 2 * (amplifier - 1) * MAX_OFFSET,
                    yPos[0] + (random.nextDouble() - 0.5) * 2 * (amplifier - 1) * MAX_OFFSET
            );
        }
    }
}
