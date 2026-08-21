package com.dungeoncraft.dungeon;

import com.dungeoncraft.config.DungeonGenerationConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Central Tier-to-effect conversion for active Dungeon modifiers. */
public final class DungeonModifierEffects {
    private static final Set<String> LOOT_REWARD_MODIFIERS = Set.of(
            "extra_chests", "loot_amount", "loot_quality",
            "ore_loot", "enchanted_loot", "food_loot");
    private DungeonModifierEffects() {
    }

    public static int tier(List<DungeonFloorModifier.AppliedModifier> modifiers, String id) {
        return modifiers.stream()
                .filter(modifier -> modifier.id().equals(id))
                .mapToInt(DungeonFloorModifier.AppliedModifier::tier)
                .max()
                .orElse(0);
    }

    public static int extraChestCount(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return intByTier(tier(modifiers, "extra_chests"),
                DungeonGenerationConfig.EXTRA_CHESTS_T1.getAsInt(),
                DungeonGenerationConfig.EXTRA_CHESTS_T2.getAsInt(),
                DungeonGenerationConfig.EXTRA_CHESTS_T3.getAsInt(),
                DungeonGenerationConfig.EXTRA_CHESTS_T5.getAsInt());
    }

    public static double lootAmountMultiplier(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "loot_amount"), 1.0,
                DungeonGenerationConfig.LOOT_AMOUNT_T1.get(),
                DungeonGenerationConfig.LOOT_AMOUNT_T2.get(),
                DungeonGenerationConfig.LOOT_AMOUNT_T3.get(),
                DungeonGenerationConfig.LOOT_AMOUNT_T5.get());
    }

    public static double lootQualityChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "loot_quality"), 0.0,
                DungeonGenerationConfig.LOOT_QUALITY_T1.get(),
                DungeonGenerationConfig.LOOT_QUALITY_T2.get(),
                DungeonGenerationConfig.LOOT_QUALITY_T3.get(),
                DungeonGenerationConfig.LOOT_QUALITY_T5.get());
    }

    public static double oreLootChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "ore_loot"), 0.0,
                DungeonGenerationConfig.ORE_LOOT_T1.get(),
                DungeonGenerationConfig.ORE_LOOT_T2.get(),
                DungeonGenerationConfig.ORE_LOOT_T3.get(),
                DungeonGenerationConfig.ORE_LOOT_T5.get());
    }

    public static double enchantedLootChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "enchanted_loot"), 0.0,
                DungeonGenerationConfig.ENCHANTED_LOOT_T1.get(),
                DungeonGenerationConfig.ENCHANTED_LOOT_T2.get(),
                DungeonGenerationConfig.ENCHANTED_LOOT_T3.get(),
                DungeonGenerationConfig.ENCHANTED_LOOT_T5.get());
    }

    public static int enchantmentCost(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return intByTier(tier(modifiers, "enchanted_loot"),
                DungeonGenerationConfig.ENCHANTMENT_COST_T1.getAsInt(),
                DungeonGenerationConfig.ENCHANTMENT_COST_T2.getAsInt(),
                DungeonGenerationConfig.ENCHANTMENT_COST_T3.getAsInt(),
                DungeonGenerationConfig.ENCHANTMENT_COST_T5.getAsInt());
    }

    public static double foodLootChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "food_loot"), 0.0,
                DungeonGenerationConfig.FOOD_LOOT_T1.get(),
                DungeonGenerationConfig.FOOD_LOOT_T2.get(),
                DungeonGenerationConfig.FOOD_LOOT_T3.get(),
                DungeonGenerationConfig.FOOD_LOOT_T5.get());
    }

    public static double experienceMultiplier(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "experience_boost"), 1.0,
                DungeonGenerationConfig.EXPERIENCE_T1.get(),
                DungeonGenerationConfig.EXPERIENCE_T2.get(),
                DungeonGenerationConfig.EXPERIENCE_T3.get(),
                DungeonGenerationConfig.EXPERIENCE_T5.get());
    }

    public static double enemyCountMultiplier(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "enemy_count"), 1.0,
                DungeonGenerationConfig.ENEMY_COUNT_T1.get(),
                DungeonGenerationConfig.ENEMY_COUNT_T2.get(),
                DungeonGenerationConfig.ENEMY_COUNT_T3.get(),
                DungeonGenerationConfig.ENEMY_COUNT_T5.get());
    }

    /** T0-T2 use Strength 1, T3-T4 unlock Strength 2, and T5 unlocks Strength 3. */
    public static int maximumEnemyStrength(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        int tier = tier(modifiers, "enemy_strength");
        if (tier >= 5) {
            return 3;
        }
        if (tier >= 3) {
            return 2;
        }
        return 1;
    }

    public static double enemyEquipmentChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "enemy_equipment"), 0.0,
                DungeonGenerationConfig.ENEMY_EQUIPMENT_T1.get(),
                DungeonGenerationConfig.ENEMY_EQUIPMENT_T2.get(),
                DungeonGenerationConfig.ENEMY_EQUIPMENT_T3.get(),
                DungeonGenerationConfig.ENEMY_EQUIPMENT_T5.get());
    }

    public static double enchantedEnemyChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "enchanted_enemies"), 0.0,
                DungeonGenerationConfig.ENCHANTED_ENEMY_T1.get(),
                DungeonGenerationConfig.ENCHANTED_ENEMY_T2.get(),
                DungeonGenerationConfig.ENCHANTED_ENEMY_T3.get(),
                DungeonGenerationConfig.ENCHANTED_ENEMY_T5.get());
    }

    public static int enemyEnchantmentCost(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return intByTier(tier(modifiers, "enchanted_enemies"),
                DungeonGenerationConfig.ENEMY_ENCHANTMENT_COST_T1.getAsInt(),
                DungeonGenerationConfig.ENEMY_ENCHANTMENT_COST_T2.getAsInt(),
                DungeonGenerationConfig.ENEMY_ENCHANTMENT_COST_T3.getAsInt(),
                DungeonGenerationConfig.ENEMY_ENCHANTMENT_COST_T5.getAsInt());
    }

    public static double hungerExhaustionPerSecond(
            List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "hunger_drain"), 0.0,
                DungeonGenerationConfig.HUNGER_EXHAUSTION_T1.get(),
                DungeonGenerationConfig.HUNGER_EXHAUSTION_T2.get(),
                DungeonGenerationConfig.HUNGER_EXHAUSTION_T3.get(),
                DungeonGenerationConfig.HUNGER_EXHAUSTION_T5.get());
    }

    /** Creates an EXIT-reward-only view where every Loot modifier receives a Tier bonus. */
    public static List<DungeonFloorModifier.AppliedModifier> boostedLootModifiers(
            List<DungeonFloorModifier.AppliedModifier> modifiers, int tierBonus) {
        List<DungeonFloorModifier.AppliedModifier> boosted = new ArrayList<>();
        for (DungeonFloorModifier.AppliedModifier modifier : modifiers) {
            if (LOOT_REWARD_MODIFIERS.contains(modifier.id())) {
                boosted.add(new DungeonFloorModifier.AppliedModifier(
                        modifier.id(), Math.min(5, modifier.tier() + tierBonus),
                        modifier.positive(), modifier.persistent()));
            } else {
                boosted.add(modifier);
            }
        }
        for (String id : LOOT_REWARD_MODIFIERS) {
            if (boosted.stream().noneMatch(modifier -> modifier.id().equals(id))) {
                boosted.add(new DungeonFloorModifier.AppliedModifier(
                        id, Math.min(5, Math.max(0, tierBonus)), true, true));
            }
        }
        return List.copyOf(boosted);
    }

    private static int intByTier(int tier, int tier1, int tier2, int tier3, int tier5) {
        if (tier <= 3) {
            return switch (tier) {
            case 1 -> tier1;
            case 2 -> tier2;
            case 3 -> tier3;
            default -> 0;
            };
        }
        return (int)Math.round(interpolateFinalTier(tier, tier3, tier5));
    }

    private static double doubleByTier(
            int tier, double noModifier, double tier1, double tier2, double tier3, double tier5) {
        if (tier <= 3) {
            return switch (tier) {
            case 1 -> tier1;
            case 2 -> tier2;
            case 3 -> tier3;
            default -> noModifier;
            };
        }
        return interpolateFinalTier(tier, tier3, tier5);
    }

    private static double interpolateFinalTier(int tier, double tier3, double tier5) {
        int clampedTier = Math.min(
                DungeonGenerationConfig.MAX_ACCUMULATED_MODIFIER_TIER.getAsInt(),
                Math.max(3, tier));
        double progress = Math.min(1.0, (clampedTier - 3) / 2.0);
        return tier3 + (tier5 - tier3) * progress;
    }
}
