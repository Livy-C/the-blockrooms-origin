package name.blockrooms.effect;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Blockrooms.MODID);
    public static void register(IEventBus eventBus) { EFFECTS.register(eventBus); }

    public static final DeferredHolder<MobEffect, MobEffect> TREMBLING =
            EFFECTS.register("trembling", () -> new BaseMobEffect(MobEffectCategory.HARMFUL, 0x51765a));
}
