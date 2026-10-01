package name.blockrooms.entity.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public class ArrowLikeProjectile extends Projectile {
    protected long life;

    protected ArrowLikeProjectile(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    protected ArrowLikeProjectile(EntityType<? extends Projectile> type, Level level, Entity owner){
        super(type, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1f, owner.getZ());
    }
    public boolean isNoPhysics() {
        return this.noPhysics;
    }
    @Override
    public void tick() {
        boolean flag = !this.isNoPhysics();
        Vec3 vec3 = this.getDeltaMovement();
        BlockPos blockpos = this.blockPosition();
        BlockState blockstate = this.level().getBlockState(blockpos);
        if (!blockstate.isAir() && flag) {
            VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
            if (!voxelshape.isEmpty()) {
                Vec3 vec31 = this.position();

                for (AABB aabb : voxelshape.toAabbs()) {
                    if (aabb.move(blockpos).contains(vec31)) {
                        this.setDeltaMovement(Vec3.ZERO);
                        break;
                    }
                }
            }
        }
        Vec3 vec32 = this.position();

        double dirX = flag ? vec3.x : -vec3.x;
        double dirZ = flag ? vec3.z : -vec3.z;
        float targetYaw = (float) (Mth.atan2(dirX, dirZ) * 180.0F / (float) Math.PI);
        float targetPitch = (float) (Mth.atan2(vec3.y, vec3.horizontalDistance()) * 180.0F / (float) Math.PI);
        this.setXRot(lerpRotation(this.getXRot(), targetPitch));
        this.setYRot(lerpRotation(this.getYRot(), targetYaw));
        this.checkLeftOwner();
        if (flag) {
            BlockHitResult blockhitresult = this.level()
                    .clipIncludingBorder(new ClipContext(vec32, vec32.add(vec3), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            this.stepMoveAndHit(blockhitresult);
        } else {
            this.setPos(vec32.add(vec3));
            this.applyEffectsFromBlocks();
        }
        life++;
        this.applyGravity();
        if(getDeltaMovement().lengthSqr() <= 0.01f || life >= getMaxLife())
            dropAndDiscard();
        super.tick();
    }

    private void stepMoveAndHit(BlockHitResult hitResult) {
        while (this.isAlive()) {
            Vec3 vec3 = this.position();
            ArrayList<EntityHitResult> arraylist = new ArrayList<>(this.findHitEntities(vec3, hitResult.getLocation()));
            arraylist.sort(Comparator.comparingDouble(p_481016_ -> vec3.distanceToSqr(p_481016_.getEntity().position())));
            EntityHitResult entityhitresult = arraylist.isEmpty() ? null : arraylist.getFirst();
            Vec3 vec31 = Objects.requireNonNullElse(entityhitresult, hitResult).getLocation();
            this.setPos(vec31);
            this.applyEffectsFromBlocks(vec3, vec31);
            if (this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
                this.handlePortal();
            }

            if (arraylist.isEmpty()) {
                if (this.isAlive() && hitResult.getType() != HitResult.Type.MISS) {
                    if (net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hitResult))
                        break;
                    this.hitTargetOrDeflectSelf(hitResult);
                    this.needsSync = true;
                }
                break;
            } else if (this.isAlive() && !this.noPhysics && entityhitresult.getType() != HitResult.Type.MISS) {
                if (net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, entityhitresult))
                    break;
                ProjectileDeflection projectiledeflection = this.hitTargetsOrDeflectSelf(arraylist);
                this.needsSync = true;
                if (projectiledeflection == ProjectileDeflection.NONE) {
                    continue;
                }
                break;
            }
        }

    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }
    private ProjectileDeflection hitTargetsOrDeflectSelf(Collection<EntityHitResult> hitResults) {
        for (EntityHitResult entityhitresult : hitResults) {
            ProjectileDeflection projectiledeflection = this.hitTargetOrDeflectSelf(entityhitresult);
            if (!this.isAlive() || projectiledeflection != ProjectileDeflection.NONE) {
                return projectiledeflection;
            }
        }

        return ProjectileDeflection.NONE;
    }
    protected Collection<EntityHitResult> findHitEntities(Vec3 start, Vec3 end) {
        return ProjectileUtil.getManyEntityHitResult(
                this.level(), this, start, end, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), this::canHitEntity, false
        );
    }

    protected boolean shouldReturn(){
        return false;
    }

    public final Optional<LivingEntity> getLivingOwner() {
        if(this.getOwner() instanceof LivingEntity le) return Optional.of(le);
        return Optional.empty();
    }

    public void dropAndDiscard(){
        this.discard();
    }

    public int getMaxLife(){
        return 200;
    }
}
