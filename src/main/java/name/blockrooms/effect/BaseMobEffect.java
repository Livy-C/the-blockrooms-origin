package name.blockrooms.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BaseMobEffect extends MobEffect {
    protected BaseMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
