package name.blockrooms.event;

import name.blockrooms.Config;
import name.blockrooms.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

@EventBusSubscriber
public class RubyTransHandler {
    private static final String TIMER_KEY = "ruby_trans_timer";

    @SubscribeEvent
    public static void onItemEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (!item.getItem().is(Items.EMERALD)) return;

        Level level = item.level();
        if (level.isClientSide()) return;

        List<ItemEntity> ruby = level.getEntitiesOfClass(
                ItemEntity.class, item.getBoundingBox().inflate(5.0),
                e -> e.getItem().is(ModItems.RUBY) && e.isAlive()
        );

        CompoundTag data = item.getPersistentData();
        int timer = data.getInt(TIMER_KEY).orElse(0);

        if (!ruby.isEmpty()) {
            timer++;
            data.putInt(TIMER_KEY, timer);

            if (timer >= Config.RUBY_TRANS_TIME.getAsInt()) {
                item.discard();

                ItemEntity ruby_new = new ItemEntity(level, item.getX(), item.getY(), item.getZ(),
                        new ItemStack(ModItems.RUBY.asItem(), item.getItem().getCount())
                );
                ruby_new.setPickUpDelay(40);
                level.addFreshEntity(ruby_new);
            }
        } else {
            if (timer != 0) {
                data.remove(TIMER_KEY);
            }
        }
    }
}
