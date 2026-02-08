package com.ryvione.gatheringchunks.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;

public record S2COpenConfigPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2COpenConfigPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, "open_config"));
    public static final StreamCodec<FriendlyByteBuf, S2COpenConfigPacket> CODEC = StreamCodec
            .unit(new S2COpenConfigPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
