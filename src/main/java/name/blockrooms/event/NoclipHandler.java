package name.blockrooms.event;

import name.blockrooms.block.ModBlocks;
import name.blockrooms.util.FlexibleMap;
import name.blockrooms.util.ModLevels;
import name.blockrooms.util.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public class NoclipHandler {
    public record LevelWithChance(ResourceKey<Level> level, double chance) {}
    /** 玩家卡出判定的检查间隔（游戏刻）。 */
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final FlexibleMap<ResourceKey<Level>, BlockState, LevelWithChance> noclipMap = new FlexibleMap<>();

    public static void noclipByCondition(ServerPlayer player, BlockState state) {
        LevelWithChance destination = noclipMap.get(player.level().dimension(), state);
        if (destination == null) return;
        if (!(player.getRandom().nextDouble() <= destination.chance)) return;

        TeleportUtils.teleportPlayer(player, destination.level);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Level level = player.level();
        if (level.isClientSide()) return;
        if (player.isSpectator()) return;

        // 直接用世界时间对齐，按玩家各自每 20 刻检查一次。
        // （原实现是一个全局 static 计数器，会被世界里所有实体的 tick 一起消耗，
        //   实体越多玩家被抽检的频率越低，因此改为与实体数量无关。）
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) return;

        BlockPos pos = BlockPos.containing(player.getEyePosition());
        BlockState state = level.getBlockState(pos);
        if (state.isSuffocating(level, pos)) noclipByCondition(player, state);
    }

    @SubscribeEvent
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        noclipMap.put(Level.OVERWORLD,
                new LevelWithChance(ModLevels.BLOCKLEVEL_0, 0.2));
        noclipMap.put(ModLevels.BLOCKLEVEL_0, ModBlocks.NOCLIP_POINT.get().defaultBlockState(),
                new LevelWithChance(ModLevels.BLOCKLEVEL_1, 0.95));
    }
}