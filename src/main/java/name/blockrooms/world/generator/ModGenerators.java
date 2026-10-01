package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModGenerators {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, Blockrooms.MODID);
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> BL0_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel0_generator", () -> BlockLevel0Generator.CODEC);
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> BL1_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel1_generator", () -> BlockLevel1Generator.CODEC);
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> BL4_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel4_generator", () -> BlockLevel4Generator.CODEC);
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> GALLERY_GENERATOR =
            CHUNK_GENERATORS.register("the_gallery_generator", () -> TheGalleryGenerator.CODEC);
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> BL2_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel2_generator", () -> BlockLevel2Generator.CODEC);
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> BL3_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel3_generator", () -> BlockLevel3Generator.CODEC);
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>> NULL_GENERATOR =
            CHUNK_GENERATORS.register("blocklevel_null_generator", () -> BlockLevelNullGenerator.CODEC);
    public static void register(IEventBus eventBus) { CHUNK_GENERATORS.register(eventBus); }
}
