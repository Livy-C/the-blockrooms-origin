package name.blockrooms.entity.projectiles;

import name.blockrooms.entity.ModEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.joml.Vector3fc;

public class ItemProjectile extends ArrowLikeProjectile {
    private static final EntityDataAccessor<Boolean> RETURN = SynchedEntityData.defineId(ItemProjectile.class, EntityDataSerializers.BOOLEAN);

    protected boolean updateRenderState;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(
            ItemProjectile.class, EntityDataSerializers.ITEM_STACK
    );
    private static final EntityDataAccessor<Vector3fc> SHOT_POS = SynchedEntityData.defineId(ItemProjectile.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(ItemProjectile.class, EntityDataSerializers.BYTE);
    private ItemRenderState itemRenderState;

    protected ItemStack getItemStack() {
        return this.entityData.get(DATA_ITEM_STACK_ID);
    }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ITEM_STACK_ID.equals(accessor) || DATA_ITEM_DISPLAY_ID.equals(accessor)) {
            this.updateRenderState = true;
        }
    }
    public ItemProjectile(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
        setItemTransform(ItemDisplayContext.FIXED);
    }
    protected ItemProjectile(Level level, Entity owner, ItemStack stack){
        super(ModEntities.ITEM_PROJECTILE.get(), level, owner);
        updateItemStack(stack);
        setItemTransform(ItemDisplayContext.FIXED);
    }
    public static ItemProjectile of(Level level, Entity owner, ItemStack stack){
        if(stack.is(Tags.Items.MUSIC_DISCS) && stack.has(DataComponents.JUKEBOX_PLAYABLE)){
            return new DiscProjectile(level, owner, stack);
        } else if(stack.has(DataComponents.TOOL)){
            return new ToolItemProjectile(level, owner, stack);
        }
        return new ItemProjectile(level, owner, stack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
        builder.define(DATA_ITEM_DISPLAY_ID, ItemDisplayContext.FIXED.getId());
        builder.define(SHOT_POS, Vec3.ZERO.toVector3f());
        builder.define(RETURN, false);
    }
    public ItemRenderState itemRenderState() {
        return this.itemRenderState;
    }
    private void setItemTransform(ItemDisplayContext itemTransform) {
        this.entityData.set(DATA_ITEM_DISPLAY_ID, itemTransform.getId());
    }

    private ItemDisplayContext getItemTransform() {
        return ItemDisplayContext.BY_ID.apply(this.entityData.get(DATA_ITEM_DISPLAY_ID));
    }
    @Override
    public void tick() {
        if (updateRenderState) {
            updateRenderSubState();
            updateRenderState = false;
        }
        super.tick();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        dropAndDiscard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if(result.getEntity().is(getOwner())) return;
        dropAndDiscard();
    }

    public void updateItemStack(ItemStack stack) {
        this.setItemStack(stack);
        this.updateRenderState = true;
    }

    protected void updateRenderSubState() {
        this.itemRenderState = new ItemRenderState(this.getItemStack(), getItemTransform());
    }

    public void setItemStack(ItemStack stack){
        this.entityData.set(DATA_ITEM_STACK_ID, stack);
    }
    public record ItemRenderState(ItemStack itemStack, ItemDisplayContext itemTransform) {
    }
    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected boolean shouldReturn() {
        return this.entityData.get(RETURN);
    }

    public Vec3 getShotPosition() {
        return new Vec3(this.entityData.get(SHOT_POS));
    }
    public void setShotPosition(Vec3 shotPosition) {
        this.entityData.set(SHOT_POS, shotPosition.toVector3f());
    }
    public void setReturn(boolean value) {
        this.entityData.set(RETURN, value);
    }

    public void dropAndDiscard(){
        if(this.level() instanceof ServerLevel sl){
            ItemEntity entity = new ItemEntity(sl, this.getX(), this.getY(), this.getZ(), this.getItemStack());
            sl.addFreshEntity(entity);
        }

        this.discard();
    }
}

