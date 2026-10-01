package name.blockrooms.item.consumables;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModConsumeEffects {
    public static final DeferredRegister<ConsumeEffect.Type<?>> CONSUME_EFFECTS = DeferredRegister.create(Registries.CONSUME_EFFECT_TYPE, Blockrooms.MODID);

    public static final DeferredHolder<ConsumeEffect.Type<?>, ConsumeEffect.Type<DamageEffect>> DAMAGE = CONSUME_EFFECTS.register("damage", () -> new ConsumeEffect.Type<>(DamageEffect.CODEC, DamageEffect.STREAM_CODEC));
}
