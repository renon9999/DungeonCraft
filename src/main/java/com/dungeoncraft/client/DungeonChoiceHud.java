package com.dungeoncraft.client;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.network.FloorChoiceHudPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Client state and renderer for the nearby Floor Choice details. */
public final class DungeonChoiceHud {
    private static final int PANEL_WIDTH = 190;
    private static final int PADDING = 7;
    private static final int LINE_HEIGHT = 10;
    private static FloorChoiceHudPayload current = FloorChoiceHudPayload.hidden();

    private DungeonChoiceHud() {
    }

    public static void accept(FloorChoiceHudPayload payload) {
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
        ChoiceTheme theme = ChoiceTheme.forChoice(current.choiceIndex());
        int panelHeight = PADDING * 2 + lines.size() * LINE_HEIGHT;
        int x = Math.max(4, graphics.guiWidth() - PANEL_WIDTH - 8);
        int y = Math.max(4, (graphics.guiHeight() - panelHeight) / 2);
        graphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, theme.background());
        graphics.outline(x, y, PANEL_WIDTH, panelHeight, theme.border());

        int textY = y + PADDING;
        for (HudLine line : lines) {
            graphics.text(font, line.text(), x + PADDING, textY, line.color(), true);
            textY += LINE_HEIGHT;
        }
    }

    private static List<HudLine> createLines(FloorChoiceHudPayload payload) {
        List<HudLine> lines = new ArrayList<>();
        ChoiceTheme theme = ChoiceTheme.forChoice(payload.choiceIndex());
        lines.add(new HudLine(Component.translatable(
                "hud.dungeoncraft.choice.title", payload.choiceIndex() + 1, payload.score()), theme.title()));
        lines.add(new HudLine(Component.translatable("hud.dungeoncraft.choice.benefits"), 0xFF75E075));
        payload.modifiers().stream().filter(FloorChoiceHudPayload.ModifierEntry::positive)
                .map(modifier -> modifierLine(modifier, "+ "))
                .forEach(lines::add);
        lines.add(new HudLine(Component.translatable("hud.dungeoncraft.choice.drawbacks"), 0xFFFF7373));
        payload.modifiers().stream().filter(modifier -> !modifier.positive())
                .map(modifier -> modifierLine(modifier, "- "))
                .forEach(lines::add);
        return lines;
    }

    private static HudLine modifierLine(FloorChoiceHudPayload.ModifierEntry modifier, String prefix) {
        Component text = Component.literal(prefix)
                .append(Component.translatable("modifier.dungeoncraft." + modifier.id()))
                .append(Component.literal("  +T" + modifier.tier()));
        return new HudLine(text, modifier.positive() ? 0xFFB5F5B5 : 0xFFFFB0B0);
    }

    private record HudLine(Component text, int color) {
    }

    private record ChoiceTheme(int background, int border, int title) {
        private static ChoiceTheme forChoice(int choiceIndex) {
            return switch (choiceIndex) {
                case 0 -> new ChoiceTheme(0xCC2A0D12, 0xFFD84A4A, 0xFFFF7777);
                case 1 -> new ChoiceTheme(0xCC2B220B, 0xFFE6B93F, 0xFFFFD761);
                default -> new ChoiceTheme(0xCC09262A, 0xFF42D5D5, 0xFF72F0EE);
            };
        }
    }
}
