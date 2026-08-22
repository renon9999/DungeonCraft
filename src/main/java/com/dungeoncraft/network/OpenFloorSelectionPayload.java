package com.dungeoncraft.network;

import com.dungeoncraft.DungeonCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Opens the client-side floor-count selection screen for a specific DC Portal. */
public record OpenFloorSelectionPayload(BlockPos portalPosition) implements CustomPacketPayload {
    public static final Type<OpenFloorSelectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "open_floor_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFloorSelectionPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public OpenFloorSelectionPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new OpenFloorSelectionPayload(BlockPos.of(buffer.readLong()));
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer, OpenFloorSelectionPayload payload) {
                    buffer.writeLong(payload.portalPosition().asLong());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
