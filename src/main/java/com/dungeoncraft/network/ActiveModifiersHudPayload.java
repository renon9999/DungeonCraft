package com.dungeoncraft.network;

import com.dungeoncraft.DungeonCraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-bound snapshot of all modifiers active in the current Dungeon run. */
public record ActiveModifiersHudPayload(boolean visible, List<ModifierEntry> modifiers)
        implements CustomPacketPayload {
    private static final int MAX_MODIFIERS = 32;

    public static final Type<ActiveModifiersHudPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "active_modifiers_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveModifiersHudPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ActiveModifiersHudPayload decode(RegistryFriendlyByteBuf buffer) {
                    boolean visible = buffer.readBoolean();
                    int count = Math.min(buffer.readVarInt(), MAX_MODIFIERS);
                    List<ModifierEntry> modifiers = new ArrayList<>(count);
                    for (int index = 0; index < count; index++) {
                        modifiers.add(new ModifierEntry(
                                buffer.readUtf(64), buffer.readVarInt(), buffer.readBoolean()));
                    }
                    return new ActiveModifiersHudPayload(visible, modifiers);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ActiveModifiersHudPayload payload) {
                    buffer.writeBoolean(payload.visible());
                    buffer.writeVarInt(payload.modifiers().size());
                    for (ModifierEntry modifier : payload.modifiers()) {
                        buffer.writeUtf(modifier.id(), 64);
                        buffer.writeVarInt(modifier.tier());
                        buffer.writeBoolean(modifier.positive());
                    }
                }
            };

    public ActiveModifiersHudPayload {
        modifiers = List.copyOf(modifiers);
        if (modifiers.size() > MAX_MODIFIERS) {
            throw new IllegalArgumentException("Too many active Dungeon modifiers");
        }
    }

    public static ActiveModifiersHudPayload hidden() {
        return new ActiveModifiersHudPayload(false, List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ModifierEntry(String id, int tier, boolean positive) {
    }
}
