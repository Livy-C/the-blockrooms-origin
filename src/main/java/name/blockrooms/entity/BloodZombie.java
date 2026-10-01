package name.blockrooms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

public class BloodZombie extends Zombie {
    public BloodZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }
    public static AttributeSupplier.@NonNull Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
}
