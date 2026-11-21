package name.blockrooms.network;

import io.netty.buffer.ByteBuf;
import name.blockrooms.Blockrooms;
import name.blockrooms.client.TemperatureSensorState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public record TemperaturePayload(float temperature) implements CustomPacketPayload {
    public static final Type<TemperaturePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Blockrooms.MODID, "temperature"));
    public static final StreamCodec<ByteBuf, TemperaturePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TemperaturePayload::temperature, TemperaturePayload::new);

    public static void handle(TemperaturePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TemperatureSensorState.set(payload.temperature()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
