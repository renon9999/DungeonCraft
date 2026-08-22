package com.dungeoncraft.client;

import com.dungeoncraft.network.SelectFloorCountPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Dedicated DC Portal screen for selecting the length of a new dungeon run. */
public final class DungeonFloorSelectionScreen extends Screen {
    private static final int[] FLOOR_COUNTS = {6, 10, 20, 40};
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 190;
    private static final int BUTTON_WIDTH = 210;
    private final BlockPos portalPosition;
    private boolean selectionSent;

    public DungeonFloorSelectionScreen(BlockPos portalPosition) {
        super(Component.translatable("screen.dungeoncraft.floor_selection.title"));
        this.portalPosition = portalPosition.immutable();
    }

    @Override
    protected void init() {
        int buttonX = (width - BUTTON_WIDTH) / 2;
        int firstButtonY = height / 2 - 45;
        for (int index = 0; index < FLOOR_COUNTS.length; index++) {
            int floorCount = FLOOR_COUNTS[index];
            addRenderableWidget(Button.builder(
                    Component.translatable(
                            "screen.dungeoncraft.floor_selection.option." + floorCount),
                    button -> select(floorCount))
                    .bounds(buttonX, firstButtonY + index * 25, BUTTON_WIDTH, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"), button -> onClose())
                .bounds((width - 100) / 2, firstButtonY + 108, 100, 20)
                .build());
    }

    private void select(int floorCount) {
        if (selectionSent) {
            return;
        }
        selectionSent = true;
        ClientPacketDistributor.sendToServer(
                new SelectFloorCountPayload(portalPosition, floorCount));
        minecraft.gui.setScreen(null);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE6110C18);
        graphics.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF8D6BB5);
        graphics.centeredText(font, title, width / 2, panelY + 16, 0xFFEAD9FF);
        graphics.centeredText(font,
                Component.translatable("screen.dungeoncraft.floor_selection.subtitle"),
                width / 2, panelY + 32, 0xFFB7A9C9);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
