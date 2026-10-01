package name.blockrooms.block;

import name.blockrooms.Blockrooms;
import name.blockrooms.block.inventory.ErrorCraftingMenu;
import name.blockrooms.item.FallableBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Blockrooms.MODID);
    public static void register(IEventBus eventBus) { BLOCKS.register(eventBus); }

    public static final DeferredBlock<Block> HEATED_IRON_BLOCK = BLOCKS.registerBlock(
            "heated_iron_block", HeatedIronBlock::new,
            properties -> properties.instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .mapColor(MapColor.FIRE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.IRON)
    );
    public static final DeferredBlock<BaseCraftingTableBlock> ERROR_CRAFTING_TABLE =
            BLOCKS.registerBlock("error_crafting_table",
                    properties -> new BaseCraftingTableBlock(
                            Component.translatable("container.error_crafting").withColor(ChatFormatting.RED.ordinal()),
                            ErrorCraftingMenu::new, properties),
                    properties -> properties.mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(2.5F).sound(SoundType.WOOD)
                            .ignitedByLava());
    public static final DeferredBlock<BaseCraftingTableBlock> STONE_CRAFTING_TABLE =
            BLOCKS.registerBlock("stone_crafting_table", BaseCraftingTableBlock::new,
                    properties -> properties.mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F));
    public static final DeferredBlock<DetectorTorchBlock> DETECTOR_TORCH =
            BLOCKS.registerBlock("detector_torch",
                    properties -> new DetectorTorchBlock(ParticleTypes.FLAME, properties),
                    properties -> properties.noCollision()
                            .instabreak()
                            .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)
                            .sound(SoundType.WOOD)
                            .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<DetectorWallTorchBlock> DETECTOR_WALL_TORCH =
            BLOCKS.registerBlock("detector_wall_torch",
                    properties -> new DetectorWallTorchBlock(ParticleTypes.FLAME, properties),
                    properties -> properties
                            .overrideLootTable(DETECTOR_TORCH.get().getLootTable())
                            .overrideDescription(DETECTOR_TORCH.get().getDescriptionId())
                            .noCollision()
                            .instabreak()
                            .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)
                            .sound(SoundType.WOOD)
                            .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<DetectorRedstoneLampBlock> DETECTOR_REDSTONE_LAMP_BLOCK =
            BLOCKS.registerBlock("detector_redstone_lamp", DetectorRedstoneLampBlock::new,
            properties -> properties
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .isValidSpawn(Blocks::always));
    public static final DeferredBlock<Block> QUARTZ_ELEVATOR = BLOCKS.registerBlock(
            "quartz_elevator",
            QuartzElevatorBlock::new,
            properties -> properties.mapColor(MapColor.QUARTZ)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(0.8F));
    public static final DeferredBlock<Block> SOFT_COBBLESTONE = BLOCKS.registerSimpleBlock(
            "soft_cobblestone",
            properties -> properties.mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(2.0F, 6.0F)
    );
    public static final DeferredBlock<Block> PROCESSED_SOFT_COBBLESTONE = BLOCKS.registerSimpleBlock(
            "processed_soft_cobblestone",
            properties -> properties.mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(2.0F, 6.0F)
    );
    public static final DeferredBlock<FallableBlock> FALLABLE_STONE = BLOCKS.registerBlock(
            "fallable_stone",
            properties -> new FallableBlock(new ColorRGBA(0xFF9C9C9C), properties),
            properties -> properties.mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(1.5F, 6.0F)
    );
    public static final DeferredBlock<Block> NOCLIP_POINT = BLOCKS.registerBlock(
            "eternal_noclip_point", NoclipPointBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_BLACK)
                    .noCollision()
                    .noLootTable()
                    .isSuffocating((state, level, pos) -> true)
                    .strength(-1.0F, 3600000.0F)
                    .pushReaction(PushReaction.IGNORE)
                    .sound(SoundType.STONE)
    );
    public static final DeferredBlock<TeleporterBlock> TELEPORTER_BLOCK = BLOCKS.registerBlock(
            "teleporter_block", TeleporterBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_BLACK)
                    .noCollision()
                    .instabreak()
                    .pushReaction(PushReaction.IGNORE)
                    .sound(SoundType.STONE)
    );
}
