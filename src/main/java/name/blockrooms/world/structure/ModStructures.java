package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Blockrooms.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Blockrooms.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<BlockLevel0OutpostStructure>> BMEG_OUTPOST_TYPE =
            STRUCTURE_TYPES.register("bmeg_outpost", () -> structureType(BlockLevel0OutpostStructure.CODEC));
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BMEG_OUTPOST_PIECE_TYPE =
            STRUCTURE_PIECE_TYPES.register("bmeg_outpost_piece", () -> (StructurePieceType.ContextlessType) BlockLevel0OutpostPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<BlockLevel1BetaBaseStructure>> BETA_BASE_TYPE =
            STRUCTURE_TYPES.register("quartz_beta_base", () -> structureType(BlockLevel1BetaBaseStructure.CODEC));
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BETA_BASE_PIECE_TYPE =
            STRUCTURE_PIECE_TYPES.register("quartz_beta_base_piece", () -> (StructurePieceType.ContextlessType) BlockLevel1BetaBasePiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<SpruceDoorStructure>> SPRUCE_DOOR_TYPE =
            STRUCTURE_TYPES.register("spruce_door", () -> structureType(SpruceDoorStructure.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<QuartzDoorStructure>> QUARTZ_DOOR_TYPE =
            STRUCTURE_TYPES.register("quartz_door", () -> structureType(QuartzDoorStructure.CODEC));
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BL2_DOOR_PIECE_TYPE =
            STRUCTURE_PIECE_TYPES.register("blocklevel2_door_piece", () -> (StructurePieceType.ContextlessType) BlockLevel2DoorPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<AbandonedCampStructure>> ABANDONED_CAMP_TYPE =
            STRUCTURE_TYPES.register("abandoned_camp", () -> structureType(AbandonedCampStructure.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<BlockLevel7VillagerCottageStructure>> BL7_COTTAGE_TYPE =
            STRUCTURE_TYPES.register("blocklevel7_villager_cottage", () -> structureType(BlockLevel7VillagerCottageStructure.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<RaftStructure>> RAFT_TYPE =
            STRUCTURE_TYPES.register("raft", () -> structureType(RaftStructure.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<SeabedCaveStructure>> SEABED_CAVE_TYPE =
            STRUCTURE_TYPES.register("seabed_cave", () -> structureType(SeabedCaveStructure.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<VoidBoatStructure>> VOID_BOAT_TYPE =
            STRUCTURE_TYPES.register("void_boat", () -> structureType(VoidBoatStructure.CODEC));
    public static final DeferredHolder<StructurePieceType, StructurePieceType> NBT_TEMPLATE_PIECE_TYPE =
            STRUCTURE_PIECE_TYPES.register("nbt_template_piece", () -> (StructurePieceType.StructureTemplateType) NbtTemplatePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<Bl3TemplateStructure>> BL3_TEMPLATE_TYPE =
            STRUCTURE_TYPES.register("bl3_template", () -> structureType(Bl3TemplateStructure.CODEC));

    private static <S extends Structure> StructureType<S> structureType(MapCodec<S> codec) {
        return () -> codec;
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECE_TYPES.register(eventBus);
    }
}
