package name.blockrooms.entity.projectiles;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.concurrent.atomic.AtomicReference;

public class ToolItemProjectile extends ItemProjectile{

    protected ToolItemProjectile(Level level, Entity owner, ItemStack stack) {
        super(level, owner, stack);
        if (!stack.has(DataComponents.TOOL)) throw new IllegalArgumentException("Stack must have a tool component");
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity le && level() instanceof ServerLevel sl){
            le.hurtServer(sl, damageSources().mobProjectile(this, getLivingOwner().orElse(null)), getToolATK() * 10);
            damageItem(20);
        }
        super.onHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        checkDiggableAndBreak(result);
    }

    public final float getToolATK(){
        ItemStack stack = getItemStack();
        AtomicReference<Float> base = new AtomicReference<>(0.0f);
        if(stack.has(DataComponents.TOOL) && stack.has(DataComponents.ATTRIBUTE_MODIFIERS)){
            var i =  stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if(i == null) return 0.0f;

            i.modifiers().forEach(a -> {
                if(a.attribute().equals(Attributes.ATTACK_DAMAGE)){
                    base.updateAndGet(v -> (float) (v + a.modifier().amount()));
                }
            });
        }
        return base.get();
    }
    public final void damageItem(int count){
        ItemStack stack = getItemStack();
        if(stack.isDamageableItem()){
            stack.setDamageValue(stack.getDamageValue() + count);
        }
        if(stack.getDamageValue() > stack.getMaxDamage()){
            this.discard();
        }
    }

    public final void checkDiggableAndBreak(BlockHitResult result){
        ItemStack stack = getItemStack();
        if(stack.has(DataComponents.TOOL)){
            Tool t = stack.get(DataComponents.TOOL);
            if(t != null){
                if(t.isCorrectForDrops(level().getBlockState(result.getBlockPos()))){
                    damageItem(2);
                    level().destroyBlock(result.getBlockPos(), true);
                    setDeltaMovement(getDeltaMovement().scale(0.9f));
                }
            }
        }
    }
}
