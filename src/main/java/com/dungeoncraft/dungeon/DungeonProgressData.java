package com.dungeoncraft.dungeon;

import com.dungeoncraft.DungeonCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persistent room bounds and entry state for each player's active dungeon run. */
public final class DungeonProgressData extends SavedData {
    private static final Codec<RoomState> ROOM_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("room_id").forGetter(RoomState::roomId),
            Codec.INT.fieldOf("min_x").forGetter(RoomState::minX),
            Codec.INT.fieldOf("max_x").forGetter(RoomState::maxX),
            Codec.INT.fieldOf("min_z").forGetter(RoomState::minZ),
            Codec.INT.fieldOf("max_z").forGetter(RoomState::maxZ),
            Codec.STRING.fieldOf("role").forGetter(RoomState::role),
            Codec.BOOL.optionalFieldOf("entered", false).forGetter(RoomState::entered)
    ).apply(instance, RoomState::new));

    private static final Codec<RunState> RUN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(run -> run.playerId().toString()),
            Codec.LONG.fieldOf("dungeon_id").forGetter(RunState::dungeonId),
            ROOM_CODEC.listOf().fieldOf("rooms").forGetter(RunState::rooms)
    ).apply(instance, (player, dungeonId, rooms) ->
            new RunState(UUID.fromString(player), dungeonId, new ArrayList<>(rooms))));

    private static final Codec<DungeonProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RUN_CODEC.listOf().optionalFieldOf("runs", List.of()).forGetter(DungeonProgressData::savedRuns)
    ).apply(instance, DungeonProgressData::new));

    private static final SavedDataType<DungeonProgressData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "dungeon_progress"),
            DungeonProgressData::new,
            CODEC);

    private final Map<UUID, RunState> runs = new HashMap<>();

    public DungeonProgressData() {
        this(List.of());
    }

    private DungeonProgressData(List<RunState> savedRuns) {
        savedRuns.forEach(run -> runs.put(run.playerId(), run));
    }

    public static DungeonProgressData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void startRun(ServerPlayer player, PrototypeDungeonGenerator.GeneratedDungeon dungeon) {
        List<RoomState> rooms = dungeon.rooms().stream()
                .map(room -> new RoomState(
                        room.roomId(), room.minX(), room.maxX(), room.minZ(), room.maxZ(), room.role(), false))
                .toList();
        runs.put(player.getUUID(), new RunState(player.getUUID(), dungeon.dungeonId(), new ArrayList<>(rooms)));
        setDirty();
    }

    public Optional<EnteredRoom> enterRoom(UUID playerId, BlockPos position) {
        RunState run = runs.get(playerId);
        if (run == null) {
            return Optional.empty();
        }

        for (int index = 0; index < run.rooms().size(); index++) {
            RoomState room = run.rooms().get(index);
            if (!room.entered() && room.contains(position)) {
                run.rooms().set(index, room.withEntered());
                setDirty();
                return Optional.of(new EnteredRoom(
                        run.dungeonId(), room.roomId(), room.role(), room.centerX(), room.centerZ()));
            }
        }
        return Optional.empty();
    }

    public void endRun(UUID playerId) {
        if (runs.remove(playerId) != null) {
            setDirty();
        }
    }

    private List<RunState> savedRuns() {
        return List.copyOf(runs.values());
    }

    private record RunState(UUID playerId, long dungeonId, List<RoomState> rooms) {
    }

    private record RoomState(
            int roomId, int minX, int maxX, int minZ, int maxZ, String role, boolean entered) {
        private boolean contains(BlockPos position) {
            return position.getX() >= minX && position.getX() <= maxX
                    && position.getZ() >= minZ && position.getZ() <= maxZ;
        }

        private RoomState withEntered() {
            return new RoomState(roomId, minX, maxX, minZ, maxZ, role, true);
        }

        private int centerX() {
            return minX + (maxX - minX) / 2;
        }

        private int centerZ() {
            return minZ + (maxZ - minZ) / 2;
        }
    }

    public record EnteredRoom(long dungeonId, int roomId, String role, int centerX, int centerZ) {
        public boolean isCombat() {
            return "COMBAT".equals(role);
        }
    }
}
