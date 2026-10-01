package name.blockrooms.network;

import io.netty.buffer.ByteBuf;
import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

public record ElevatorTeleportPayload(boolean upwards) implements CustomPacketPayload {
    private static final int MAX_SEARCH_HEIGHT = 16;
    public static final Type<ElevatorTeleportPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Blockrooms.MODID, "elevator_tp"));

    public static final StreamCodec<ByteBuf, ElevatorTeleportPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ElevatorTeleportPayload::upwards,
                    ElevatorTeleportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean isSafeDestination(ServerLevel level, BlockPos pos) {
        BlockState up1 = level.getBlockState(pos.above());
        BlockState up2 = level.getBlockState(pos.above().above());
        return level.getBlockState(pos).is(ModBlocks.QUARTZ_ELEVATOR)
                && up1.getBlock().isPossibleToRespawnInThis(up1)
                && up2.getBlock().isPossibleToRespawnInThis(up2);
    }

    @Nullable
    private BlockPos getTeleportDestination(ServerLevel level, BlockPos pos) {
        Direction direction = this.upwards ? Direction.UP : Direction.DOWN;
        int i = 0;
        do {
            pos = pos.relative(direction);
            if (++i > MAX_SEARCH_HEIGHT) return null;
        } while (!isSafeDestination(level, pos));
        return pos;
    }

    public static void handleServer(final ElevatorTeleportPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = player.level();
            if (player.getBlockStateOn().is(ModBlocks.QUARTZ_ELEVATOR)) {
                BlockPos blockDestination = payload.getTeleportDestination(level, player.getOnPos());
                if (blockDestination != null) {
                    Vec3 destination = Vec3.atBottomCenterOf(blockDestination.above());
                    player.teleportTo(destination.x, destination.y, destination.z);
                }
            }
        });
    }
}
