package name.blockrooms.item.consumables;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;

public record DamageEffect(float amount, float probability) implements ConsumeEffect {
    public static final MapCodec<DamageEffect> CODEC = RecordCodecBuilder.mapCodec(
            p_366712_ -> p_366712_.group(
                            Codec.FLOAT.fieldOf("amount").forGetter(DamageEffect::amount),
                            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(DamageEffect::probability)
                    )
                    .apply(p_366712_, DamageEffect::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DamageEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            DamageEffect::amount,
            ByteBufCodecs.FLOAT,
            DamageEffect::probability,
            DamageEffect::new
    );
    @Override
    public Type<? extends ConsumeEffect> getType() {
        return ModConsumeEffects.DAMAGE.get();
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity entity) {
        if(level.isClientSide() || !(level instanceof ServerLevel sl)) return false;
        var d = level.damageSources().mobAttack(entity);
        if(entity.getRandom().nextFloat() <= probability) return entity.hurtServer(sl, d, amount);
        else return false;
    }

    public DamageEffect(float amount){
        this(amount, 1.0f);
    }
}
