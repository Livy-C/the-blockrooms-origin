package name.blockrooms.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class HeatInsulationArmor {
    public static final Predicate<LivingEntity> HAS_FULL_SET = HeatInsulationArmor::isFullSet;

    public static final Consumer<LivingEntity> APPLY_PASSIVE_EFFECT = entity -> {
        if (HAS_FULL_SET.test(entity)) {
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, false, false));
        }
    };

    public static int wornCount(LivingEntity entity) {
        int count = 0;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.HEAT_INSULATION_HELMET.get())) {
            count++;
        }
        if (entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.HEAT_INSULATION_CHESTPLATE.get())) {
            count++;
        }
        if (entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.HEAT_INSULATION_LEGGINGS.get())) {
            count++;
        }
        if (entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.HEAT_INSULATION_BOOTS.get())) {
            count++;
        }
        return count;
    }

    public static boolean isFullSet(LivingEntity entity) {
        return wornCount(entity) == 4;
    }

    private HeatInsulationArmor() {
    }
}