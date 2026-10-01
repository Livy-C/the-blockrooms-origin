package name.blockrooms.client.renderer;

import name.blockrooms.Blockrooms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.resources.Identifier;

public class BlackstoneShulkerRenderer extends ShulkerRenderer {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Blockrooms.MODID, "textures/entity/blackstone_shulker.png");

    public BlackstoneShulkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(ShulkerRenderState state) {
        return TEXTURE;
    }
}
