package name.blockrooms.client.renderer;

import name.blockrooms.entity.projectiles.BlockProjectile;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class BlockProjectileRenderState extends EntityRenderState {
    public BlockProjectile.BlockRenderState blockRenderState;

    public boolean hasSubState() {
        return this.blockRenderState != null;
    }
}

