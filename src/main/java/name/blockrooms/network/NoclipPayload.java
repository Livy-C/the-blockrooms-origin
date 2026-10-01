package name.blockrooms.network;

import io.netty.buffer.ByteBuf;
import name.blockrooms.Blockrooms;
import name.blockrooms.event.NoclipHandler;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NoclipPayload() implements CustomPacketPayload {
    public static final Type<NoclipPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Blockrooms.MODID, "noclip"));

    public static final StreamCodec<ByteBuf, NoclipPayload> STREAM_CODEC =
            StreamCodec.unit(new NoclipPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(final NoclipPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            player.causeFoodExhaustion(3);
            NoclipHandler.noclipByCondition(player, player.getBlockStateOn());
            //player.hurtServer(player.level(), player.damageSources().inWall(), 3);
        });
    }
}
