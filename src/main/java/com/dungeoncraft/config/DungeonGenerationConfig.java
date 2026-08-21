package com.dungeoncraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DungeonGenerationConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_ROOMS = BUILDER
            .comment("Minimum number of rooms in a generated prototype dungeon.")
            .defineInRange("generation.minRooms", 6, 4, 20);
    public static final ModConfigSpec.IntValue MAX_ROOMS = BUILDER
            .comment("Maximum number of rooms in a generated prototype dungeon.")
            .defineInRange("generation.maxRooms", 8, 4, 20);
    public static final ModConfigSpec.IntValue FLOOR_COUNT = BUILDER
            .comment("Number of floors generated for one dungeon run.")
            .defineInRange("generation.floorCount", 3, 1, 16);
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
            .comment("Chance for an ordinary room to become a COMBAT room.")
            .defineInRange("generation.combatRoomChance", 1.0, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue LOOT_ROOM_ENABLED = BUILDER
            .comment("Whether one dedicated LOOT room is assigned during dungeon generation.")
            .define("generation.lootRoomEnabled", false);
    public static final ModConfigSpec.IntValue COMBAT_ENEMY_COUNT = BUILDER
            .comment("Number of zombies spawned once when entering a COMBAT room.")
            .defineInRange("combat.enemyCount", 1, 1, 20);
    public static final ModConfigSpec.IntValue MAX_PARTY_SIZE = BUILDER
            .comment("Maximum number of players allowed in one dungeon party.")
            .defineInRange("multiplayer.maxPartySize", 8, 1, 8);
    public static final ModConfigSpec.DoubleValue EXTRA_CHEST_CHANCE = BUILDER
            .comment("Chance for a NORMAL or COMBAT room to contain a chest. LOOT rooms always contain one.")
            .defineInRange("loot.extraChestChance", 0.25, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue FOOD_CHANCE = BUILDER
            .comment("Chance for a generated chest to contain food.")
            .defineInRange("loot.foodChance", 0.8, 0.0, 1.0);
    public static final ModConfigSpec.IntValue FOOD_MIN = BUILDER
            .comment("Minimum food item count.")
            .defineInRange("loot.foodMin", 2, 1, 64);
    public static final ModConfigSpec.IntValue FOOD_MAX = BUILDER
            .comment("Maximum food item count.")
            .defineInRange("loot.foodMax", 6, 1, 64);
    public static final ModConfigSpec.DoubleValue ARROW_CHANCE = BUILDER
            .comment("Chance for a generated chest to contain arrows.")
            .defineInRange("loot.arrowChance", 0.65, 0.0, 1.0);
    public static final ModConfigSpec.IntValue ARROW_MIN = BUILDER
            .comment("Minimum arrow count.")
            .defineInRange("loot.arrowMin", 4, 1, 64);
    public static final ModConfigSpec.IntValue ARROW_MAX = BUILDER
            .comment("Maximum arrow count.")
            .defineInRange("loot.arrowMax", 16, 1, 64);
    public static final ModConfigSpec.DoubleValue MATERIAL_CHANCE = BUILDER
            .comment("Chance for a generated chest to contain a material stack.")
            .defineInRange("loot.materialChance", 0.65, 0.0, 1.0);
    public static final ModConfigSpec.IntValue MATERIAL_MIN = BUILDER
            .comment("Minimum material item count.")
            .defineInRange("loot.materialMin", 2, 1, 64);
    public static final ModConfigSpec.IntValue MATERIAL_MAX = BUILDER
            .comment("Maximum material item count.")
            .defineInRange("loot.materialMax", 8, 1, 64);
    public static final ModConfigSpec.DoubleValue EQUIPMENT_CHANCE = BUILDER
            .comment("Chance for a generated chest to contain one equipment item.")
            .defineInRange("loot.equipmentChance", 0.25, 0.0, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private DungeonGenerationConfig() {
    }
}
