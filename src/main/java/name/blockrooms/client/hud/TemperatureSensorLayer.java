package name.blockrooms.client.hud;

import name.blockrooms.client.TemperatureSensorState;
import name.blockrooms.item.ModItems;
import name.blockrooms.util.ModLevels;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.Locale;

public final class TemperatureSensorLayer implements GuiLayer {
    private static final int SCREEN_MARGIN = 4;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;
    private static final int BACKGROUND_ALPHA = 0x70;

    private static final int COLOR_COMFORT = 0xFFA8E0A8;
    private static final int COLOR_HOT = 0xFFFFB84A;
    private static final int COLOR_DANGER = 0xFFFF5533;
    private static final int COLOR_TITLE = 0xFFB8B8B8;

    private static final TemperatureSensorLayer INSTANCE = new TemperatureSensorLayer();

    public static TemperatureSensorLayer instance() {
        return INSTANCE;
    }

    private TemperatureSensorLayer() {
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!mc.level.dimension().equals(ModLevels.BLOCKLEVEL_2)) {
            return;
        }
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        if (!main.is(ModItems.TEMPERATURE_SENSOR.get()) && !off.is(ModItems.TEMPERATURE_SENSOR.get())) {
            return;
        }
        Float temperature = TemperatureSensorState.get();
        if (temperature == null) {
            return;
        }

        Font font = mc.font;
        String title = Component.translatable("item.blockrooms.temperature_sensor").getString();
        String value = String.format(Locale.ROOT, "%.1f℃", temperature);
        int valueColor = temperature > 45.0F ? COLOR_DANGER
                : temperature > 40.0F ? COLOR_HOT : COLOR_COMFORT;

        int lineHeight = font.lineHeight + 1;
        int panelWidth = Math.max(font.width(title), font.width(value)) + PADDING_X * 2;
        int panelHeight = 2 * lineHeight + PADDING_Y * 2;
        int x = SCREEN_MARGIN;
        int y = SCREEN_MARGIN;

        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND_ALPHA << 24);
        guiGraphics.text(font, Component.literal(title), x + PADDING_X, y + PADDING_Y, COLOR_TITLE, true);
        guiGraphics.text(font, Component.literal(value), x + PADDING_X, y + PADDING_Y + lineHeight, valueColor, true);
    }
}
