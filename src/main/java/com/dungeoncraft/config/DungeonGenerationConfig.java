package com.dungeoncraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DungeonGenerationConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_ROOMS = BUILDER
            .comment("Minimum number of rooms in a generated prototype dungeon.")
            .defineInRange("generation.minRooms", 6, 3, 20);
    public static final ModConfigSpec.IntValue MAX_ROOMS = BUILDER
            .comment("Maximum number of rooms in a generated prototype dungeon.")
            .defineInRange("generation.maxRooms", 6, 3, 20);
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
            .define("generation.lootRoomEnabled", true);
    public static final ModConfigSpec.IntValue COMBAT_ENEMY_COUNT = BUILDER
            .comment("Number of zombies spawned once when entering a COMBAT room.")
            .defineInRange("combat.enemyCount", 1, 1, 20);
    public static final ModConfigSpec.IntValue MAX_PARTY_SIZE = BUILDER
            .comment("Maximum number of players allowed in one dungeon party.")
            .defineInRange("multiplayer.maxPartySize", 8, 1, 8);
    public static final ModConfigSpec.IntValue MIN_POSITIVE_MODIFIERS = BUILDER
            .comment("Minimum number of positive modifiers offered by one Floor Choice.")
            .defineInRange("modifier.minPositiveCount", 2, 1, 4);
    public static final ModConfigSpec.IntValue MAX_POSITIVE_MODIFIERS = BUILDER
            .comment("Maximum number of positive modifiers offered by one Floor Choice.")
            .defineInRange("modifier.maxPositiveCount", 4, 1, 4);
    public static final ModConfigSpec.IntValue MIN_NEGATIVE_MODIFIERS = BUILDER
            .comment("Minimum number of negative modifiers offered by one Floor Choice.")
            .defineInRange("modifier.minNegativeCount", 2, 1, 4);
    public static final ModConfigSpec.IntValue MAX_NEGATIVE_MODIFIERS = BUILDER
            .comment("Maximum number of negative modifiers offered by one Floor Choice.")
            .defineInRange("modifier.maxNegativeCount", 4, 1, 4);
    public static final ModConfigSpec.IntValue MAX_MODIFIER_TIER = BUILDER
            .comment("Highest Tier increment available to generated Floor Choices.")
            .defineInRange("modifier.maxTier", 3, 1, 3);
    public static final ModConfigSpec.IntValue MAX_ACCUMULATED_MODIFIER_TIER = BUILDER
            .comment("Maximum accumulated Tier for one active modifier.")
            .defineInRange("modifier.maxAccumulatedTier", 10, 1, 100);
    public static final ModConfigSpec.IntValue EXTRA_CHESTS_T1 = BUILDER
            .comment("Extra LOOT-room chests from Chest Increase T1.")
            .defineInRange("modifier.lootEffects.extraChestsT1", 1, 0, 3);
    public static final ModConfigSpec.IntValue EXTRA_CHESTS_T2 = BUILDER
            .comment("Extra LOOT-room chests from Chest Increase T2.")
            .defineInRange("modifier.lootEffects.extraChestsT2", 2, 0, 3);
    public static final ModConfigSpec.IntValue EXTRA_CHESTS_T3 = BUILDER
            .comment("Extra LOOT-room chests from Chest Increase T3.")
            .defineInRange("modifier.lootEffects.extraChestsT3", 3, 0, 3);
    public static final ModConfigSpec.IntValue EXTRA_CHESTS_T10 = BUILDER
            .comment("Extra LOOT-room chests from Chest Increase T10.")
            .defineInRange("modifier.lootEffects.extraChestsT10", 6, 0, 10);
    public static final ModConfigSpec.DoubleValue LOOT_AMOUNT_T1 = multiplier("lootAmountT1", 1.25);
    public static final ModConfigSpec.DoubleValue LOOT_AMOUNT_T2 = multiplier("lootAmountT2", 1.5);
    public static final ModConfigSpec.DoubleValue LOOT_AMOUNT_T3 = multiplier("lootAmountT3", 2.0);
    public static final ModConfigSpec.DoubleValue LOOT_AMOUNT_T10 = multiplier("lootAmountT10", 3.0);
    public static final ModConfigSpec.DoubleValue LOOT_QUALITY_T1 = chance("lootQualityChanceT1", 0.35);
    public static final ModConfigSpec.DoubleValue LOOT_QUALITY_T2 = chance("lootQualityChanceT2", 0.65);
    public static final ModConfigSpec.DoubleValue LOOT_QUALITY_T3 = chance("lootQualityChanceT3", 1.0);
    public static final ModConfigSpec.DoubleValue LOOT_QUALITY_T10 = chance("lootQualityChanceT10", 1.0);
    public static final ModConfigSpec.DoubleValue ORE_LOOT_T1 = chance("oreLootChanceT1", 0.35);
    public static final ModConfigSpec.DoubleValue ORE_LOOT_T2 = chance("oreLootChanceT2", 0.65);
    public static final ModConfigSpec.DoubleValue ORE_LOOT_T3 = chance("oreLootChanceT3", 1.0);
    public static final ModConfigSpec.DoubleValue ORE_LOOT_T10 = chance("oreLootChanceT10", 1.0);
    public static final ModConfigSpec.DoubleValue ENCHANTED_LOOT_T1 = chance("enchantedLootChanceT1", 0.25);
    public static final ModConfigSpec.DoubleValue ENCHANTED_LOOT_T2 = chance("enchantedLootChanceT2", 0.5);
    public static final ModConfigSpec.DoubleValue ENCHANTED_LOOT_T3 = chance("enchantedLootChanceT3", 0.75);
    public static final ModConfigSpec.DoubleValue ENCHANTED_LOOT_T10 = chance("enchantedLootChanceT10", 1.0);
    public static final ModConfigSpec.IntValue ENCHANTMENT_COST_T1 = enchantmentCost("enchantmentCostT1", 10);
    public static final ModConfigSpec.IntValue ENCHANTMENT_COST_T2 = enchantmentCost("enchantmentCostT2", 20);
    public static final ModConfigSpec.IntValue ENCHANTMENT_COST_T3 = enchantmentCost("enchantmentCostT3", 30);
    public static final ModConfigSpec.IntValue ENCHANTMENT_COST_T10 = enchantmentCost("enchantmentCostT10", 50);
    public static final ModConfigSpec.DoubleValue FOOD_LOOT_T1 = chance("foodLootChanceT1", 0.35);
    public static final ModConfigSpec.DoubleValue FOOD_LOOT_T2 = chance("foodLootChanceT2", 0.65);
    public static final ModConfigSpec.DoubleValue FOOD_LOOT_T3 = chance("foodLootChanceT3", 1.0);
    public static final ModConfigSpec.DoubleValue FOOD_LOOT_T10 = chance("foodLootChanceT10", 1.0);
    public static final ModConfigSpec.DoubleValue EXPERIENCE_T1 = multiplier("experienceMultiplierT1", 1.25);
    public static final ModConfigSpec.DoubleValue EXPERIENCE_T2 = multiplier("experienceMultiplierT2", 1.5);
    public static final ModConfigSpec.DoubleValue EXPERIENCE_T3 = multiplier("experienceMultiplierT3", 2.0);
    public static final ModConfigSpec.DoubleValue EXPERIENCE_T10 = multiplier("experienceMultiplierT10", 3.0);
    public static final ModConfigSpec.DoubleValue EXTRA_CHEST_CHANCE = BUILDER
            .comment("Chance for a NORMAL room to contain a chest. LOOT rooms always contain one; COMBAT rooms never do.")
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

    private static ModConfigSpec.DoubleValue chance(String name, double defaultValue) {
        return BUILDER.comment("Modifier effect chance for " + name + ".")
                .defineInRange("modifier.lootEffects." + name, defaultValue, 0.0, 1.0);
    }

    private static ModConfigSpec.DoubleValue multiplier(String name, double defaultValue) {
        return BUILDER.comment("Modifier effect multiplier for " + name + ".")
                .defineInRange("modifier.lootEffects." + name, defaultValue, 1.0, 10.0);
    }

    private static ModConfigSpec.IntValue enchantmentCost(String name, int defaultValue) {
        return BUILDER.comment("Enchanting cost used for " + name + ".")
                .defineInRange("modifier.lootEffects." + name, defaultValue, 1, 100);
    }

    private DungeonGenerationConfig() {
    }
}
