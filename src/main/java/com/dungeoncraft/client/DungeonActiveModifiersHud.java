package com.dungeoncraft.client;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.network.ActiveModifiersHudPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Small persistent top-left HUD for modifiers active during a Dungeon run. */
public final class DungeonActiveModifiersHud {
    private static final float SCALE = 0.8F;
    private static final int PANEL_WIDTH = 180;
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static ActiveModifiersHudPayload current = ActiveModifiersHudPayload.hidden();

    private DungeonActiveModifiersHud() {
    }

    public static void accept(ActiveModifiersHudPayload payload) {
        current = payload;
    }

    public static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!current.visible()
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.level.dimension() != DungeonCraft.DUNGEON_LEVEL) {
            return;
        }

        Font font = minecraft.font;
        List<HudLine> lines = createLines(current);
        int panelHeight = PADDING * 2 + lines.size() * LINE_HEIGHT;
        graphics.pose().pushMatrix();
        graphics.pose().scale(SCALE, SCALE);
        int x = 7;
        int y = 7;
        graphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xB8101018);
        graphics.outline(x, y, PANEL_WIDTH, panelHeight, 0xCC666B78);
        int textY = y + PADDING;
        for (HudLine line : lines) {
            graphics.text(font, line.text(), x + PADDING, textY, line.color(), true);
            textY += LINE_HEIGHT;
        }
        graphics.pose().popMatrix();
    }

    private static List<HudLine> createLines(ActiveModifiersHudPayload payload) {
        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine(Component.translatable(
                "hud.dungeoncraft.floor", payload.currentFloor(), payload.floorCount()), 0xFFFFFFFF));
        lines.add(new HudLine(Component.translatable("hud.dungeoncraft.active.title"), 0xFFF5D76E));
        if (payload.modifiers().isEmpty()) {
            lines.add(new HudLine(Component.translatable("hud.dungeoncraft.active.none"), 0xFFB0B0B0));
            return lines;
        }
        payload.modifiers().stream().filter(ActiveModifiersHudPayload.ModifierEntry::positive)
                .map(modifier -> modifierLine(modifier, "+ "))
                .forEach(lines::add);
        payload.modifiers().stream().filter(modifier -> !modifier.positive())
                .map(modifier -> modifierLine(modifier, "- "))
                .forEach(lines::add);
        return lines;
    }

    private static HudLine modifierLine(ActiveModifiersHudPayload.ModifierEntry modifier, String prefix) {
        Component text = Component.literal(prefix)
                .append(Component.translatable("modifier.dungeoncraft." + modifier.id()))
                .append(Component.literal(" T" + modifier.tier()));
        return new HudLine(text, modifier.positive() ? 0xFF9DE69D : 0xFFFF9B9B);
    }

    private record HudLine(Component text, int color) {
    }
}
