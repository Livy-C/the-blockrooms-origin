package name.blockrooms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import name.blockrooms.entity.projectiles.BlockProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class BlockProjectileRenderer extends EntityRenderer<BlockProjectile, BlockDisplayEntityRenderState> {
    public BlockProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(BlockDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (state != null && state.hasSubState()) {
            super.submit(state, poseStack, nodeCollector, cameraRenderState);
            this.submitInner(state, poseStack, nodeCollector, state.lightCoords);
        }
    }

    @Override
    public BlockDisplayEntityRenderState createRenderState() {
        return new BlockDisplayEntityRenderState();
    }

    public void submitInner(BlockDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        state.blockModel.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }

    @Override
    public void extractRenderState(BlockProjectile entity, BlockDisplayEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.blockRenderState() != null) {
            Minecraft.getInstance().getBlockModelResolver().update(state.blockModel, entity.blockRenderState().blockState(), DisplayRenderer.BLOCK_DISPLAY_CONTEXT);
        } else {
            state.blockModel.clear();
        }
    }
}
