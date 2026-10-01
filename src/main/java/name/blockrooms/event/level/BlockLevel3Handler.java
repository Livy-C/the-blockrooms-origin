package name.blockrooms.event.level;

import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@EventBusSubscriber
public class BlockLevel3Handler {
    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getPlayer().isCreative() || event.getPlayer().isSpectator()) return;
        if (shouldHandle(event.getLevel())) {
            if (!event.getState().is(BlockTags.create(Identifier.fromNamespaceAndPath("c", "ores_in_ground/stone")))
                || !event.getState().is(Blocks.REDSTONE_WIRE)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (shouldHandle(event.getEntity().level())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (shouldHandle(event.getLevel())) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) ||
                (player.isCreative() || player.isSpectator())) return;
        if (shouldHandle(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!shouldHandle(event.getLevel())) return;
        if (event.getState().is(BlockTags.create(Identifier.fromNamespaceAndPath("c", "ores_in_ground/stone")))) {
            event.getLevel().setBlock(event.getPos(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!shouldHandle(event.getLevel())) return;
        if (event.getSpawnType() != EntitySpawnReason.NATURAL) return;

        if (hasSoulTorchNear(event.getLevel(), event.getPos())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
        } else {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    private static boolean shouldHandle(LevelAccessor level) {
        return level instanceof Level && ((Level) level).dimension().equals(ModLevels.BLOCKLEVEL_3);
    }

    private static boolean hasSoulTorchNear(LevelAccessor level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 2, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
                return true;
            }
        }
        return false;
    }
}
