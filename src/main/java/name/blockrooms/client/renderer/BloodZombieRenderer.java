package name.blockrooms.client.renderer;

import name.blockrooms.Blockrooms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

public class BloodZombieRenderer extends ZombieRenderer {
    private static final Identifier BLOOD_ZOMBIE_LOCATION = Identifier.fromNamespaceAndPath(Blockrooms.MODID,"textures/entity/blood_zombie.png");
    public BloodZombieRenderer(EntityRendererProvider.Context p_174456_) {
        super(p_174456_);
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState p_467832_) {
        return BLOOD_ZOMBIE_LOCATION;
    }
}
