package com.dungeoncraft.dungeon;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.config.DungeonGenerationConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

/** Generates a connected, branching room graph with varied room shapes and decoration patterns. */
public final class PrototypeDungeonGenerator {
    public static final int FLOOR_Y = 65;
    public static final int PLAYER_Y = FLOOR_Y + 1;

    private static final int REGION_SPACING = 2048;
    private static final int REGIONS_PER_ROW = 10_000;
    private static final int FLOOR_SLOTS_PER_DUNGEON = 16;
    private static final int CORRIDOR_INTERIOR_HEIGHT = 4;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final List<BlockPos> LOOT_CHEST_OFFSETS = List.of(
            new BlockPos(3, 0, 0), new BlockPos(-3, 0, 0),
            new BlockPos(0, 0, 3), new BlockPos(0, 0, -3),
            new BlockPos(3, 0, 3), new BlockPos(-3, 0, 3),
            new BlockPos(3, 0, -3), new BlockPos(-3, 0, -3),
            new BlockPos(6, 0, 0), new BlockPos(-6, 0, 0),
            new BlockPos(0, 0, 6));
    private static final List<BlockPos> EXIT_REWARD_CHEST_OFFSETS = List.of(
            new BlockPos(0, 0, -4),
            new BlockPos(-3, 0, -4), new BlockPos(3, 0, -4),
            new BlockPos(-6, 0, -4), new BlockPos(6, 0, -4));

    private PrototypeDungeonGenerator() {
    }

    public static GeneratedDungeon generate(ServerLevel level, long dungeonId) {
        GenerationParameters parameters = GenerationParameters.fromConfig();
        GeneratedFloor firstFloor = generateFloor(
                level, dungeonId, 1, 0, -1, parameters.floorCount() == 1,
                parameters, List.of());
        DungeonCraft.LOGGER.info("Started Dungeon #{} with Floor 1 of {} generated",
                dungeonId, parameters.floorCount());
        return new GeneratedDungeon(
                dungeonId, parameters.floorCount(), firstFloor.spawn(), firstFloor.returnSwitch(),
                firstFloor.floorExits(),
                firstFloor.rooms());
    }

    public static GeneratedFloor generateNextFloor(
            ServerLevel level, long dungeonId, int floorNumber, int firstRoomId,
            int selectedChoice, boolean finalFloor,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers) {
        return generateFloor(
                level, dungeonId, floorNumber, firstRoomId, selectedChoice, finalFloor,
                GenerationParameters.fromConfig(), activeModifiers);
    }

