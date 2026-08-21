package com.dungeoncraft.dungeon;

import com.dungeoncraft.DungeonCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Stores only the allocation sequence; full run state is added in a later milestone. */
public final class DungeonSequenceData extends SavedData {
    private static final Codec<DungeonSequenceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("next_dungeon_id", 1L).forGetter(data -> data.nextDungeonId)
    ).apply(instance, DungeonSequenceData::new));

    private static final SavedDataType<DungeonSequenceData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "dungeon_sequence"),
            DungeonSequenceData::new,
            CODEC);

    private long nextDungeonId;

    public DungeonSequenceData() {
        this(1L);
    }

    private DungeonSequenceData(long nextDungeonId) {
        this.nextDungeonId = Math.max(1L, nextDungeonId);
    }

    public static DungeonSequenceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public long allocateDungeonId() {
        long allocated = nextDungeonId++;
        setDirty();
        return allocated;
    }
}
