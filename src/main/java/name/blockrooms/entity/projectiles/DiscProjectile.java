package name.blockrooms.entity.projectiles;

import name.blockrooms.util.VectorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscProjectile extends ItemProjectile {
    private static final Logger log = LoggerFactory.getLogger(DiscProjectile.class);
    private boolean soundPlayed = false;
    private Identifier lastSoundLocation;

    DiscProjectile(Level level, Entity owner, ItemStack stack) {
        super(level, owner, stack);
        if (!stack.is(Tags.Items.MUSIC_DISCS) || !stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            throw new IllegalArgumentException("ItemStack must be a music disc");
        }

    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity owner = getOwner();
        if (owner instanceof LivingEntity leowner) {
            if (this.level() instanceof ServerLevel sl) {
                if (result.getEntity().is(getOwner()) && shouldReturn()) {
                    if (this.life <= 10) return;
                    dropAndDiscard();
                } else if (result.getEntity() instanceof LivingEntity le) {

                    float damage = 2.0f;
                    switch(getItemStack().getRarity()){
                        case UNCOMMON -> damage = 3.0f;
                        case RARE -> damage = 5.0f;
                    }
                    le.hurtServer(sl, damageSources().mobProjectile(this, leowner), damage);
                }
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if(level().isClientSide()) return;
        if(life <= 5) return;
        if(level() instanceof ServerLevel l){
            if(l.getBlockState(result.getBlockPos()).is(Blocks.JUKEBOX)){
                BlockEntity entity = l.getBlockEntity(result.getBlockPos());
                if(entity instanceof JukeboxBlockEntity jbe){
                    if(!jbe.getTheItem().isEmpty()){
                        ItemEntity jbei = new ItemEntity(l, this.getX(), this.getY(), this.getZ(), jbe.getTheItem());
                        l.addFreshEntity(jbei);
                    }
                    jbe.setTheItem(getItemStack());
                    this.discard();
                }
            } else {
                super.onHitBlock(result);
            }
        } else {
            return;
        }

    }

    @Override
    public void tick() {
        setReturn(VectorUtils.horizontalDisSqr(position(), getShotPosition()) >= 256.0f);
        if(shouldReturn())
            setDeltaMovement(VectorUtils.directionTo(position(), getShotPosition(), 4.0f));
        super.tick();
        ItemStack stack = getItemStack();
        if(stack == null || stack.isEmpty() || !stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            this.discard();
            return;
        }
        if(this.shouldReturn() && VectorUtils.horizontalDisSqr(position(), getShotPosition()) <= 1.0f){
            dropAndDiscard();
            return;
        }
        if (soundPlayed) return;
        if(stack.is(Items.MUSIC_DISC_11)){
            if(shouldReturn()){
                level().explode(this, this.getX(), this.getY(), this.getZ(), 5.0f, Level.ExplosionInteraction.MOB);
                discard();
            }
        } else {
            JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
            if (playable != null){
                playable.song().unwrap().ifRight(song -> {
                    SoundEvent soundEvent = song.soundEvent().value();
                    playSound(soundEvent, 1.0f, 1.0f);
                    lastSoundLocation = soundEvent.location();
                    soundPlayed = true;
                });
            }
        }

    }

    @Override
    public void onRemoval(RemovalReason reason) {
        Minecraft.getInstance().getSoundManager().stop(lastSoundLocation, this.getSoundSource());
        super.onRemoval(reason);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        this.life = 0;
        setShotPosition(position());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }


    @Override
    protected double getDefaultGravity() {
        return 0.05f;
    }

    @Override
    public void setReturn(boolean value) {
        if (shouldReturn()) return;
        super.setReturn(value);
    }


}
