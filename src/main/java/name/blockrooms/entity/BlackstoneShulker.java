package name.blockrooms.entity;

import name.blockrooms.effect.ModMobEffects;
import name.blockrooms.entity.projectiles.BlockProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A shulker made of blackstone.
 *
 * <p>Traits:
 * <ul>
 *     <li>Fire / lava immune, and slowly regenerates while standing in lava.</li>
 *     <li>Its shell is nearly impenetrable: anything that is not a pickaxe-wielding living
 *     entity deals no damage at all, it just bounces the attacker back with a loud clank.</li>
 *     <li>When actually hurt by a pickaxe it retaliates: the attacker gets the Trembling
 *     effect, the shulker may teleport away and it may spit a blackstone shard at the
 *     attacker.</li>
 *     <li>Drops blackstone / obsidian (and a shell when killed by a player).</li>
 * </ul>
 */
public class BlackstoneShulker extends Shulker {
    private static final int TREMBLING_DURATION = 100;
    private static final int SHARD_COOLDOWN_TICKS = 60;
    private static final double MAX_SHARD_RANGE_SQR = 64.0D;

    private int shardCooldown;

    public BlackstoneShulker(EntityType<? extends BlackstoneShulker> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Shulker.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity directEntity = source.getDirectEntity();
        boolean pickaxeHit = directEntity instanceof LivingEntity attacker
                && attacker.getMainHandItem().is(ItemTags.PICKAXES);

        if (!pickaxeHit) {
            // Unavoidable damage (e.g. /kill or falling out of the world) must still work.
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return super.hurtServer(level, source, damage);
            }
            this.blockAttack(level, directEntity);
            return false;
        }

        if (!super.hurtServer(level, source, damage)) {
            return false;
        }

        if (directEntity instanceof LivingEntity attacker) {
            this.counterAttack(level, attacker);
        }
        return true;
    }

    /** Non-pickaxe hits are fully absorbed: clank and shove the attacker away. */
    private void blockAttack(ServerLevel level, Entity directEntity) {
        if (directEntity instanceof LivingEntity attacker) {
            Vec3 away = attacker.position().subtract(this.position());
            if (away.lengthSqr() > 1.0E-4D) {
                away = away.normalize();
                attacker.push(away.x * 0.8D, 0.35D, away.z * 0.8D);
                attacker.hurtMarked = true;
            }
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SHULKER_HURT_CLOSED, this.getSoundSource(), 1.0F, 1.0F);
    }

    /** Only a pickaxe can hurt it; when it does, the shulker fights back. */
    private void counterAttack(ServerLevel level, LivingEntity attacker) {
        attacker.addEffect(new MobEffectInstance(ModMobEffects.TREMBLING, TREMBLING_DURATION, 2, false, true));

        if (this.random.nextFloat() < 0.35F) {
            this.teleportSomewhere();
        }

        if (this.shardCooldown <= 0
                && this.random.nextFloat() < 0.5F
                && attacker.isAlive()
                && attacker.distanceToSqr(this) <= MAX_SHARD_RANGE_SQR) {
            Vec3 start = this.getEyePosition();
            Vec3 direction = attacker.getEyePosition().subtract(start);
            if (direction.lengthSqr() > 1.0E-4D) {
                direction = direction.normalize();
                BlockProjectile shard = BlockProjectile.of(level, this, Blocks.BLACKSTONE.defaultBlockState());
                shard.setPos(start.x, start.y, start.z);
                shard.shoot(direction.x, direction.y, direction.z, 1.6F, 1.0F);
                level.addFreshEntity(shard);
                this.shardCooldown = SHARD_COOLDOWN_TICKS;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.shardCooldown > 0) {
            this.shardCooldown--;
        }

        if (this.level().isClientSide()) {
            // Ash particles around the open lid.
            if (this.entityData.get(DATA_PEEK_ID) > 0 && this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.ASH,
                        this.getRandomX(0.8D), this.getY() + 0.9D, this.getRandomZ(0.8D),
                        0.0D, 0.02D, 0.0D);
            }
        } else if (this.tickCount % 20 == 0
                && this.isInLava()
                && this.getHealth() < this.getMaxHealth()) {
            // Blackstone is born in fire: it slowly heals inside lava.
            this.heal(2.0F);
        }
    }
}
