package com.dungeoncraft.client;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.network.ActiveModifiersHudPayload;
import com.dungeoncraft.network.FloorChoiceHudPayload;
import com.dungeoncraft.network.OpenFloorSelectionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DungeonCraft.MOD_ID, value = Dist.CLIENT)
public final class DungeonClientEvents {
    private static final KeyMapping TOGGLE_MODIFIER_HUD = new KeyMapping(
            "key.dungeoncraft.toggle_modifiers",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KeyMapping.Category.MISC);

    private DungeonClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MODIFIER_HUD);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (TOGGLE_MODIFIER_HUD.consumeClick()) {
            DungeonActiveModifiersHud.toggleVisibility();
        }
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(FloorChoiceHudPayload.TYPE,
                (payload, context) -> DungeonChoiceHud.accept(payload));
        event.register(ActiveModifiersHudPayload.TYPE,
                (payload, context) -> DungeonActiveModifiersHud.accept(payload));
        event.register(OpenFloorSelectionPayload.TYPE,
                (payload, context) -> Minecraft.getInstance().gui.setScreen(
                        new DungeonFloorSelectionScreen(payload.portalPosition())));
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
