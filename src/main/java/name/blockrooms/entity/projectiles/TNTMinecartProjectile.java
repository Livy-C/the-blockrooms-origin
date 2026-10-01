package name.blockrooms.entity.projectiles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class TNTMinecartProjectile extends MinecartTNT {
    public TNTMinecartProjectile(EntityType<? extends MinecartTNT> p_478532_, Level p_481071_) {
        super(p_478532_, p_481071_);
    }

    protected Collection<EntityHitResult> findHitEntities(Vec3 start, Vec3 end) {
        return ProjectileUtil.getManyEntityHitResult(
                this.level(), this, start, end, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), (entity) -> true, false
        );
    }

    @Override
    public void tick() {
        super.tick();
        while (this.isAlive()) {
            Vec3 vec32 = this.position();
            Vec3 vec3 = getDeltaMovement();
            HitResult hitResult = this.level()
                    .clipIncludingBorder(new ClipContext(vec32, vec32.add(vec3), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            ArrayList<EntityHitResult> arraylist = new ArrayList<>(this.findHitEntities(vec3, hitResult.getLocation()));
            arraylist.sort(Comparator.comparingDouble(p_481016_ -> vec3.distanceToSqr(p_481016_.getEntity().position())));
            EntityHitResult entityhitresult = arraylist.isEmpty() ? null : arraylist.getFirst();
            if(entityhitresult != null){
                hitResult = entityhitresult;
            }
            if (this.isAlive() && hitResult.getType() != HitResult.Type.MISS) {
                explode();
                this.needsSync = true;
            }
        }
    }
    protected void explode() {
        if (this.level() instanceof ServerLevel serverlevel && serverlevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            this.level()
                    .explode(
                            this,
                            Explosion.getDefaultDamageSource(this.level(), this),
                            null,
                            this.getX(),
                            this.getY(0.0625),
                            this.getZ(),
                            (float) (6.0f * this.random.nextDouble() * 1.5),
                            false,
                            Level.ExplosionInteraction.TNT
                    );
        }
    }


    @Override
    public int getFuse() {
        return -1;
    }
}
