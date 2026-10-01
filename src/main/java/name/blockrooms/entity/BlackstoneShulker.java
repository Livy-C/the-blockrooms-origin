package name.blockrooms.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;

public class BlackstoneShulker extends Shulker {
    public BlackstoneShulker(EntityType<? extends BlackstoneShulker> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity entity = source.getDirectEntity();
        return !(entity instanceof LivingEntity sourceEntity
                    && !sourceEntity.getMainHandItem().is(ItemTags.PICKAXES))
                && super.hurtServer(level, source, damage);
    }
}
