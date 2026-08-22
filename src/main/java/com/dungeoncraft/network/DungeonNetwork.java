package com.dungeoncraft.network;

import com.dungeoncraft.block.DCPortalBlock;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DungeonNetwork {
    private static final String NETWORK_VERSION = "3";

    private DungeonNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToClient(FloorChoiceHudPayload.TYPE, FloorChoiceHudPayload.STREAM_CODEC)
                .playToClient(ActiveModifiersHudPayload.TYPE, ActiveModifiersHudPayload.STREAM_CODEC)
                .playToClient(OpenFloorSelectionPayload.TYPE, OpenFloorSelectionPayload.STREAM_CODEC)
                .playToServer(
                        SelectFloorCountPayload.TYPE, SelectFloorCountPayload.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof ServerPlayer player) {
                                DCPortalBlock.startSelectedDungeon(
                                        player, payload.portalPosition(), payload.floorCount());
                            }
                        });
    }
}
