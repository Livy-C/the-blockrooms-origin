package name.blockrooms.event.level;

import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;

@EventBusSubscriber
public class BlockLevel1LightHandler {
    private static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final Map<ChunkPos, List<BlockPos>> DETECTOR_BLOCKS = new HashMap<>();

    private static boolean wasDay = false;

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        Level level = event.getChunk().getLevel();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.BLOCKLEVEL_1)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        if (DETECTOR_BLOCKS.containsKey(pos)) {
            return; // already tracked
        }
        // One-time scan of this chunk; afterwards only cached positions are touched.
        List<BlockPos> found = new ArrayList<>();
        chunk.findBlocks(BlockLevel1LightHandler::isValidDetectorBlock, (p, state) -> found.add(p.immutable()));
        DETECTOR_BLOCKS.put(pos, found);
        applyDayState(chunk, isDay(level));
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        Level level = event.getChunk().getLevel();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.BLOCKLEVEL_1)) {
            return;
        }
        DETECTOR_BLOCKS.remove(event.getChunk().getPos());
    }

    /** Player placement: add the position to the chunk's cache when it is a detector block. */
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        Level level = toServerLevel(event.getLevel());
        if (level == null || !level.dimension().equals(ModLevels.BLOCKLEVEL_1)) {
            return;
        }
        if (!isValidDetectorBlock(event.getPlacedBlock())) {
            return;
        }
        List<BlockPos> positions = DETECTOR_BLOCKS.computeIfAbsent(ChunkPos.containing(event.getPos()), k -> new ArrayList<>());
        if (!positions.contains(event.getPos())) {
            positions.add(event.getPos().immutable());
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BreakBlockEvent event) {
        Level level = toServerLevel(event.getLevel());
        if (level == null || !level.dimension().equals(ModLevels.BLOCKLEVEL_1)) {
            return;
        }
        List<BlockPos> positions = DETECTOR_BLOCKS.get(ChunkPos.containing(event.getPos()));
        if (positions != null) {
            positions.remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Level level = event.getServer().getLevel(ModLevels.BLOCKLEVEL_1);
        if (level != null) {
            wasDay = isDay(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DETECTOR_BLOCKS.clear();
    }

    @SubscribeEvent
    public static void onDayNightChanged(LevelTickEvent.Pre event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.BLOCKLEVEL_1)) {
            return;
        }
        boolean isDay = isDay(level);
        if (wasDay == isDay) {
            return;
        }
        wasDay = isDay;

        long startTime = System.currentTimeMillis();
        // Single-threaded server: chunk load/unload events cannot interleave here,
        // and getChunkNow never force-loads a chunk.
        for (ChunkPos pos : DETECTOR_BLOCKS.keySet()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
            if (chunk != null) {
                applyDayState(chunk, isDay);
            }
        }
        long time = System.currentTimeMillis() - startTime;
        Blockrooms.LOGGER.info("Took {}ms for BlockLevel 1 to handle one cycle", time);
    }

    private static void applyDayState(LevelChunk chunk, boolean isDay) {
        List<BlockPos> positions = DETECTOR_BLOCKS.get(chunk.getPos());
        if (positions == null || positions.isEmpty()) {
            return;
        }
        Iterator<BlockPos> it = positions.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockState state = chunk.getBlockState(pos);

            if (!isValidDetectorBlock(state)) {
                it.remove(); // stale entry (block removed/moved by a non-tracked source)
                continue;
            }
            if (state.getValue(LIT) == isDay) {
                continue;
            }
            chunk.getLevel().scheduleTick(pos, state.getBlock(), 20);
//            BlockState newState = state.setValue(LIT, isDay);
//            chunk.setBlockState(pos, newState);
//            chunk.getLevel().sendBlockUpdated(pos, state, newState, Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isValidDetectorBlock(BlockState state) {
        return state.is(ModBlocks.DETECTOR_TORCH) || state.is(ModBlocks.DETECTOR_WALL_TORCH)
                || state.is(ModBlocks.DETECTOR_REDSTONE_LAMP_BLOCK);
    }

    private static boolean isDay(Level level) {
        return level.isBrightOutside();
    }

    private static Level toServerLevel(LevelAccessor accessor) {
        if (!(accessor instanceof Level level) || level.isClientSide()) {
            return null;
        }
        return level;
    }
}
