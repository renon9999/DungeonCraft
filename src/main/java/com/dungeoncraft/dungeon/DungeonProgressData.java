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

/** Persistent shared state for active dungeon runs and their party members. */
public final class DungeonProgressData extends SavedData {
    private static final Codec<SealPosition> SEAL_POSITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(SealPosition::x),
            Codec.INT.fieldOf("y").forGetter(SealPosition::y),
            Codec.INT.fieldOf("z").forGetter(SealPosition::z)
    ).apply(instance, SealPosition::new));

    private static final Codec<RoomState> ROOM_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("room_id").forGetter(RoomState::roomId),
            Codec.INT.fieldOf("min_x").forGetter(RoomState::minX),
            Codec.INT.fieldOf("max_x").forGetter(RoomState::maxX),
            Codec.INT.fieldOf("min_z").forGetter(RoomState::minZ),
            Codec.INT.fieldOf("max_z").forGetter(RoomState::maxZ),
            Codec.STRING.fieldOf("role").forGetter(RoomState::role),
            Codec.BOOL.optionalFieldOf("entered", false).forGetter(RoomState::entered),
            Codec.BOOL.optionalFieldOf("cleared", false).forGetter(RoomState::cleared),
            SEAL_POSITION_CODEC.listOf().optionalFieldOf("exit_seals", List.of()).forGetter(RoomState::exitSeals)
    ).apply(instance, RoomState::new));

    private static final Codec<RoomCombatState> ROOM_COMBAT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("room_id").forGetter(RoomCombatState::roomId),
            Codec.STRING.listOf().fieldOf("enemies").forGetter(combat ->
                    combat.enemyIds().stream().map(UUID::toString).toList())
    ).apply(instance, (roomId, enemies) -> new RoomCombatState(
            roomId,
            enemies.stream().map(UUID::fromString)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new)))));

    private static final Codec<FloorExitState> FLOOR_EXIT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("floor_number").forGetter(FloorExitState::floorNumber),
            SEAL_POSITION_CODEC.fieldOf("switch_position").forGetter(FloorExitState::switchPosition)
    ).apply(instance, FloorExitState::new));

    private static final Codec<RunState> RUN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(run -> run.leaderId().toString()),
            Codec.LONG.fieldOf("dungeon_id").forGetter(RunState::dungeonId),
            Codec.STRING.listOf().optionalFieldOf("members", List.of()).forGetter(run ->
                    run.memberIds().stream().map(UUID::toString).toList()),
            Codec.INT.optionalFieldOf("current_floor", 1).forGetter(RunState::currentFloor),
            Codec.INT.optionalFieldOf("floor_count", 1).forGetter(RunState::floorCount),
            ROOM_CODEC.listOf().fieldOf("rooms").forGetter(RunState::rooms),
            ROOM_COMBAT_CODEC.listOf().optionalFieldOf("active_room_combats", List.of())
                    .forGetter(RunState::activeRoomCombats),
            FLOOR_EXIT_CODEC.listOf().optionalFieldOf("floor_exits", List.of()).forGetter(RunState::floorExits)
    ).apply(instance, (player, dungeonId, members, currentFloor, floorCount,
                       rooms, activeRoomCombats, floorExits) -> {
        UUID leaderId = UUID.fromString(player);
        List<UUID> memberIds = members.isEmpty()
                ? new ArrayList<>(List.of(leaderId))
                : members.stream().map(UUID::fromString)
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return new RunState(
                    leaderId,
                    dungeonId,
                    memberIds,
                    currentFloor,
                    floorCount,
                    new ArrayList<>(rooms),
                    new ArrayList<>(activeRoomCombats),
                    new ArrayList<>(floorExits));
    }));

    private static final Codec<DungeonProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RUN_CODEC.listOf().optionalFieldOf("runs", List.of()).forGetter(DungeonProgressData::savedRuns)
    ).apply(instance, DungeonProgressData::new));

    private static final SavedDataType<DungeonProgressData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "dungeon_progress"),
            DungeonProgressData::new,
            CODEC);

    private final Map<Long, RunState> runs = new HashMap<>();
    private final Map<UUID, Long> playerRuns = new HashMap<>();

    public DungeonProgressData() {
        this(List.of());
    }

    private DungeonProgressData(List<RunState> savedRuns) {
        savedRuns.forEach(this::registerRun);
    }

    public static DungeonProgressData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void startRun(ServerPlayer player, PrototypeDungeonGenerator.GeneratedDungeon dungeon) {
        endRun(player.getUUID());
        List<RoomState> rooms = createRoomStates(dungeon.rooms());
        List<FloorExitState> floorExits = List.of(new FloorExitState(
                dungeon.floorExit().floorNumber(),
                SealPosition.fromBlockPos(dungeon.floorExit().switchPosition())));
        registerRun(new RunState(
                player.getUUID(), dungeon.dungeonId(), new ArrayList<>(List.of(player.getUUID())),
                1, dungeon.floorCount(), new ArrayList<>(rooms), new ArrayList<>(),
                new ArrayList<>(floorExits)));
        setDirty();
    }

    public Optional<FloorExitAction> findFloorExit(UUID playerId, BlockPos position) {
        RunState run = runFor(playerId);
        if (run == null) {
            return Optional.empty();
        }
        return run.floorExits().stream()
                .filter(exit -> exit.floorNumber() == run.currentFloor())
                .filter(exit -> exit.switchPosition().toBlockPos().equals(position))
                .findFirst()
                .map(exit -> new FloorExitAction(
                        run.dungeonId(), exit.floorNumber(), run.floorCount(), nextRoomId(run)));
    }

    public boolean advanceToFloor(
            UUID playerId, int expectedCurrentFloor,
            PrototypeDungeonGenerator.GeneratedFloor floor) {
        RunState run = runFor(playerId);
        if (run == null
                || run.currentFloor() != expectedCurrentFloor
                || floor.floorNumber() != expectedCurrentFloor + 1
                || floor.floorNumber() > run.floorCount()) {
            return false;
        }
        run.rooms().addAll(createRoomStates(floor.rooms()));
        run.floorExits().add(new FloorExitState(
                floor.floorNumber(), SealPosition.fromBlockPos(floor.exitSwitch())));
        run.currentFloor = floor.floorNumber();
        setDirty();
        return true;
    }

    public void startRoomCombat(UUID playerId, int roomId, List<UUID> enemyIds) {
        RunState run = runFor(playerId);
        if (run == null) {
            return;
        }
        run.activeRoomCombats().removeIf(combat -> combat.roomId() == roomId);
        run.activeRoomCombats().add(new RoomCombatState(roomId, new ArrayList<>(enemyIds)));
        setDirty();
    }

    public Optional<RoomCleared> removeRoomEnemy(UUID enemyId) {
        for (RunState run : runs.values()) {
            for (int index = 0; index < run.activeRoomCombats().size(); index++) {
                RoomCombatState combat = run.activeRoomCombats().get(index);
                if (!combat.enemyIds().remove(enemyId)) {
                    continue;
                }
                setDirty();
                if (combat.enemyIds().isEmpty()) {
                    run.activeRoomCombats().remove(index);
                    List<BlockPos> exitSeals = markRoomCleared(run, combat.roomId());
                    return Optional.of(new RoomCleared(
                            List.copyOf(run.memberIds()), run.dungeonId(), combat.roomId(), exitSeals));
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<EnteredRoom> enterRoom(UUID playerId, BlockPos position) {
        RunState run = runFor(playerId);
        if (run == null) {
            return Optional.empty();
        }

        for (int index = 0; index < run.rooms().size(); index++) {
            RoomState room = run.rooms().get(index);
            if (!room.entered() && room.contains(position)) {
                RoomState enteredRoom = room.withEntered();
                if (!enteredRoom.isCombat()) {
                    enteredRoom = enteredRoom.withCleared();
                }
                run.rooms().set(index, enteredRoom);
                setDirty();
                return Optional.of(new EnteredRoom(
                        run.dungeonId(), room.roomId(), room.role(), room.centerX(), room.centerZ(),
                        room.exitSealBlocks()));
            }
        }
        return Optional.empty();
    }

    public List<BlockPos> clearRoom(UUID playerId, int roomId) {
        RunState run = runFor(playerId);
        if (run == null) {
            return List.of();
        }
        List<BlockPos> exitSeals = markRoomCleared(run, roomId);
        setDirty();
        return exitSeals;
    }

    private static List<BlockPos> markRoomCleared(RunState run, int roomId) {
        for (int index = 0; index < run.rooms().size(); index++) {
            RoomState room = run.rooms().get(index);
            if (room.roomId() == roomId) {
                run.rooms().set(index, room.withCleared());
                return room.exitSealBlocks();
            }
        }
        return List.of();
    }

    private static List<RoomState> createRoomStates(
            List<PrototypeDungeonGenerator.GeneratedRoom> generatedRooms) {
        return generatedRooms.stream()
                .map(room -> new RoomState(
                        room.roomId(), room.minX(), room.maxX(), room.minZ(), room.maxZ(), room.role(),
                        false, false,
                        room.exitSealBlocks().stream().map(SealPosition::fromBlockPos).toList()))
                .toList();
    }

    private static int nextRoomId(RunState run) {
        return run.rooms().stream().mapToInt(RoomState::roomId).max().orElse(-1) + 1;
    }

    public void endRun(UUID playerId) {
        Long dungeonId = playerRuns.get(playerId);
        if (dungeonId == null) {
            return;
        }
        RunState removed = runs.remove(dungeonId);
        if (removed != null) {
            removed.memberIds().forEach(playerRuns::remove);
            setDirty();
        }
    }

    private RunState runFor(UUID playerId) {
        Long dungeonId = playerRuns.get(playerId);
        return dungeonId == null ? null : runs.get(dungeonId);
    }

    private void registerRun(RunState run) {
        runs.put(run.dungeonId(), run);
        run.memberIds().forEach(memberId -> playerRuns.put(memberId, run.dungeonId()));
    }

    private List<RunState> savedRuns() {
        return List.copyOf(runs.values());
    }

    private static final class RunState {
        private final UUID leaderId;
        private final long dungeonId;
        private final List<UUID> memberIds;
        private int currentFloor;
        private final int floorCount;
        private final List<RoomState> rooms;
        private final List<RoomCombatState> activeRoomCombats;
        private final List<FloorExitState> floorExits;

        private RunState(
                UUID leaderId, long dungeonId, List<UUID> memberIds,
                int currentFloor, int floorCount, List<RoomState> rooms,
                List<RoomCombatState> activeRoomCombats, List<FloorExitState> floorExits) {
            this.leaderId = leaderId;
            this.dungeonId = dungeonId;
            this.memberIds = memberIds;
            this.currentFloor = currentFloor;
            this.floorCount = floorCount;
            this.rooms = rooms;
            this.activeRoomCombats = activeRoomCombats;
            this.floorExits = floorExits;
        }

        private UUID leaderId() { return leaderId; }
        private long dungeonId() { return dungeonId; }
        private List<UUID> memberIds() { return memberIds; }
        private int currentFloor() { return currentFloor; }
        private int floorCount() { return floorCount; }
        private List<RoomState> rooms() { return rooms; }
        private List<RoomCombatState> activeRoomCombats() { return activeRoomCombats; }
        private List<FloorExitState> floorExits() { return floorExits; }
    }

    private record RoomCombatState(int roomId, List<UUID> enemyIds) {
    }

    private record FloorExitState(int floorNumber, SealPosition switchPosition) {
    }

    private record RoomState(
            int roomId, int minX, int maxX, int minZ, int maxZ, String role,
            boolean entered, boolean cleared, List<SealPosition> exitSeals) {
        private boolean contains(BlockPos position) {
            return position.getX() >= minX && position.getX() <= maxX
                    && position.getZ() >= minZ && position.getZ() <= maxZ;
        }

        private RoomState withEntered() {
            return new RoomState(roomId, minX, maxX, minZ, maxZ, role, true, cleared, exitSeals);
        }

        private RoomState withCleared() {
            return new RoomState(roomId, minX, maxX, minZ, maxZ, role, entered, true, exitSeals);
        }

        private boolean isCombat() {
            return "COMBAT".equals(role);
        }

        private List<BlockPos> exitSealBlocks() {
            return exitSeals.stream().map(SealPosition::toBlockPos).toList();
        }

        private int centerX() {
            return minX + (maxX - minX) / 2;
        }

        private int centerZ() {
            return minZ + (maxZ - minZ) / 2;
        }
    }

    private record SealPosition(int x, int y, int z) {
        private static SealPosition fromBlockPos(BlockPos position) {
            return new SealPosition(position.getX(), position.getY(), position.getZ());
        }

        private BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    public record EnteredRoom(
            long dungeonId, int roomId, String role, int centerX, int centerZ,
            List<BlockPos> exitSealBlocks) {
        public boolean isCombat() {
            return "COMBAT".equals(role);
        }
    }

    public record RoomCleared(
            List<UUID> memberIds, long dungeonId, int roomId, List<BlockPos> exitSealBlocks) {
    }

    public record FloorExitAction(
            long dungeonId, int floorNumber, int floorCount, int nextRoomId) {
        public boolean isFinalFloor() {
            return floorNumber >= floorCount;
        }
    }

}
