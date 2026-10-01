package name.blockrooms.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import name.blockrooms.event.client.DynamicLightingHandler;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @ModifyReturnValue(at = @At("RETURN"), method = "getBlockLightLevel")
    private int modifyBlockLightLevel(int original, T entity, BlockPos pos) {
        int modified = DynamicLightingHandler.getLightWithOcclusion(entity.level(), pos);
        return Math.max(modified, original);
    }
}
