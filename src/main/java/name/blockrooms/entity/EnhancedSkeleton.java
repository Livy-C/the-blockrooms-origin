package name.blockrooms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

public class EnhancedSkeleton extends Skeleton {

    public EnhancedSkeleton(EntityType<? extends Skeleton> p_480588_, Level p_479309_) {
        super(p_480588_, p_479309_);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes().add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
    }

    @Override
    public void tick() {

        super.tick();
    }
}
