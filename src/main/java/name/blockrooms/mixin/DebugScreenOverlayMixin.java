package name.blockrooms.mixin;

import name.blockrooms.util.ModLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (minecraft.level != null && minecraft.player != null) {
            if (ModLevels.isInBlockrooms(minecraft.level.dimension()) && !minecraft.player.isCreative()) {
                ci.cancel();
                invokeExtractLines(graphics, List.of(Component.translatable("gui.in_blockrooms").getString()), true);
            }
        }
    }

    @Invoker("extractLines")
    public abstract void invokeExtractLines(GuiGraphicsExtractor graphics, List<String> lines, boolean leftSide);
}
