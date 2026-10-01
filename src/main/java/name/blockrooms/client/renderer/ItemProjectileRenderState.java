package name.blockrooms.client.renderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class ItemProjectileRenderState extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();

    public boolean hasSubState() {
        return !this.item.isEmpty();
    }
}
