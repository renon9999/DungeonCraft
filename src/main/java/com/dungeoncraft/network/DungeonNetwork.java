package com.dungeoncraft.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DungeonNetwork {
    private static final String NETWORK_VERSION = "2";

    private DungeonNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToClient(
                FloorChoiceHudPayload.TYPE, FloorChoiceHudPayload.STREAM_CODEC)
                .playToClient(ActiveModifiersHudPayload.TYPE, ActiveModifiersHudPayload.STREAM_CODEC);
    }
}
