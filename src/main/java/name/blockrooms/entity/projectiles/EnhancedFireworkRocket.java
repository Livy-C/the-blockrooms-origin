package name.blockrooms.entity.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EnhancedFireworkRocket extends FireworkRocketEntity {
    public EnhancedFireworkRocket(EntityType<? extends FireworkRocketEntity> p_37027_, Level p_37028_) {
        super(p_37027_, p_37028_);
    }

    public EnhancedFireworkRocket(Level level, ItemStack stack, LivingEntity shooter) {
        super(level, stack, shooter);
    }


    public EnhancedFireworkRocket(Level level, ItemStack stack, Entity shooter, double x, double y, double z, boolean shotAtAngle) {
        super(level, stack, x, y, z, shotAtAngle);
        setOwner(shooter);
    }
    @Override
    protected void onHitBlock(BlockHitResult p_37069_) {
        BlockPos blockpos = new BlockPos(p_37069_.getBlockPos());
        this.level().getBlockState(blockpos).entityInside(this.level(), blockpos, this, InsideBlockEffectApplier.NOOP, true);
        if (this.level() instanceof ServerLevel serverlevel && this.hasExplosion()) {
            this.explode(serverlevel);
        }

        super.onHitBlock(p_37069_);
    }

    private boolean hasExplosion() {
        return !this.getExplosions().isEmpty();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level() instanceof ServerLevel serverlevel) {
            this.explode(serverlevel);
        }
    }


    private void explode(ServerLevel level){
        level.broadcastEntityEvent(this, (byte) 17);
        this.gameEvent(GameEvent.EXPLODE, this.getOwner());
        this.dealExplosionDamage(level);
        this.discard();
    }

    private void dealExplosionDamage(ServerLevel level) {
        float f = 0.0F;
        List<FireworkExplosion> list = this.getExplosions();
        if (!list.isEmpty()) {
            f = 7.5F + list.size() * 2;
        }

        if (f > 0.0F) {
            double d0 = 5.0;
            Vec3 vec3 = this.position();

            for (LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(8.0))) {
                if (livingentity != this.getOwner() && !(this.distanceToSqr(livingentity) > 25.0)) {
                    boolean flag = false;

                    for (int i = 0; i < 2; i++) {
                        Vec3 vec31 = new Vec3(livingentity.getX(), livingentity.getY(0.5 * i), livingentity.getZ());
                        HitResult hitresult = this.level().clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                        if (hitresult.getType() == HitResult.Type.MISS) {
                            flag = true;
                            break;
                        }
                    }

                    if (flag) {
                        float f1 = f * (float)Math.sqrt((d0 - this.distanceTo(livingentity)) / d0) * 1.5f;
                        livingentity.hurtServer(level, this.damageSources().fireworks(this, this.getOwner()), f1);
                        livingentity.igniteForSeconds(15.0f);
                    }
                }
            }
        }
    }

    private List<FireworkExplosion> getExplosions() {
        ItemStack itemstack = getItem();
        Fireworks fireworks = itemstack.get(DataComponents.FIREWORKS);
        return fireworks != null ? fireworks.explosions() : List.of();
    }
}
