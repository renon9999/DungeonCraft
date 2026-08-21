package com.dungeoncraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DungeonGenerationConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_ROOMS = BUILDER
            .comment("Minimum number of rooms in a generated prototype dungeon.")
            .defineInRange("generation.minRooms", 6, 5, 20);
    public static final ModConfigSpec.IntValue MAX_ROOMS = BUILDER
            .comment("Maximum number of rooms in a generated prototype dungeon.")
            .defineInRange("generation.maxRooms", 10, 5, 20);
    public static final ModConfigSpec.IntValue MIN_ROOM_SIZE = BUILDER
            .comment("Minimum room width/depth. Even values are adjusted to an odd value.")
            .defineInRange("generation.minRoomSize", 17, 7, 35);
    public static final ModConfigSpec.IntValue MAX_ROOM_SIZE = BUILDER
            .comment("Maximum room width/depth. Even values are adjusted to an odd value.")
            .defineInRange("generation.maxRoomSize", 29, 7, 35);
    public static final ModConfigSpec.IntValue MIN_ROOM_HEIGHT = BUILDER
            .comment("Minimum room interior height.")
            .defineInRange("generation.minRoomHeight", 6, 5, 10);
    public static final ModConfigSpec.IntValue MAX_ROOM_HEIGHT = BUILDER
            .comment("Maximum room interior height.")
            .defineInRange("generation.maxRoomHeight", 8, 5, 10);
    public static final ModConfigSpec.IntValue CELL_SPACING = BUILDER
            .comment("Distance between logical room centers. Larger values create longer corridors.")
            .defineInRange("generation.cellSpacing", 38, 20, 56);
    public static final ModConfigSpec.IntValue CORRIDOR_WIDTH = BUILDER
            .comment("Corridor interior width. Even values are adjusted to an odd value.")
            .defineInRange("generation.corridorWidth", 3, 3, 5);
    public static final ModConfigSpec.DoubleValue BRANCH_CHANCE = BUILDER
            .comment("Chance to grow from a random frontier room instead of the newest room.")
            .defineInRange("generation.branchChance", 0.4, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue COMBAT_ROOM_CHANCE = BUILDER
            .comment("Chance for an ordinary room to be marked as a future combat room.")
            .defineInRange("generation.combatRoomChance", 0.55, 0.0, 1.0);
    public static final ModConfigSpec.IntValue FIRST_WAVE_ZOMBIE_COUNT = BUILDER
            .comment("Number of zombies spawned by the prototype combat-room wave.")
            .defineInRange("wave.firstWaveZombieCount", 4, 1, 20);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private DungeonGenerationConfig() {
    }
}
