package com.dungeoncraft.dungeon;

import com.dungeoncraft.config.DungeonGenerationConfig;
import java.util.List;

/** Central Tier-to-effect conversion for active Dungeon modifiers. */
public final class DungeonModifierEffects {
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
                DungeonGenerationConfig.EXTRA_CHESTS_T10.getAsInt());
    }

    public static double lootAmountMultiplier(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "loot_amount"), 1.0,
                DungeonGenerationConfig.LOOT_AMOUNT_T1.get(),
                DungeonGenerationConfig.LOOT_AMOUNT_T2.get(),
                DungeonGenerationConfig.LOOT_AMOUNT_T3.get(),
                DungeonGenerationConfig.LOOT_AMOUNT_T10.get());
    }

    public static double lootQualityChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "loot_quality"), 0.0,
                DungeonGenerationConfig.LOOT_QUALITY_T1.get(),
                DungeonGenerationConfig.LOOT_QUALITY_T2.get(),
                DungeonGenerationConfig.LOOT_QUALITY_T3.get(),
                DungeonGenerationConfig.LOOT_QUALITY_T10.get());
    }

    public static double oreLootChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "ore_loot"), 0.0,
                DungeonGenerationConfig.ORE_LOOT_T1.get(),
                DungeonGenerationConfig.ORE_LOOT_T2.get(),
                DungeonGenerationConfig.ORE_LOOT_T3.get(),
                DungeonGenerationConfig.ORE_LOOT_T10.get());
    }

    public static double enchantedLootChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "enchanted_loot"), 0.0,
                DungeonGenerationConfig.ENCHANTED_LOOT_T1.get(),
                DungeonGenerationConfig.ENCHANTED_LOOT_T2.get(),
                DungeonGenerationConfig.ENCHANTED_LOOT_T3.get(),
                DungeonGenerationConfig.ENCHANTED_LOOT_T10.get());
    }

    public static int enchantmentCost(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return intByTier(tier(modifiers, "enchanted_loot"),
                DungeonGenerationConfig.ENCHANTMENT_COST_T1.getAsInt(),
                DungeonGenerationConfig.ENCHANTMENT_COST_T2.getAsInt(),
                DungeonGenerationConfig.ENCHANTMENT_COST_T3.getAsInt(),
                DungeonGenerationConfig.ENCHANTMENT_COST_T10.getAsInt());
    }

    public static double foodLootChance(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "food_loot"), 0.0,
                DungeonGenerationConfig.FOOD_LOOT_T1.get(),
                DungeonGenerationConfig.FOOD_LOOT_T2.get(),
                DungeonGenerationConfig.FOOD_LOOT_T3.get(),
                DungeonGenerationConfig.FOOD_LOOT_T10.get());
    }

    public static double experienceMultiplier(List<DungeonFloorModifier.AppliedModifier> modifiers) {
        return doubleByTier(tier(modifiers, "experience_boost"), 1.0,
                DungeonGenerationConfig.EXPERIENCE_T1.get(),
                DungeonGenerationConfig.EXPERIENCE_T2.get(),
                DungeonGenerationConfig.EXPERIENCE_T3.get(),
                DungeonGenerationConfig.EXPERIENCE_T10.get());
    }

    private static int intByTier(int tier, int tier1, int tier2, int tier3, int tier10) {
        if (tier <= 3) {
            return switch (tier) {
            case 1 -> tier1;
            case 2 -> tier2;
            case 3 -> tier3;
            default -> 0;
            };
        }
        return (int)Math.round(interpolateTier10(tier, tier3, tier10));
    }

    private static double doubleByTier(
            int tier, double noModifier, double tier1, double tier2, double tier3, double tier10) {
        if (tier <= 3) {
            return switch (tier) {
            case 1 -> tier1;
            case 2 -> tier2;
            case 3 -> tier3;
            default -> noModifier;
            };
        }
        return interpolateTier10(tier, tier3, tier10);
    }

    private static double interpolateTier10(int tier, double tier3, double tier10) {
        int clampedTier = Math.min(
                DungeonGenerationConfig.MAX_ACCUMULATED_MODIFIER_TIER.getAsInt(),
                Math.max(3, tier));
        double progress = Math.min(1.0, (clampedTier - 3) / 7.0);
        return tier3 + (tier10 - tier3) * progress;
    }
}
