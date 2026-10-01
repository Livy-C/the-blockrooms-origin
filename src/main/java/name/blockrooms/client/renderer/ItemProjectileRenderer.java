package name.blockrooms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import name.blockrooms.entity.projectiles.ItemProjectile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class ItemProjectileRenderer extends EntityRenderer<ItemProjectile, ItemProjectileRenderState> {
    private final ItemModelResolver itemModelResolver;

    public ItemProjectileRenderer(EntityRendererProvider.Context p_270110_) {
        super(p_270110_);
        this.itemModelResolver = p_270110_.getItemModelResolver();
    }

    @Override
    public ItemProjectileRenderState createRenderState() {
        return new ItemProjectileRenderState();
    }

    @Override
    public void submit(ItemProjectileRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState != null && renderState.hasSubState()) {
            super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
            this.submitInner(renderState, poseStack, nodeCollector);
        }
    }

    @Override
    public void extractRenderState(ItemProjectile entity, ItemProjectileRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        ItemProjectile.ItemRenderState display$itemdisplay$itemrenderstate = entity.itemRenderState();
        if (display$itemdisplay$itemrenderstate != null) {
            this.itemModelResolver
                    .updateForNonLiving(
                            reusedState.item, display$itemdisplay$itemrenderstate.itemStack(), display$itemdisplay$itemrenderstate.itemTransform(), entity
                    );
        } else {
            reusedState.item.clear();
        }
    }

    public void submitInner(ItemProjectileRenderState p_433571_, PoseStack p_432839_, SubmitNodeCollector p_433402_) {
        p_433571_.item.submit(p_432839_, p_433402_, p_433571_.lightCoords, OverlayTexture.NO_OVERLAY, p_433571_.outlineColor);
    }
}
