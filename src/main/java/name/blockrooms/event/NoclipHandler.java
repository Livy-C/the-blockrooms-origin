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
    private static int counter = 0;
    private static final FlexibleMap<ResourceKey<Level>, BlockState, LevelWithChance> noclipMap = new FlexibleMap<>();

    public static void noclipByCondition(ServerPlayer player, BlockState state) {
        LevelWithChance destination = noclipMap.get(player.level().dimension(), state);
        if (destination == null) return;
        if (!(player.getRandom().nextDouble() <= destination.chance)) return;

        TeleportUtils.teleportPlayer(player, destination.level);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) return;
        if (counter++ < 20) return;
        counter = 0;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

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