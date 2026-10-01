package name.blockrooms.item.impl;

import name.blockrooms.entity.projectiles.*;
import name.blockrooms.item.components.ModDataComponents;
import name.blockrooms.util.ItemList;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GunBowItem extends Item {
    public GunBowItem(Properties p_40660_) {
        super(p_40660_);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        if (level.getGameTime() % 20 == 0) {
            if (player.getRandom().nextFloat() > 0.15f) return;

            consumeRandomItem(player, stack);
        }

    }
    private void consumeRandomItem(Player player, ItemStack gunBow) {
        List<Integer> validSlots = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && !(stack.getItem() instanceof GunBowItem)) {
                validSlots.add(i);
            }
        }
        if (validSlots.isEmpty()) return;
        int slot = validSlots.get(player.getRandom().nextInt(validSlots.size()));
        ItemStack target = inventory.getItem(slot);

        int countToTake = 1;
        if (target.getCount() > 1) {
            countToTake = player.getRandom().nextInt(target.getCount()) + 1;
        }
        ItemStack consumed = target.copy();
        consumed.setCount(countToTake);
        ItemList chargedItems = new ItemList(gunBow.getOrDefault(ModDataComponents.CHARGED_ITEMS, List.of()));

        chargedItems.add(consumed);
        gunBow.set(ModDataComponents.CHARGED_ITEMS, chargedItems);
        target.shrink(countToTake);
        if (target.isEmpty()) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (isCharged(stack)) {
            shoot((ServerLevel) level, player, stack);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
    public static boolean isCharged(ItemStack gunbow) {
        List<ItemStack> chargedItems = gunbow.getOrDefault(ModDataComponents.CHARGED_ITEMS, new ArrayList<>());
        return gunbow.has(ModDataComponents.CHARGED_ITEMS) || !chargedItems.isEmpty();
    }
    public void shoot(ServerLevel level, Player player, ItemStack gunbow) {
        List<ItemStack> chargedProjectiles = new ItemList(gunbow.getOrDefault(ModDataComponents.CHARGED_ITEMS, List.of()));
        if(chargedProjectiles.isEmpty()) return;
        ItemStack ammo = chargedProjectiles.getFirst();
        if(ammo.getItem() instanceof EggItem){
            Chicken chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
            if (chicken != null) {
                chicken.setAge(-24000);
                Optional.ofNullable(ammo.get(DataComponents.CHICKEN_VARIANT))
                        .ifPresent(chicken::setVariant);
                shootEntity(level, player, chicken, 1, 0.0f);
            }

        } else if(ammo.getItem() instanceof MinecartItem mi &&  mi.asItem().equals(Items.TNT_MINECART)){
            MinecartTNT tnt = new TNTMinecartProjectile(EntityTypes.TNT_MINECART, level);
            shootEntity(level, player, tnt, 1, 0.0f);
        }else if(ammo.getItem() instanceof BlockItem && ammo.getItem().equals(Items.TNT)) {
            PrimedTnt tnt = new TNTProjectile(level, player.getX(), player.getEyeY() - 0.1f, player.getZ(), player);
            shootEntity(level, player, tnt, 1, 0.0f);
        }else {
            Projectile projectile = createProjectileForAmmo(level, player, gunbow, ammo);
            if (projectile != null) {
                float velocity = getVelocityForAmmo(ammo);
                Projectile.spawnProjectile(
                        projectile,
                        level,
                        gunbow,
                        p_360045_ -> shootProjectile(player, p_360045_, 1, velocity, 0.0f, 0.0f, null)
                );
            }

        }


        if (ammo.getCount() <= 1) {
            chargedProjectiles.remove(ammo);
        } else {
            ammo.shrink(1);
        }
        gunbow.set(ModDataComponents.CHARGED_ITEMS, chargedProjectiles);
    }

    private Projectile createProjectileForAmmo(Level level, Player player, ItemStack gunbow, ItemStack ammo) {
        if (ammo.getItem() instanceof ArrowItem) {
            return createArrow(level, player, gunbow, ammo, false);
        } else if (ammo.getItem() instanceof EnderpearlItem) {
            return new UndamagedThrownEnderpearl(level, player, ammo);
        } else if(ammo.getItem() instanceof FireworkRocketItem){
            return new EnhancedFireworkRocket(level, ammo, player, player.getX(), player.getEyeY() - 0.15F, player.getZ(), true);
        }else if(ammo.getItem() instanceof ProjectileItem pi){
            return pi.asProjectile(level, player.position(), ammo, Direction.getApproximateNearest(player.getViewVector(1.0f)));
        }else if (ammo.getItem() instanceof BlockItem bi) {
            return BlockProjectile.of(level, player, bi.getBlock().defaultBlockState());
        } else {
            ItemStack o = ammo.copy();
            o.setCount(1);
            return ItemProjectile.of(level, player, o);
        }
    }

    private static float getVelocityForAmmo(ItemStack ammo) {
        SecureRandom source = new SecureRandom();
        if (ammo.getItem() instanceof ArrowItem) {
            return 6.0f;
        } else if (ammo.getItem() instanceof EnderpearlItem) {
            return 4.5f;
        } else if(ammo.getItem() instanceof ProjectileItem){
            return 1.5f * source.nextFloat(2.0f, 5.0f);
        }else if (ammo.getItem() instanceof BlockItem) {
            return 3.5f;
        }
        return 5.0f;
    }
    protected Projectile createArrow(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        ArrowItem arrowitem = ammo.getItem() instanceof ArrowItem arrowitem1 ? arrowitem1 : (ArrowItem)Items.ARROW;
        AbstractArrow abstractarrow = arrowitem.createArrow(level, ammo, shooter, weapon);
        if (isCrit) {
            abstractarrow.setCritArrow(true);
        }

        return abstractarrow;
    }
    public static Vector3f calculateShootVector(LivingEntity shooter,
                                                float speed, float inaccuracy) {
        Vec3 direction = shooter.getViewVector(1.0F);
        RandomSource random = shooter.getRandom();

        Vec3 velocity = direction.scale(speed);
        if (inaccuracy > 0) {
            double spreadX = (random.nextDouble() - 0.5) * inaccuracy;
            double spreadY = (random.nextDouble() - 0.5) * inaccuracy;
            double spreadZ = (random.nextDouble() - 0.5) * inaccuracy;
            velocity = velocity.add(spreadX, spreadY, spreadZ);
        }

        return velocity.toVector3f();
    }
    protected void shootProjectile(
            LivingEntity p_40896_, Projectile p_332122_, int p_331865_, float p_40900_, float p_40902_, float p_40903_, @Nullable LivingEntity p_330303_
    ) {
        Vector3f vector3f = calculateShootVector(p_40896_, p_40900_, p_40902_);

        p_332122_.shoot(vector3f.x(), vector3f.y(), vector3f.z(), p_40900_, p_40902_);
        p_40896_.level().playSound(null, p_40896_.getX(), p_40896_.getY(), p_40896_.getZ(), SoundEvents.CROSSBOW_SHOOT, p_40896_.getSoundSource(), 1.0F, 0.0f);
    }
    protected void shootEntity(Level level, LivingEntity shooter, Entity entity, float p_40900_, float p_40902_){
        entity.snapTo(shooter.getX(), shooter.getY(), shooter.getZ(), shooter.getYRot(), 0.0F);
        entity.setDeltaMovement(new Vec3(calculateShootVector(shooter, 6.0f, 0.5f)));
        level.addFreshEntity(entity);
    }
}
