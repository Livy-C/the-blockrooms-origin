package name.blockrooms.entity.projectiles;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class UndamagedThrownEnderpearl extends ThrownEnderpearl {
    public UndamagedThrownEnderpearl(EntityType<? extends ThrownEnderpearl> p_478915_, Level p_480017_) {
        super(p_478915_, p_480017_);
    }

    public UndamagedThrownEnderpearl(Level level, LivingEntity owner, ItemStack stack){
        super(level, owner, stack);
    }

    @Override
    protected void onHit(HitResult result) {
        for (int i = 0; i < 32; i++) {
            this.level()
                    .addParticle(
                            ParticleTypes.PORTAL,
                            this.getX(),
                            this.getY() + this.random.nextDouble() * 2.0,
                            this.getZ(),
                            this.random.nextGaussian(),
                            0.0,
                            this.random.nextGaussian()
                    );
        }

        if (this.level() instanceof ServerLevel serverlevel && !this.isRemoved()) {
            Entity entity = this.getOwner();
            if (entity != null && isAllowedToTeleportOwner(entity, serverlevel)) {
                Vec3 vec3 = this.oldPosition();
                if (entity instanceof ServerPlayer serverplayer) {
                    if (serverplayer.connection.isAcceptingMessages()) {
                        net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl event = net.neoforged.neoforge.event.EventHooks.onEnderPearlLand(serverplayer, this.getX(), this.getY(), this.getZ(), this, 5.0F, result);
                        if (!event.isCanceled()) { // Don't indent to lower patch size
                            if (this.isOnPortalCooldown()) {
                                entity.setPortalCooldown();
                            }

                            ServerPlayer serverplayer1 = serverplayer.teleport(
                                    new TeleportTransition(
                                            serverlevel, event.getTarget(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING
                                    )
                            );

                            this.playSound(serverlevel, vec3);
                        } //Forge: End
                    }
                } else {
                    Entity entity1 = entity.teleport(
                            new TeleportTransition(serverlevel, vec3, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING)
                    );
                    if (entity1 != null) {
                        entity1.resetFallDistance();
                    }

                    this.playSound(serverlevel, vec3);
                }

                this.discard();
            } else {
                this.discard();
            }
        }
    }
    private void playSound(Level level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
    }
    private static boolean isAllowedToTeleportOwner(Entity entity, Level level) {
        if (entity.level().dimension() == level.dimension()) {
            return !(entity instanceof LivingEntity livingentity) ? entity.isAlive() : livingentity.isAlive() && !livingentity.isSleeping();
        } else {
            return entity.canUsePortal(true);
        }
    }

}
