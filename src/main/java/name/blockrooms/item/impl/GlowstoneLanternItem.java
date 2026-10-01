package name.blockrooms.item.impl;

import name.blockrooms.item.DurabilityConsumingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jspecify.annotations.Nullable;

public class GlowstoneLanternItem extends DurabilityConsumingItem {
    public GlowstoneLanternItem(Properties properties, int interval) {
        super(properties, interval, 1);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (stack.getEnchantmentLevel(level.registryAccess().getOrThrow(Enchantments.MENDING)) > 0) {
            stack.shrink(1);
            level.playSound(null, BlockPos.containing(entity.position()), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS);
        }
        super.inventoryTick(stack, level, entity, slot);
    }
}