    private static GeneratedFloor generateFloor(
            ServerLevel level, long dungeonId, int floorNumber, int firstRoomId,
            int selectedChoice, boolean finalFloor, GenerationParameters parameters,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers) {
        int floorIndex = floorNumber - 1;
        Random random = new Random(
                level.getSeed()
                        ^ dungeonId * 0x9E3779B97F4A7C15L
                        ^ floorNumber * 0xC2B2AE3D27D4EB4FL
                        ^ (long)(selectedChoice + 1) * 0x165667B19E3779F9L);
        BlockPos origin = originFor(dungeonId, floorIndex);
        int targetRooms = between(random, parameters.minRooms(), parameters.maxRooms());

        List<Room> rooms = createRoomGraph(random, origin, targetRooms, parameters);
        assignRoles(random, rooms, parameters.combatRoomChance(), parameters.lootRoomEnabled(),
                floorNumber % 3 == 0,
                DungeonModifierEffects.maximumEnemyStrength(activeModifiers),
                DungeonModifierEffects.tier(activeModifiers, "enemy_strength"));

        for (Room room : rooms) {
            createRoom(level, room);
        }
        for (Room room : rooms) {
            if (room.parent() != null) {
                createCorridor(level, room.parent(), room, parameters.corridorWidth());
            }
        }
        for (Room child : rooms) {
            if (child.parent() != null) {
                createExitSeal(level, child.parent(), child, parameters.corridorWidth());
            }
        }
        for (Room room : rooms) {
            decorateRoom(level, room);
            placeRoleMarker(level, room);
            if (room.role == RoomRole.REST) {
                placeRestFacilities(level, room);
            }
            if (room.role == RoomRole.EXIT) {
                List<DungeonFloorModifier.AppliedModifier> exitRewardModifiers =
                        DungeonModifierEffects.boostedLootModifiers(activeModifiers, 1);
                int exitChestCount = 1 + Math.min(
                        EXIT_REWARD_CHEST_OFFSETS.size() - 1,
                        DungeonModifierEffects.extraChestCount(exitRewardModifiers));
                for (int chestIndex = 0; chestIndex < exitChestCount; chestIndex++) {
                    BlockPos offset = EXIT_REWARD_CHEST_OFFSETS.get(chestIndex);
                    DungeonLootGenerator.placeAndFillChest(
                            level, new BlockPos(
                                    room.centerX() + offset.getX(), PLAYER_Y,
                                    room.centerZ() + offset.getZ()),
                            random, exitRewardModifiers);
                }
            }
            int chestCount = lootChestCount(random, room, activeModifiers);
            for (int chestIndex = 0; chestIndex < chestCount; chestIndex++) {
                BlockPos offset = LOOT_CHEST_OFFSETS.get(chestIndex);
                DungeonLootGenerator.placeAndFillChest(
                        level, new BlockPos(
                                room.centerX() + offset.getX(), PLAYER_Y,
                                room.centerZ() + offset.getZ()),
                        random, activeModifiers);
            }
        }

        Room start = rooms.getFirst();
        Room exit = rooms.stream().filter(room -> room.role == RoomRole.EXIT).findFirst().orElseThrow();
        BlockPos spawn = new BlockPos(start.centerX(), PLAYER_Y, start.centerZ());
        List<GeneratedFloorExit> floorExits = placeFloorExitSwitches(
                level, exit, dungeonId, floorNumber, finalFloor);
        DungeonCraft.LOGGER.info("Generated Dungeon #{} Floor {} with {} rooms at {}, {}",
                dungeonId, floorIndex + 1, rooms.size(), origin.getX(), origin.getZ());
        List<GeneratedRoom> generatedRooms = new ArrayList<>();
        for (int localRoomId = 0; localRoomId < rooms.size(); localRoomId++) {
            Room room = rooms.get(localRoomId);
            generatedRooms.add(new GeneratedRoom(
                    firstRoomId + localRoomId,
                    room.centerX() - room.width() / 2 + 1,
                    room.centerX() + room.width() / 2 - 1,
                    room.centerZ() - room.roomDepth() / 2 + 1,
                    room.centerZ() + room.roomDepth() / 2 - 1,
                    room.role.name(),
                    room.combatEncounter() == null ? "" : room.combatEncounter().id(),
                    room.entranceDirection().getName(),
                    List.copyOf(room.exitSealBlocks)));
        }
        return new GeneratedFloor(
                floorNumber, spawn, spawn, floorExits, List.copyOf(generatedRooms));
    }

    private static List<Room> createRoomGraph(
            Random random, BlockPos origin, int targetRooms, GenerationParameters parameters) {
        List<Room> rooms = new ArrayList<>();
        Map<GridPos, Room> occupied = new HashMap<>();
        List<Room> frontier = new ArrayList<>();

        Room start = randomRoom(random, new GridPos(0, 0), origin, null, 0, parameters);
        rooms.add(start);
        occupied.put(start.grid(), start);
        frontier.add(start);

        while (rooms.size() < targetRooms && !frontier.isEmpty()) {
            Room parent = random.nextDouble() < parameters.branchChance()
                    ? frontier.get(random.nextInt(frontier.size()))
                    : frontier.getLast();
            List<Direction> available = availableDirections(parent.grid(), occupied);
            if (available.isEmpty()) {
                frontier.remove(parent);
                continue;
            }

            Direction direction = available.get(random.nextInt(available.size()));
            GridPos childGrid = parent.grid().move(direction);
            Room child = randomRoom(random, childGrid, origin, parent, parent.depth() + 1, parameters);
            rooms.add(child);
            occupied.put(childGrid, child);
            frontier.add(child);

            if (availableDirections(parent.grid(), occupied).isEmpty()) {
                frontier.remove(parent);
            }
        }
        return rooms;
    }

