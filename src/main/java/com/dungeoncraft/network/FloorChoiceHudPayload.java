package com.dungeoncraft.network;

import com.dungeoncraft.DungeonCraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-bound data for the nearby Floor Choice HUD. */
public record FloorChoiceHudPayload(
        boolean visible, int choiceIndex, int score, List<ModifierEntry> modifiers)
        implements CustomPacketPayload {
    private static final int MAX_MODIFIERS = 16;

    public static final Type<FloorChoiceHudPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "floor_choice_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FloorChoiceHudPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public FloorChoiceHudPayload decode(RegistryFriendlyByteBuf buffer) {
                    boolean visible = buffer.readBoolean();
                    int choiceIndex = buffer.readVarInt();
                    int score = buffer.readVarInt();
                    int count = Math.min(buffer.readVarInt(), MAX_MODIFIERS);
                    List<ModifierEntry> modifiers = new ArrayList<>(count);
                    for (int index = 0; index < count; index++) {
                        modifiers.add(new ModifierEntry(
                                buffer.readUtf(64), buffer.readVarInt(), buffer.readBoolean()));
                    }
                    return new FloorChoiceHudPayload(visible, choiceIndex, score, modifiers);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, FloorChoiceHudPayload payload) {
                    buffer.writeBoolean(payload.visible());
                    buffer.writeVarInt(payload.choiceIndex());
                    buffer.writeVarInt(payload.score());
                    buffer.writeVarInt(payload.modifiers().size());
                    for (ModifierEntry modifier : payload.modifiers()) {
                        buffer.writeUtf(modifier.id(), 64);
                        buffer.writeVarInt(modifier.tier());
                        buffer.writeBoolean(modifier.positive());
                    }
                }
            };

    public FloorChoiceHudPayload {
        modifiers = List.copyOf(modifiers);
        if (modifiers.size() > MAX_MODIFIERS) {
            throw new IllegalArgumentException("Too many Floor Choice modifiers");
        }
    }

    public static FloorChoiceHudPayload hidden() {
        return new FloorChoiceHudPayload(false, 0, 0, List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ModifierEntry(String id, int tier, boolean positive) {
    }
}
