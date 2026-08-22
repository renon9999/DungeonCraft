package com.dungeoncraft.network;

import com.dungeoncraft.DungeonCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-bound floor-count choice made from the DC Portal selection screen. */
public record SelectFloorCountPayload(BlockPos portalPosition, int floorCount)
        implements CustomPacketPayload {
    public static final Type<SelectFloorCountPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "select_floor_count"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectFloorCountPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SelectFloorCountPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new SelectFloorCountPayload(
                            BlockPos.of(buffer.readLong()), buffer.readVarInt());
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer, SelectFloorCountPayload payload) {
                    buffer.writeLong(payload.portalPosition().asLong());
                    buffer.writeVarInt(payload.floorCount());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
