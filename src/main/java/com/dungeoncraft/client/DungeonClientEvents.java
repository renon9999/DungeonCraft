package com.dungeoncraft.client;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.network.ActiveModifiersHudPayload;
import com.dungeoncraft.network.FloorChoiceHudPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = DungeonCraft.MOD_ID, value = Dist.CLIENT)
public final class DungeonClientEvents {
    private DungeonClientEvents() {
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(FloorChoiceHudPayload.TYPE,
                (payload, context) -> DungeonChoiceHud.accept(payload));
        event.register(ActiveModifiersHudPayload.TYPE,
                (payload, context) -> DungeonActiveModifiersHud.accept(payload));
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "floor_choice_hud"),
                DungeonChoiceHud::render);
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "active_modifiers_hud"),
                DungeonActiveModifiersHud::render);
    }
}