    private static Room randomRoom(
            Random random, GridPos grid, BlockPos origin, Room parent, int depth,
            GenerationParameters parameters) {
        int width = randomOdd(random, parameters.minRoomSize(), parameters.maxRoomSize());
        int depthSize = randomOdd(random, parameters.minRoomSize(), parameters.maxRoomSize());
        int height = between(random, parameters.minRoomHeight(), parameters.maxRoomHeight());
        RoomPattern[] patterns = RoomPattern.values();
        RoomPattern pattern = parent == null ? RoomPattern.OPEN : patterns[random.nextInt(patterns.length)];
        int centerX = origin.getX() + grid.x() * parameters.cellSpacing();
        int centerZ = origin.getZ() + grid.z() * parameters.cellSpacing();
        return new Room(grid, centerX, centerZ, width, depthSize, height, pattern, parent, depth);
    }

    private static List<Direction> availableDirections(GridPos position, Map<GridPos, Room> occupied) {
        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
        directions.removeIf(direction -> occupied.containsKey(position.move(direction)));
        return directions;
    }

    private static void assignRoles(
            Random random, List<Room> rooms, double combatChance, boolean lootRoomEnabled,
            boolean restRoomRequired, int maximumEnemyStrength, int enemyStrengthTier) {
        Room start = rooms.getFirst();
        start.role = RoomRole.START;
        Room exit = rooms.stream().max((left, right) -> Integer.compare(left.depth(), right.depth())).orElseThrow();
        exit.role = RoomRole.EXIT;

        List<Room> candidates = new ArrayList<>(rooms);
        candidates.remove(start);
        candidates.remove(exit);
        if (restRoomRequired && !candidates.isEmpty()) {
            Room rest = candidates.remove(random.nextInt(candidates.size()));
            rest.role = RoomRole.REST;
        }
        if (lootRoomEnabled && !candidates.isEmpty()) {
            Room loot = candidates.remove(random.nextInt(candidates.size()));
            loot.role = RoomRole.LOOT;
        }
        for (Room room : candidates) {
            room.role = random.nextDouble() < combatChance ? RoomRole.COMBAT : RoomRole.NORMAL;
            if (room.role == RoomRole.COMBAT) {
                room.applyEncounter(DungeonCombatEncounter.choose(
                        random, maximumEnemyStrength, enemyStrengthTier));
            }
        }
    }

