package name.blockrooms.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class DurabilityConsumingItem extends Item {
    private final int interval;
    private final int damage;

    public DurabilityConsumingItem(Properties properties, int interval, int damage) {
        super(properties);
        this.interval = interval;
        this.damage = damage;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (level.getGameTime() % interval == 0 && entity instanceof Player player) {
            if (slot != null && (slot.equals(EquipmentSlot.MAINHAND) || slot.equals(EquipmentSlot.OFFHAND))) {
                if (stack.getDamageValue() + 2 == stack.getMaxDamage()) {
                    level.playSound(null, BlockPos.containing(entity.position()), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS);
                }
                if (!player.isSpectator()) {
                    stack.hurtWithoutBreaking(damage, player);
                }
            }
        }
    }
}
