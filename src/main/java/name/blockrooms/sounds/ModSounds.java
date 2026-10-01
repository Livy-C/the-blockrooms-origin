package name.blockrooms.sounds;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Blockrooms.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BEGINNING =
            SOUNDS.register("music.beginning", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCKLEVEL1 =
            SOUNDS.register("music.blockrooms.blocklevel1", SoundEvent::createVariableRangeEvent);
    public static void register(IEventBus eventBus) { SOUNDS.register(eventBus); }
}
