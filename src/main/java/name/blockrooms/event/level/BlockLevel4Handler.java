package name.blockrooms.event.level;

import name.blockrooms.Blockrooms;
import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber
public class BlockLevel4Handler {
    private static final ResourceKey<LootTable> DROP_IDENTIFIER =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "gameplay/blocklevel4_drop"));

    @SubscribeEvent
    public static void onBlockDrop(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        boolean hasSilkTouch = event.getTool().getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH)) > 0;
        if (level.dimension() == ModLevels.BLOCKLEVEL_4 && !hasSilkTouch) {
            event.getDrops().clear();
            BlockPos pos = event.getPos();
            LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(DROP_IDENTIFIER);
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.BLOCK_STATE, event.getState())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, event.getTool())
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, event.getBreaker())
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, event.getBlockEntity())
                    .create(LootContextParamSets.BLOCK);
            for (ItemStack stack : lootTable.getRandomItems(params)) {
                event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level() instanceof ServerLevel level && level.dimension() == ModLevels.BLOCKLEVEL_4
                && event.getEntity() instanceof Monster && !(level.isBrightOutside())) {
            if (!(event.getSource().getEntity() instanceof ServerPlayer player && player.isCreative())) {
                event.setNewDamage(0);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().dimension() == ModLevels.BLOCKLEVEL_4
                && event.getEntity() instanceof Player player) {
            MobEffectInstance effect = event.getEffectInstance();
            if (effect.is(MobEffects.POISON)) {
                player.forceAddEffect(new MobEffectInstance(
                        MobEffects.POISON, MobEffectInstance.INFINITE_DURATION,
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()),
                        event.getEffectSource());
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeavingLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().dimension() == ModLevels.BLOCKLEVEL_4
                && event.getEntity() instanceof LivingEntity entity
                && entity.getEffect(MobEffects.POISON) != null) {
            entity.removeEffect(MobEffects.POISON);
        }
    }
}
