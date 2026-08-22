package com.dungeoncraft.dungeon;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.config.DungeonGenerationConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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
            Codec.INT.optionalFieldOf("floor_number", 0).forGetter(RoomState::floorNumber),
            Codec.INT.fieldOf("min_x").forGetter(RoomState::minX),
            Codec.INT.fieldOf("max_x").forGetter(RoomState::maxX),
            Codec.INT.fieldOf("min_z").forGetter(RoomState::minZ),
            Codec.INT.fieldOf("max_z").forGetter(RoomState::maxZ),
            Codec.STRING.fieldOf("role").forGetter(RoomState::role),
            Codec.STRING.optionalFieldOf("encounter", "").forGetter(RoomState::encounterId),
            Codec.STRING.optionalFieldOf("entrance_direction", "north").forGetter(RoomState::entranceDirection),
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
            Codec.INT.optionalFieldOf("choice_index", 0).forGetter(FloorExitState::choiceIndex),
            Codec.INT.optionalFieldOf("modifier_score", 0).forGetter(FloorExitState::modifierScore),
            DungeonFloorModifier.APPLIED_CODEC.listOf().optionalFieldOf("modifiers", List.of())
                    .forGetter(FloorExitState::modifiers),
            SEAL_POSITION_CODEC.fieldOf("switch_position").forGetter(FloorExitState::switchPosition)
    ).apply(instance, FloorExitState::new));

    private static final Codec<RunState> RUN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(run -> run.leaderId().toString()),
            Codec.LONG.fieldOf("dungeon_id").forGetter(RunState::dungeonId),
            Codec.STRING.listOf().optionalFieldOf("members", List.of()).forGetter(run ->
                    run.memberIds().stream().map(UUID::toString).toList()),
            Codec.INT.optionalFieldOf("current_floor", 1).forGetter(RunState::currentFloor),
            Codec.INT.optionalFieldOf("floor_count", 1).forGetter(RunState::floorCount),
            DungeonFloorModifier.APPLIED_CODEC.listOf().optionalFieldOf("current_floor_modifiers", List.of())
                    .forGetter(RunState::currentFloorModifiers),
            DungeonFloorModifier.APPLIED_CODEC.listOf().optionalFieldOf("persistent_modifiers", List.of())
                    .forGetter(RunState::persistentModifiers),
            ROOM_CODEC.listOf().fieldOf("rooms").forGetter(RunState::rooms),
            ROOM_COMBAT_CODEC.listOf().optionalFieldOf("active_room_combats", List.of())
                    .forGetter(RunState::activeRoomCombats),
            FLOOR_EXIT_CODEC.listOf().optionalFieldOf("floor_exits", List.of()).forGetter(RunState::floorExits)
    ).apply(instance, (player, dungeonId, members, currentFloor, floorCount,
                       currentFloorModifiers, persistentModifiers,
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
                    new ArrayList<>(currentFloorModifiers),
                    new ArrayList<>(persistentModifiers),
                    rooms.stream()
                            .map(room -> room.floorNumber() > 0
                                    ? room
                                    : room.withFloorNumber(currentFloor))
                            .collect(java.util.stream.Collectors.toCollection(ArrayList::new)),
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
        startRun(player, List.of(player), dungeon);
    }

    public void startRun(
            ServerPlayer leader, List<ServerPlayer> party,
            PrototypeDungeonGenerator.GeneratedDungeon dungeon) {
        List<UUID> memberIds = party.stream()
                .map(ServerPlayer::getUUID)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (!memberIds.contains(leader.getUUID())) {
            memberIds.addFirst(leader.getUUID());
        }
        memberIds.forEach(this::endRun);
        List<RoomState> rooms = createRoomStates(1, dungeon.rooms());
        List<FloorExitState> floorExits = dungeon.floorExits().stream()
                .map(exit -> new FloorExitState(
                        exit.floorNumber(), exit.choiceIndex(), exit.modifierScore(), exit.modifiers(),
                        SealPosition.fromBlockPos(exit.switchPosition())))
                .toList();
        registerRun(new RunState(
                leader.getUUID(), dungeon.dungeonId(), memberIds,
                1, dungeon.floorCount(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(rooms), new ArrayList<>(),
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
                        run.dungeonId(), exit.floorNumber(), run.floorCount(),
                        exit.choiceIndex(), exit.modifierScore(),
                        List.copyOf(exit.modifiers()), nextRoomId(run)));
    }

    public List<FloorChoiceInfo> currentFloorChoices(UUID playerId) {
        RunState run = runFor(playerId);
        if (run == null) {
            return List.of();
        }
        return run.floorExits().stream()
                .filter(exit -> exit.floorNumber() == run.currentFloor())
                .filter(exit -> !exit.modifiers().isEmpty())
                .map(exit -> new FloorChoiceInfo(
                        exit.choiceIndex(), exit.modifierScore(), List.copyOf(exit.modifiers())))
                .toList();
    }

    public Optional<NearbyFloorChoice> nearestFloorChoice(
            UUID playerId, BlockPos playerPosition, double maximumDistanceSquared) {
        RunState run = runFor(playerId);
        if (run == null) {
            return Optional.empty();
        }
        return run.floorExits().stream()
                .filter(exit -> exit.floorNumber() == run.currentFloor())
                .filter(exit -> !exit.modifiers().isEmpty())
                .map(exit -> new NearbyFloorChoice(
                        exit.floorNumber(), exit.choiceIndex(), exit.modifierScore(),
                        List.copyOf(exit.modifiers()),
                        distanceSquared(playerPosition, exit.switchPosition().toBlockPos())))
                .filter(choice -> choice.distanceSquared() <= maximumDistanceSquared)
                .min(java.util.Comparator.comparingDouble(NearbyFloorChoice::distanceSquared));
    }

    private static double distanceSquared(BlockPos left, BlockPos right) {
        long x = (long)left.getX() - right.getX();
        long y = (long)left.getY() - right.getY();
        long z = (long)left.getZ() - right.getZ();
        return x * x + y * y + z * z;
    }

    public List<DungeonFloorModifier.AppliedModifier> activeModifiers(UUID playerId) {
        RunState run = runFor(playerId);
        if (run == null) {
            return List.of();
        }
        Map<String, DungeonFloorModifier.AppliedModifier> accumulatedById = new HashMap<>();
        run.currentFloorModifiers().forEach(modifier -> mergeAccumulatedTier(accumulatedById, modifier));
        run.persistentModifiers().forEach(modifier -> mergeAccumulatedTier(accumulatedById, modifier));
        return accumulatedById.values().stream()
                .sorted(java.util.Comparator.comparing(DungeonFloorModifier.AppliedModifier::positive).reversed()
                        .thenComparing(DungeonFloorModifier.AppliedModifier::id))
                .toList();
    }

    public Optional<FloorProgress> floorProgress(UUID playerId) {
        RunState run = runFor(playerId);
        return run == null
                ? Optional.empty()
                : Optional.of(new FloorProgress(run.currentFloor(), run.floorCount()));
    }

    public List<UUID> partyMemberIds(UUID playerId) {
        RunState run = runFor(playerId);
        return run == null ? List.of() : List.copyOf(run.memberIds());
    }

    public boolean hasActiveRun(UUID playerId) {
        return runFor(playerId) != null;
    }

    public OptionalLong activeDungeonId(UUID playerId) {
        Long dungeonId = playerRuns.get(playerId);
        return dungeonId == null ? OptionalLong.empty() : OptionalLong.of(dungeonId);
    }

    /** Removes only the departing member; the shared run survives while at least one member remains. */
    public Optional<PartyDeparture> removePartyMember(UUID playerId) {
        Long dungeonId = playerRuns.remove(playerId);
        if (dungeonId == null) {
            return Optional.empty();
        }
        RunState run = runs.get(dungeonId);
        if (run == null) {
            setDirty();
            return Optional.of(new PartyDeparture(dungeonId, List.of(), true));
        }
        run.memberIds().remove(playerId);
        boolean abandoned = run.memberIds().isEmpty();
        List<UUID> remaining = List.copyOf(run.memberIds());
        if (abandoned) {
            runs.remove(dungeonId);
        }
        setDirty();
        return Optional.of(new PartyDeparture(dungeonId, remaining, abandoned));
    }

    public List<DungeonFloorModifier.AppliedModifier> modifiersAfterSelection(
            UUID playerId, List<DungeonFloorModifier.AppliedModifier> selectedModifiers) {
        List<DungeonFloorModifier.AppliedModifier> combined = new ArrayList<>(activeModifiers(playerId));
        selectedModifiers.forEach(modifier -> addModifierTier(combined, modifier));
        return List.copyOf(combined);
    }

    public boolean advanceToFloor(
            UUID playerId, int expectedCurrentFloor,
            PrototypeDungeonGenerator.GeneratedFloor floor,
            List<DungeonFloorModifier.AppliedModifier> selectedModifiers) {
        RunState run = runFor(playerId);
        if (run == null
                || run.currentFloor() != expectedCurrentFloor
                || floor.floorNumber() != expectedCurrentFloor + 1
                || floor.floorNumber() > run.floorCount()) {
            return false;
        }
        run.currentFloorModifiers().clear();
        for (DungeonFloorModifier.AppliedModifier modifier : selectedModifiers) {
            addModifierTier(run.persistentModifiers(), modifier);
        }
        run.rooms().removeIf(room -> room.floorNumber() <= expectedCurrentFloor);
        run.floorExits().removeIf(exit -> exit.floorNumber() <= expectedCurrentFloor);
        run.rooms().addAll(createRoomStates(floor.floorNumber(), floor.rooms()));
        floor.floorExits().stream()
                .map(exit -> new FloorExitState(
                        exit.floorNumber(), exit.choiceIndex(), exit.modifierScore(), exit.modifiers(),
                        SealPosition.fromBlockPos(exit.switchPosition())))
                .forEach(run.floorExits()::add);
        run.currentFloor = floor.floorNumber();
        run.activeRoomCombats().clear();
        setDirty();
        return true;
    }

    private static void addModifierTier(
            List<DungeonFloorModifier.AppliedModifier> modifiers,
            DungeonFloorModifier.AppliedModifier selected) {
        for (int index = 0; index < modifiers.size(); index++) {
            DungeonFloorModifier.AppliedModifier current = modifiers.get(index);
            if (!current.id().equals(selected.id())) {
                continue;
            }
            int accumulatedTier = Math.min(
                    DungeonGenerationConfig.MAX_ACCUMULATED_MODIFIER_TIER.getAsInt(),
                    current.tier() + selected.tier());
            modifiers.set(index, new DungeonFloorModifier.AppliedModifier(
                    current.id(), accumulatedTier, current.positive(), true));
            return;
        }
        modifiers.add(new DungeonFloorModifier.AppliedModifier(
                selected.id(), Math.min(
                        DungeonGenerationConfig.MAX_ACCUMULATED_MODIFIER_TIER.getAsInt(),
                        selected.tier()),
                selected.positive(), true));
    }

    private static void mergeAccumulatedTier(
            Map<String, DungeonFloorModifier.AppliedModifier> modifiers,
            DungeonFloorModifier.AppliedModifier candidate) {
        DungeonFloorModifier.AppliedModifier normalized = new DungeonFloorModifier.AppliedModifier(
                candidate.id(), Math.min(
                        DungeonGenerationConfig.MAX_ACCUMULATED_MODIFIER_TIER.getAsInt(),
                        candidate.tier()),
                candidate.positive(), candidate.persistent());
        modifiers.merge(candidate.id(), normalized,
                (current, addition) -> new DungeonFloorModifier.AppliedModifier(
                        current.id(), Math.min(
                                DungeonGenerationConfig.MAX_ACCUMULATED_MODIFIER_TIER.getAsInt(),
                                current.tier() + addition.tier()),
                        current.positive(), true));
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
            if (room.floorNumber() == run.currentFloor()
                    && !room.entered() && room.contains(position)) {
                RoomState enteredRoom = room.withEntered();
                if (!enteredRoom.isCombat()) {
                    enteredRoom = enteredRoom.withCleared();
                }
                run.rooms().set(index, enteredRoom);
                setDirty();
                return Optional.of(new EnteredRoom(
                        run.dungeonId(), room.roomId(), room.role(),
                        room.encounterId(), room.entranceDirection(),
                        room.centerX(), room.centerZ(),
                        room.minX(), room.maxX(), room.minZ(), room.maxZ(),
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
            int floorNumber, List<PrototypeDungeonGenerator.GeneratedRoom> generatedRooms) {
        return generatedRooms.stream()
                .map(room -> new RoomState(
                        room.roomId(), floorNumber,
                        room.minX(), room.maxX(), room.minZ(), room.maxZ(), room.role(),
                        room.encounterId(), room.entranceDirection(),
                        false, false,
                        room.exitSealBlocks().stream().map(SealPosition::fromBlockPos).toList()))
                .toList();
    }

    private static int nextRoomId(RunState run) {
        return run.rooms().stream().mapToInt(RoomState::roomId).max().orElse(-1) + 1;
    }

    public List<UUID> endRun(UUID playerId) {
        Long dungeonId = playerRuns.get(playerId);
        if (dungeonId == null) {
            return List.of();
        }
        RunState removed = runs.remove(dungeonId);
        if (removed != null) {
            removed.memberIds().forEach(playerRuns::remove);
            setDirty();
            return List.copyOf(removed.memberIds());
        }
        playerRuns.remove(playerId);
        return List.of();
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
        private final List<DungeonFloorModifier.AppliedModifier> currentFloorModifiers;
        private final List<DungeonFloorModifier.AppliedModifier> persistentModifiers;
        private final List<RoomState> rooms;
        private final List<RoomCombatState> activeRoomCombats;
        private final List<FloorExitState> floorExits;

        private RunState(
                UUID leaderId, long dungeonId, List<UUID> memberIds,
                int currentFloor, int floorCount,
                List<DungeonFloorModifier.AppliedModifier> currentFloorModifiers,
                List<DungeonFloorModifier.AppliedModifier> persistentModifiers,
                List<RoomState> rooms,
                List<RoomCombatState> activeRoomCombats, List<FloorExitState> floorExits) {
            this.leaderId = leaderId;
            this.dungeonId = dungeonId;
            this.memberIds = memberIds;
            this.currentFloor = currentFloor;
            this.floorCount = floorCount;
            this.currentFloorModifiers = currentFloorModifiers;
            this.persistentModifiers = persistentModifiers;
            this.rooms = rooms;
            this.activeRoomCombats = activeRoomCombats;
            this.floorExits = floorExits;
        }

        private UUID leaderId() { return leaderId; }
        private long dungeonId() { return dungeonId; }
        private List<UUID> memberIds() { return memberIds; }
        private int currentFloor() { return currentFloor; }
        private int floorCount() { return floorCount; }
        private List<DungeonFloorModifier.AppliedModifier> currentFloorModifiers() {
            return currentFloorModifiers;
        }
        private List<DungeonFloorModifier.AppliedModifier> persistentModifiers() {
            return persistentModifiers;
        }
        private List<RoomState> rooms() { return rooms; }
        private List<RoomCombatState> activeRoomCombats() { return activeRoomCombats; }
        private List<FloorExitState> floorExits() { return floorExits; }
    }

    private record RoomCombatState(int roomId, List<UUID> enemyIds) {
    }

    private record FloorExitState(
            int floorNumber, int choiceIndex, int modifierScore,
            List<DungeonFloorModifier.AppliedModifier> modifiers,
            SealPosition switchPosition) {
    }

    private record RoomState(
            int roomId, int floorNumber,
            int minX, int maxX, int minZ, int maxZ, String role,
            String encounterId, String entranceDirection,
            boolean entered, boolean cleared, List<SealPosition> exitSeals) {
        private boolean contains(BlockPos position) {
            return position.getX() >= minX && position.getX() <= maxX
                    && position.getZ() >= minZ && position.getZ() <= maxZ;
        }

        private RoomState withEntered() {
            return new RoomState(
                    roomId, floorNumber, minX, maxX, minZ, maxZ, role, encounterId, entranceDirection,
                    true, cleared, exitSeals);
        }

        private RoomState withFloorNumber(int loadedFloorNumber) {
            return new RoomState(
                    roomId, loadedFloorNumber,
                    minX, maxX, minZ, maxZ, role, encounterId, entranceDirection,
                    entered, cleared, exitSeals);
        }

        private RoomState withCleared() {
            return new RoomState(
                    roomId, floorNumber, minX, maxX, minZ, maxZ, role, encounterId, entranceDirection,
                    entered, true, exitSeals);
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
            long dungeonId, int roomId, String role,
            String encounterId, String entranceDirection,
            int centerX, int centerZ,
            int minX, int maxX, int minZ, int maxZ,
            List<BlockPos> exitSealBlocks) {
        public boolean isCombat() {
            return "COMBAT".equals(role);
        }
    }

    public record RoomCleared(
            List<UUID> memberIds, long dungeonId, int roomId, List<BlockPos> exitSealBlocks) {
    }

    public record FloorExitAction(
            long dungeonId, int floorNumber, int floorCount, int choiceIndex, int modifierScore,
            List<DungeonFloorModifier.AppliedModifier> modifiers, int nextRoomId) {
        public boolean isFinalFloor() {
            return floorNumber >= floorCount;
        }
    }

    public record FloorChoiceInfo(
            int choiceIndex, int modifierScore,
            List<DungeonFloorModifier.AppliedModifier> modifiers) {
    }

    public record FloorProgress(int currentFloor, int floorCount) {
    }

    public record PartyDeparture(
            long dungeonId, List<UUID> remainingMemberIds, boolean abandoned) {
    }

    public record NearbyFloorChoice(
            int floorNumber, int choiceIndex, int modifierScore,
            List<DungeonFloorModifier.AppliedModifier> modifiers,
            double distanceSquared) {
    }

}