    private static void createRoom(ServerLevel level, Room room) {
        int halfWidth = room.width() / 2;
        int halfDepth = room.roomDepth() / 2;
        int ceilingY = FLOOR_Y + room.height();
        for (int x = room.centerX() - halfWidth; x <= room.centerX() + halfWidth; x++) {
            for (int z = room.centerZ() - halfDepth; z <= room.centerZ() + halfDepth; z++) {
                set(level, x, FLOOR_Y - 1, z, Blocks.BEDROCK.defaultBlockState());
                set(level, x, FLOOR_Y, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                boolean wall = x == room.centerX() - halfWidth || x == room.centerX() + halfWidth
                        || z == room.centerZ() - halfDepth || z == room.centerZ() + halfDepth;
                for (int y = PLAYER_Y; y < ceilingY; y++) {
                    set(level, x, y, z, wall ? Blocks.BEDROCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                set(level, x, ceilingY, z, Blocks.BEDROCK.defaultBlockState());
            }
        }
        addCeilingLights(level, room, ceilingY);
    }

    private static void addCeilingLights(ServerLevel level, Room room, int ceilingY) {
        int usableHalfWidth = Math.max(0, room.width() / 2 - 3);
        int usableHalfDepth = Math.max(0, room.roomDepth() / 2 - 3);
        for (int xOffset : centeredLightOffsets(usableHalfWidth)) {
            for (int zOffset : centeredLightOffsets(usableHalfDepth)) {
                set(level, room.centerX() + xOffset, ceilingY, room.centerZ() + zOffset,
                        Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
    }

    private static List<Integer> centeredLightOffsets(int usableHalfSize) {
        int rings = Math.max(0, usableHalfSize / 6);
        List<Integer> offsets = new ArrayList<>();
        for (int index = -rings; index <= rings; index++) {
            offsets.add(index * 6);
        }
        return offsets;
    }

    private static void createCorridor(ServerLevel level, Room first, Room second, int corridorWidth) {
        int halfWidth = corridorWidth / 2;
        if (first.grid().x() != second.grid().x()) {
            Room west = first.centerX() < second.centerX() ? first : second;
            Room east = west == first ? second : first;
            int startX = west.centerX() + west.width() / 2 + 1;
            int endX = east.centerX() - east.width() / 2 - 1;
            createEastWestCorridor(level, startX, endX, west.centerZ(), halfWidth);
            openEastWestDoor(level, west.centerX() + west.width() / 2, west.centerZ(), halfWidth);
            openEastWestDoor(level, east.centerX() - east.width() / 2, east.centerZ(), halfWidth);
        } else {
            Room north = first.centerZ() < second.centerZ() ? first : second;
            Room south = north == first ? second : first;
            int startZ = north.centerZ() + north.roomDepth() / 2 + 1;
            int endZ = south.centerZ() - south.roomDepth() / 2 - 1;
            createNorthSouthCorridor(level, startZ, endZ, north.centerX(), halfWidth);
            openNorthSouthDoor(level, north.centerZ() + north.roomDepth() / 2, north.centerX(), halfWidth);
            openNorthSouthDoor(level, south.centerZ() - south.roomDepth() / 2, south.centerX(), halfWidth);
        }
    }

    private static void createEastWestCorridor(ServerLevel level, int startX, int endX, int centerZ, int halfWidth) {
        for (int x = startX; x <= endX; x++) {
            for (int z = centerZ - halfWidth - 1; z <= centerZ + halfWidth + 1; z++) {
                boolean wall = Math.abs(z - centerZ) == halfWidth + 1;
                buildCorridorColumn(level, x, z, wall, x % 6 == 0 && z == centerZ);
            }
        }
    }

    private static void createNorthSouthCorridor(ServerLevel level, int startZ, int endZ, int centerX, int halfWidth) {
        for (int z = startZ; z <= endZ; z++) {
            for (int x = centerX - halfWidth - 1; x <= centerX + halfWidth + 1; x++) {
                boolean wall = Math.abs(x - centerX) == halfWidth + 1;
                buildCorridorColumn(level, x, z, wall, z % 6 == 0 && x == centerX);
            }
        }
    }

    private static void buildCorridorColumn(ServerLevel level, int x, int z, boolean wall, boolean light) {
        int ceilingY = FLOOR_Y + CORRIDOR_INTERIOR_HEIGHT + 1;
        set(level, x, FLOOR_Y - 1, z, Blocks.BEDROCK.defaultBlockState());
        set(level, x, FLOOR_Y, z, Blocks.DEEPSLATE_TILES.defaultBlockState());
        for (int y = PLAYER_Y; y < ceilingY; y++) {
            set(level, x, y, z, wall ? Blocks.BEDROCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        set(level, x, ceilingY, z,
                light ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BEDROCK.defaultBlockState());
    }

    private static void openEastWestDoor(ServerLevel level, int x, int centerZ, int halfWidth) {
        for (int z = centerZ - halfWidth; z <= centerZ + halfWidth; z++) {
            openDoorColumn(level, x, z);
        }
    }

    private static void openNorthSouthDoor(ServerLevel level, int z, int centerX, int halfWidth) {
        for (int x = centerX - halfWidth; x <= centerX + halfWidth; x++) {
            openDoorColumn(level, x, z);
        }
    }

    private static void openDoorColumn(ServerLevel level, int x, int z) {
        for (int y = PLAYER_Y; y <= FLOOR_Y + CORRIDOR_INTERIOR_HEIGHT; y++) {
            set(level, x, y, z, Blocks.AIR.defaultBlockState());
        }
    }

    private static void createExitSeal(ServerLevel level, Room parent, Room child, int corridorWidth) {
        int halfWidth = corridorWidth / 2;
        List<BlockPos> sealBlocks = parent.exitSealBlocks;
        if (parent.centerX() != child.centerX()) {
            int doorX = parent.centerX()
                    + (child.centerX() > parent.centerX() ? parent.width() / 2 : -parent.width() / 2);
            for (int z = parent.centerZ() - halfWidth; z <= parent.centerZ() + halfWidth; z++) {
                addSealColumn(sealBlocks, doorX, z);
            }
        } else {
            int doorZ = parent.centerZ()
                    + (child.centerZ() > parent.centerZ() ? parent.roomDepth() / 2 : -parent.roomDepth() / 2);
            for (int x = parent.centerX() - halfWidth; x <= parent.centerX() + halfWidth; x++) {
                addSealColumn(sealBlocks, x, doorZ);
            }
        }
        DungeonRoomSealer.close(level, sealBlocks);
    }

    private static void addSealColumn(List<BlockPos> sealBlocks, int x, int z) {
        for (int y = PLAYER_Y; y <= FLOOR_Y + CORRIDOR_INTERIOR_HEIGHT; y++) {
            sealBlocks.add(new BlockPos(x, y, z));
        }
    }

    private static void decorateRoom(ServerLevel level, Room room) {
        if (room.combatEncounter() != null) {
            room.combatEncounter().decorate(
                    level, room.centerX(), room.centerZ(), room.entranceDirection());
            return;
        }
        switch (room.pattern()) {
            case OPEN -> { }
            case PILLARS -> addPillars(level, room);
            case CENTRAL_DAIS -> addCentralDais(level, room);
            case CROSS_FLOOR -> addCrossFloor(level, room);
            case WORN_FLOOR -> addWornFloor(level, room);
        }
    }

    private static void addPillars(ServerLevel level, Room room) {
        int offsetX = Math.max(2, room.width() / 2 - 2);
        int offsetZ = Math.max(2, room.roomDepth() / 2 - 2);
        for (int x : List.of(room.centerX() - offsetX, room.centerX() + offsetX)) {
            for (int z : List.of(room.centerZ() - offsetZ, room.centerZ() + offsetZ)) {
                for (int y = PLAYER_Y; y < FLOOR_Y + room.height(); y++) {
                    set(level, x, y, z, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                }
            }
        }
    }

    private static void addCentralDais(ServerLevel level, Room room) {
        for (int x = room.centerX() - 1; x <= room.centerX() + 1; x++) {
            for (int z = room.centerZ() - 1; z <= room.centerZ() + 1; z++) {
                set(level, x, FLOOR_Y, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
        }
    }

    private static void addCrossFloor(ServerLevel level, Room room) {
        for (int x = room.centerX() - room.width() / 2 + 1; x < room.centerX() + room.width() / 2; x++) {
            set(level, x, FLOOR_Y, room.centerZ(), Blocks.POLISHED_BASALT.defaultBlockState());
        }
        for (int z = room.centerZ() - room.roomDepth() / 2 + 1; z < room.centerZ() + room.roomDepth() / 2; z++) {
            set(level, room.centerX(), FLOOR_Y, z, Blocks.POLISHED_BASALT.defaultBlockState());
        }
    }

    private static void addWornFloor(ServerLevel level, Room room) {
        for (int x = room.centerX() - room.width() / 2 + 1; x < room.centerX() + room.width() / 2; x++) {
            for (int z = room.centerZ() - room.roomDepth() / 2 + 1; z < room.centerZ() + room.roomDepth() / 2; z++) {
                if (Math.floorMod(x + z, 4) == 0) {
                    set(level, x, FLOOR_Y, z, Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState());
                }
            }
        }
    }

    private static void placeRoleMarker(ServerLevel level, Room room) {
        BlockState marker = switch (room.role) {
            case START -> Blocks.EMERALD_BLOCK.defaultBlockState();
            case NORMAL -> Blocks.STONE_BRICKS.defaultBlockState();
            case COMBAT -> Blocks.REDSTONE_BLOCK.defaultBlockState();
            case LOOT -> Blocks.GOLD_BLOCK.defaultBlockState();
            case REST -> Blocks.LAPIS_BLOCK.defaultBlockState();
            case EXIT -> Blocks.LODESTONE.defaultBlockState();
        };
        set(level, room.centerX(), FLOOR_Y, room.centerZ(), marker);
    }

    private static int lootChestCount(
            Random random, Room room,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers) {
        if (room.role == RoomRole.LOOT) {
            return 1 + Math.min(
                    LOOT_CHEST_OFFSETS.size() - 1,
                    DungeonModifierEffects.extraChestCount(activeModifiers));
        }
        if (room.role == RoomRole.NORMAL
                && random.nextDouble() < DungeonGenerationConfig.EXTRA_CHEST_CHANCE.get()) {
            return 1;
        }
        return 0;
    }

    private static void placeRestFacilities(ServerLevel level, Room room) {
        BlockPos table = new BlockPos(room.centerX(), PLAYER_Y, room.centerZ());
        set(level, table.getX(), table.getY(), table.getZ(), Blocks.ENCHANTING_TABLE.defaultBlockState());
        set(level, room.centerX() - 4, PLAYER_Y, room.centerZ(), Blocks.ANVIL.defaultBlockState());
        DungeonLootGenerator.placeEnchantingSupplyChest(
                level, new BlockPos(room.centerX() + 4, PLAYER_Y, room.centerZ()));

        int placedBookshelves = 0;
        for (int xOffset = -2; xOffset <= 2 && placedBookshelves < 15; xOffset++) {
            for (int zOffset = -2; zOffset <= 2 && placedBookshelves < 15; zOffset++) {
                if (Math.abs(xOffset) != 2 && Math.abs(zOffset) != 2) {
                    continue;
                }
                if (xOffset == 0 && zOffset == -2) {
                    continue;
                }
                set(level, room.centerX() + xOffset, PLAYER_Y, room.centerZ() + zOffset,
                        Blocks.BOOKSHELF.defaultBlockState());
                placedBookshelves++;
            }
        }
    }

    private static List<GeneratedFloorExit> placeFloorExitSwitches(
            ServerLevel level, Room exit, long dungeonId, int floorNumber, boolean finalFloor) {
        if (finalFloor) {
            BlockPos switchPos = placeExitLever(
                    level, exit.centerX(), exit.centerZ() + 2, Blocks.IRON_BLOCK.defaultBlockState());
            return List.of(new GeneratedFloorExit(floorNumber, 0, 0, List.of(), switchPos));
        }

        List<DungeonFloorModifier.FloorOption> options =
                DungeonFloorModifier.generateOptions(level.getSeed(), dungeonId, floorNumber + 1);
        List<BlockState> bases = List.of(
                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                Blocks.GOLD_BLOCK.defaultBlockState(),
                Blocks.DIAMOND_BLOCK.defaultBlockState());
        List<GeneratedFloorExit> choices = new ArrayList<>();
        for (int choiceIndex = 0; choiceIndex < 3; choiceIndex++) {
            int x = exit.centerX() + (choiceIndex - 1) * 4;
            BlockPos switchPos = placeExitLever(level, x, exit.centerZ() + 2, bases.get(choiceIndex));
            DungeonFloorModifier.FloorOption option = options.get(choiceIndex);
            choices.add(new GeneratedFloorExit(
                    floorNumber, choiceIndex, option.score(), option.modifiers(), switchPos));
        }
        return List.copyOf(choices);
    }

    private static BlockPos placeExitLever(
            ServerLevel level, int x, int z, BlockState baseBlock) {
        BlockPos switchPos = new BlockPos(x, PLAYER_Y, z);
        set(level, x, FLOOR_Y, z, baseBlock);
        BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LeverBlock.POWERED, false);
        set(level, x, switchPos.getY(), z, lever);
        return switchPos;
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, UPDATE_FLAGS);
    }

    private static BlockPos originFor(long dungeonId, int floorIndex) {
        long dungeonIndex = Math.max(0L, dungeonId - 1L);
        long index = dungeonIndex * FLOOR_SLOTS_PER_DUNGEON + floorIndex;
        int column = (int)Math.floorMod(index, REGIONS_PER_ROW);
        int row = (int)Math.floorMod(index / REGIONS_PER_ROW, REGIONS_PER_ROW);
        return new BlockPos(column * REGION_SPACING, FLOOR_Y, row * REGION_SPACING);
    }

    private static int randomOdd(Random random, int minimum, int maximum) {
        int value = between(random, minimum, maximum);
        if (value % 2 == 0) {
            value += value < maximum ? 1 : -1;
        }
        return value;
    }

    private static int between(Random random, int minimum, int maximum) {
        int lower = Math.min(minimum, maximum);
        int upper = Math.max(minimum, maximum);
        return lower + random.nextInt(upper - lower + 1);
    }

    public record GeneratedDungeon(
            long dungeonId, int floorCount, BlockPos spawn, BlockPos returnSwitch,
            List<GeneratedFloorExit> floorExits,
            List<GeneratedRoom> rooms) {
        public int roomCount() {
            return rooms.size();
        }

    }

    public record GeneratedFloorExit(
            int floorNumber, int choiceIndex, int modifierScore,
            List<DungeonFloorModifier.AppliedModifier> modifiers,
            BlockPos switchPosition) {
    }

    public record GeneratedFloor(
            int floorNumber, BlockPos spawn, BlockPos returnSwitch,
            List<GeneratedFloorExit> floorExits,
            List<GeneratedRoom> rooms) {
    }

    public record GeneratedRoom(
            int roomId, int minX, int maxX, int minZ, int maxZ, String role,
            String encounterId, String entranceDirection,
            List<BlockPos> exitSealBlocks) {
    }

    private record GenerationParameters(
            int minRooms, int maxRooms, int floorCount, int minRoomSize, int maxRoomSize,
            int minRoomHeight, int maxRoomHeight, int cellSpacing,
            int corridorWidth, double branchChance, double combatRoomChance, boolean lootRoomEnabled) {
        private static GenerationParameters fromConfig() {
            int cellSpacing = DungeonGenerationConfig.CELL_SPACING.getAsInt();
            int maxRoomSize = Math.min(DungeonGenerationConfig.MAX_ROOM_SIZE.getAsInt(), cellSpacing - 5);
            return new GenerationParameters(
                    DungeonGenerationConfig.MIN_ROOMS.getAsInt(),
                    DungeonGenerationConfig.MAX_ROOMS.getAsInt(),
                    DungeonGenerationConfig.FLOOR_COUNT.getAsInt(),
                    Math.min(DungeonGenerationConfig.MIN_ROOM_SIZE.getAsInt(), maxRoomSize),
                    maxRoomSize,
                    DungeonGenerationConfig.MIN_ROOM_HEIGHT.getAsInt(),
                    DungeonGenerationConfig.MAX_ROOM_HEIGHT.getAsInt(),
                    cellSpacing,
                    odd(DungeonGenerationConfig.CORRIDOR_WIDTH.getAsInt()),
                    DungeonGenerationConfig.BRANCH_CHANCE.get(),
                    DungeonGenerationConfig.COMBAT_ROOM_CHANCE.get(),
                    DungeonGenerationConfig.LOOT_ROOM_ENABLED.get());
        }

        private static int odd(int value) {
            return value % 2 == 0 ? value - 1 : value;
        }
    }

    private static final class Room {
        private final GridPos grid;
        private final int centerX;
        private final int centerZ;
        private int width;
        private int roomDepth;
        private int height;
        private final RoomPattern pattern;
        private final Room parent;
        private final int depth;
        private final List<BlockPos> exitSealBlocks = new ArrayList<>();
        private RoomRole role = RoomRole.NORMAL;
        private DungeonCombatEncounter combatEncounter;

        private Room(GridPos grid, int centerX, int centerZ, int width, int roomDepth,
                     int height, RoomPattern pattern, Room parent, int depth) {
            this.grid = grid;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.width = width;
            this.roomDepth = roomDepth;
            this.height = height;
            this.pattern = pattern;
            this.parent = parent;
            this.depth = depth;
        }

        private GridPos grid() { return grid; }
        private int centerX() { return centerX; }
        private int centerZ() { return centerZ; }
        private int width() { return width; }
        private int roomDepth() { return roomDepth; }
        private int height() { return height; }
        private RoomPattern pattern() { return pattern; }
        private Room parent() { return parent; }
        private int depth() { return depth; }
        private DungeonCombatEncounter combatEncounter() { return combatEncounter; }

        private void applyEncounter(DungeonCombatEncounter encounter) {
            combatEncounter = encounter;
            width = encounter.width();
            roomDepth = encounter.depth();
            height = encounter.height();
        }

        private net.minecraft.core.Direction entranceDirection() {
            if (parent == null) {
                return net.minecraft.core.Direction.NORTH;
            }
            if (parent.centerX < centerX) {
                return net.minecraft.core.Direction.WEST;
            }
            if (parent.centerX > centerX) {
                return net.minecraft.core.Direction.EAST;
            }
            return parent.centerZ < centerZ
                    ? net.minecraft.core.Direction.NORTH
                    : net.minecraft.core.Direction.SOUTH;
        }
    }

    private record GridPos(int x, int z) {
        private GridPos move(Direction direction) {
            return new GridPos(x + direction.dx, z + direction.dz);
        }
    }

    private enum Direction {
        NORTH(0, -1), SOUTH(0, 1), WEST(-1, 0), EAST(1, 0);

        private final int dx;
        private final int dz;

        Direction(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }
    }

    private enum RoomPattern {
        OPEN, PILLARS, CENTRAL_DAIS, CROSS_FLOOR, WORN_FLOOR
    }

    private enum RoomRole {
        START, NORMAL, COMBAT, LOOT, REST, EXIT
    }
}
