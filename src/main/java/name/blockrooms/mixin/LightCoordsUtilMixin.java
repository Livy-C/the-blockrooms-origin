package name.blockrooms.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import name.blockrooms.event.client.DynamicLightingHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightCoordsUtil.class)
public class LightCoordsUtilMixin {
    @ModifyReturnValue(method = "getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"))
    private static int modifyLightCoords(int original, LightCoordsUtil.BrightnessGetter brightnessGetter, BlockAndLightGetter level, BlockState state, BlockPos pos) {
        int sky = LightCoordsUtil.sky(original);
        int block = LightCoordsUtil.block(original);
        int modified = DynamicLightingHandler.getLightWithOcclusion(level, pos);
        return LightCoordsUtil.pack(Math.max(block, modified), sky);
    }
}
