package com.dungeoncraft.dungeon;

import com.dungeoncraft.config.DungeonGenerationConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Definitions and deterministic generation for Floor Choice modifiers. */
public final class DungeonFloorModifier {
    public static final Codec<AppliedModifier> APPLIED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(AppliedModifier::id),
            Codec.INT.fieldOf("tier").forGetter(AppliedModifier::tier),
            Codec.BOOL.fieldOf("positive").forGetter(AppliedModifier::positive),
            Codec.BOOL.fieldOf("persistent").forGetter(AppliedModifier::persistent)
    ).apply(instance, AppliedModifier::new));

    private static final List<Definition> POSITIVE = List.of(
            new Definition("extra_chests", true, true),
            new Definition("loot_amount", true, true),
            new Definition("loot_quality", true, true),
            new Definition("ore_loot", true, true),
            new Definition("enchanted_loot", true, true),
            new Definition("food_loot", true, true),
            new Definition("experience_boost", true, true));
    private static final List<Definition> NEGATIVE = List.of(
            new Definition("enemy_count", false, true),
            new Definition("enemy_strength", false, true),
            new Definition("enemy_equipment", false, true),
            new Definition("enchanted_enemies", false, true),
            new Definition("hunger_drain", false, true));

    private DungeonFloorModifier() {
    }

    public static List<FloorOption> generateOptions(long levelSeed, long dungeonId, int floorNumber) {
        Random random = new Random(
                levelSeed ^ dungeonId * 0xD6E8FEB86659FD93L ^ floorNumber * 0xA5A3564E27F8862BL);
        List<FloorOption> options = new ArrayList<>();
        Map<String, Integer> positiveUsage = new HashMap<>();
        Map<String, Integer> negativeUsage = new HashMap<>();
        for (int choiceIndex = 0; choiceIndex < 3; choiceIndex++) {
            int maxTier = DungeonGenerationConfig.MAX_MODIFIER_TIER.getAsInt();
            int score = choiceIndex + 1;
            CountPair counts = chooseCompatibleCounts(
                    random,
                    DungeonGenerationConfig.MIN_POSITIVE_MODIFIERS.getAsInt(),
                    DungeonGenerationConfig.MAX_POSITIVE_MODIFIERS.getAsInt(),
                    DungeonGenerationConfig.MIN_NEGATIVE_MODIFIERS.getAsInt(),
                    DungeonGenerationConfig.MAX_NEGATIVE_MODIFIERS.getAsInt(),
                    maxTier, score);
            int positiveCount = counts.positiveCount();
            int negativeCount = counts.negativeCount();
            List<Integer> positiveTiers = distributeScore(random, positiveCount, score, maxTier);
            List<Integer> negativeTiers = distributeScore(random, negativeCount, score, maxTier);
            List<AppliedModifier> modifiers = new ArrayList<>();
            modifiers.addAll(pickDiverse(random, POSITIVE, positiveTiers, positiveUsage));
            modifiers.addAll(pickDiverse(random, NEGATIVE, negativeTiers, negativeUsage));
            options.add(new FloorOption(choiceIndex, score, List.copyOf(modifiers)));
        }
        return List.copyOf(options);
    }

    private static CountPair chooseCompatibleCounts(
            Random random,
            int positiveMinimum, int positiveMaximum,
            int negativeMinimum, int negativeMaximum, int maxTier, int score) {
        List<CountPair> compatible = new ArrayList<>();
        int positiveLower = Math.min(positiveMinimum, positiveMaximum);
        int positiveUpper = Math.max(positiveMinimum, positiveMaximum);
        int negativeLower = Math.min(negativeMinimum, negativeMaximum);
        int negativeUpper = Math.max(negativeMinimum, negativeMaximum);
        for (int positiveCount = positiveLower; positiveCount <= positiveUpper; positiveCount++) {
            for (int negativeCount = negativeLower; negativeCount <= negativeUpper; negativeCount++) {
                int minimumScore = Math.max(positiveCount, negativeCount);
                int maximumScore = Math.min(positiveCount * maxTier, negativeCount * maxTier);
                if (score >= minimumScore && score <= maximumScore) {
                    compatible.add(new CountPair(positiveCount, negativeCount));
                }
            }
        }
        if (compatible.isEmpty()) {
            int fallbackCount = Math.max(1, (score + maxTier - 1) / maxTier);
            return new CountPair(fallbackCount, fallbackCount);
        }
        return compatible.get(random.nextInt(compatible.size()));
    }

    private static List<AppliedModifier> pickDiverse(
            Random random, List<Definition> definitions, List<Integer> tiers,
            Map<String, Integer> usage) {
        List<Definition> shuffled = new ArrayList<>(definitions);
        Collections.shuffle(shuffled, random);
        shuffled.sort((left, right) -> Integer.compare(
                usage.getOrDefault(left.id(), 0), usage.getOrDefault(right.id(), 0)));
        List<AppliedModifier> picked = new ArrayList<>();
        for (int index = 0; index < Math.min(tiers.size(), shuffled.size()); index++) {
            Definition definition = shuffled.get(index);
            picked.add(new AppliedModifier(
                    definition.id(), tiers.get(index), definition.positive(), definition.persistent()));
            usage.merge(definition.id(), 1, Integer::sum);
        }
        return List.copyOf(picked);
    }

    private static List<Integer> distributeScore(
            Random random, int modifierCount, int score, int maxTier) {
        List<Integer> tiers = new ArrayList<>(Collections.nCopies(modifierCount, 1));
        int remaining = score - modifierCount;
        while (remaining > 0) {
            List<Integer> available = new ArrayList<>();
            for (int index = 0; index < tiers.size(); index++) {
                if (tiers.get(index) < maxTier) {
                    available.add(index);
                }
            }
            int selected = available.get(random.nextInt(available.size()));
            tiers.set(selected, tiers.get(selected) + 1);
            remaining--;
        }
        Collections.shuffle(tiers, random);
        return List.copyOf(tiers);
    }

    private static int between(Random random, int minimum, int maximum) {
        int lower = Math.min(minimum, maximum);
        int upper = Math.max(minimum, maximum);
        return lower + random.nextInt(upper - lower + 1);
    }

    private record Definition(String id, boolean positive, boolean persistent) {
    }

    private record CountPair(int positiveCount, int negativeCount) {
    }

    public record AppliedModifier(String id, int tier, boolean positive, boolean persistent) {
    }

    public record FloorOption(int choiceIndex, int score, List<AppliedModifier> modifiers) {
        public FloorOption {
            modifiers = List.copyOf(modifiers);
            int positiveScore = modifiers.stream().filter(AppliedModifier::positive)
                    .mapToInt(AppliedModifier::tier).sum();
            int negativeScore = modifiers.stream().filter(modifier -> !modifier.positive())
                    .mapToInt(AppliedModifier::tier).sum();
            if (positiveScore != negativeScore || positiveScore != score) {
                throw new IllegalArgumentException("Floor modifier scores must be balanced");
            }
        }
    }
}
