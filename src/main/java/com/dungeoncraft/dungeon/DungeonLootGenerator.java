package com.dungeoncraft.dungeon;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.config.DungeonGenerationConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Places a vanilla chest and fills it from configurable prototype loot categories. */
public final class DungeonLootGenerator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final List<Item> FOODS = List.of(Items.BREAD, Items.COOKED_BEEF, Items.BAKED_POTATO);
    private static final List<Item> MATERIALS = List.of(
            Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.REDSTONE);
    private static final List<Item> EQUIPMENT = List.of(
            Items.IRON_SWORD, Items.BOW, Items.SHIELD, Items.IRON_HELMET, Items.IRON_CHESTPLATE);
    private static final List<Item> LOW_ORES = List.of(
            Items.COAL, Items.COPPER_INGOT, Items.IRON_INGOT);
    private static final List<Item> MID_ORES = List.of(
            Items.COAL, Items.COPPER_INGOT, Items.IRON_INGOT,
            Items.GOLD_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI);
    private static final List<Item> ORES_T3 = List.of(
            Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT,
            Items.REDSTONE, Items.LAPIS_LAZULI, Items.DIAMOND);
    private static final List<Item> ORES_T4 = List.of(
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.REDSTONE,
            Items.LAPIS_LAZULI, Items.DIAMOND, Items.EMERALD);
    private static final List<Item> ORES_T5 = List.of(
            Items.GOLD_INGOT, Items.LAPIS_LAZULI, Items.DIAMOND, Items.EMERALD);
    private static final List<Item> QUALITY_T1 = List.of(
            Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_HELMET,
            Items.IRON_CHESTPLATE, Items.SHIELD);
    private static final List<Item> QUALITY_T2 = List.of(
            Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_CHESTPLATE,
            Items.IRON_BOOTS, Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE);
    private static final List<Item> QUALITY_T3 = List.of(
            Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE, Items.DIAMOND_BOOTS, Items.GOLDEN_APPLE);
    private static final List<Item> QUALITY_T4 = List.of(
            Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE, Items.DIAMOND_BOOTS, Items.GOLDEN_APPLE,
            Items.NETHERITE_SCRAP);
    private static final List<Item> QUALITY_T5 = List.of(
            Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE, Items.DIAMOND_BOOTS, Items.GOLDEN_APPLE,
            Items.NETHERITE_SCRAP);
    private static final List<Item> RARE_QUALITY_ITEMS = List.of(
            Items.ELYTRA, Items.TOTEM_OF_UNDYING, Items.ENCHANTED_GOLDEN_APPLE,
            Items.NETHER_STAR, Items.TRIDENT, Items.HEAVY_CORE,
            Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.NETHERITE_INGOT,
            Items.NETHERITE_SWORD);

    private DungeonLootGenerator() {
    }

    public static void placeAndFillChest(
            ServerLevel level, BlockPos position, Random random,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers) {
        level.setBlock(position, Blocks.CHEST.defaultBlockState(), UPDATE_FLAGS);
        if (!(level.getBlockEntity(position) instanceof ChestBlockEntity chest)) {
            DungeonCraft.LOGGER.warn("Could not create dungeon loot chest at {}", position);
            return;
        }

        List<ItemStack> loot = createLoot(level, random, activeModifiers);
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            slots.add(slot);
        }
        Collections.shuffle(slots, random);
        for (int index = 0; index < loot.size() && index < slots.size(); index++) {
            chest.setItem(slots.get(index), loot.get(index));
        }
        chest.setChanged();
    }

    public static void placeEnchantingSupplyChest(ServerLevel level, BlockPos position) {
        level.setBlock(position, Blocks.CHEST.defaultBlockState(), UPDATE_FLAGS);
        if (!(level.getBlockEntity(position) instanceof ChestBlockEntity chest)) {
            DungeonCraft.LOGGER.warn("Could not create enchanting supply chest at {}", position);
            return;
        }
        for (int slot = 9; slot <= 17; slot++) {
            chest.setItem(slot, new ItemStack(Items.LAPIS_LAZULI, 64));
        }
        chest.setItem(22, new ItemStack(Items.BOOK, 3));
        chest.setChanged();
    }

    private static List<ItemStack> createLoot(
            ServerLevel level, Random random,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers) {
        int amountTier = DungeonModifierEffects.tier(activeModifiers, "loot_amount");
        double stackMultiplier = DungeonModifierEffects.lootAmountMultiplier(activeModifiers);
        double slotMultiplier = DungeonModifierEffects.lootSlotMultiplier(activeModifiers);
        List<ItemStack> loot = createLootRoll(level, random, activeModifiers, stackMultiplier);
        int scaledSlots = (int)Math.ceil(loot.size() * slotMultiplier);
        int guaranteedTierSlots = loot.size() + amountTier;
        int targetSlots = Math.min(27, Math.max(scaledSlots, guaranteedTierSlots));
        while (loot.size() < targetSlots) {
            List<ItemStack> bonus = createLootRoll(level, random, activeModifiers, stackMultiplier);
            for (ItemStack stack : bonus) {
                if (loot.size() >= targetSlots) {
                    break;
                }
                loot.add(stack);
            }
        }
        return loot;
    }

    private static List<ItemStack> createLootRoll(
            ServerLevel level, Random random,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers,
            double amountMultiplier) {
        List<ItemStack> loot = new ArrayList<>();
        if (random.nextDouble() < DungeonGenerationConfig.FOOD_CHANCE.get()) {
            loot.add(randomStack(random, FOODS,
                    DungeonGenerationConfig.FOOD_MIN.getAsInt(), DungeonGenerationConfig.FOOD_MAX.getAsInt(),
                    amountMultiplier));
        }
        if (random.nextDouble() < DungeonGenerationConfig.ARROW_CHANCE.get()) {
            loot.add(new ItemStack(Items.ARROW, scaledCount(between(random,
                    DungeonGenerationConfig.ARROW_MIN.getAsInt(), DungeonGenerationConfig.ARROW_MAX.getAsInt()),
                    amountMultiplier, Items.ARROW)));
        }
        if (random.nextDouble() < DungeonGenerationConfig.MATERIAL_CHANCE.get()) {
            loot.add(randomStack(random, MATERIALS,
                    DungeonGenerationConfig.MATERIAL_MIN.getAsInt(), DungeonGenerationConfig.MATERIAL_MAX.getAsInt(),
                    amountMultiplier));
        }
        if (random.nextDouble() < DungeonGenerationConfig.EQUIPMENT_CHANCE.get()) {
            loot.add(new ItemStack(EQUIPMENT.get(random.nextInt(EQUIPMENT.size()))));
        }
        int qualityTier = DungeonModifierEffects.tier(activeModifiers, "loot_quality");
        if (qualityTier > 0
                && random.nextDouble() < DungeonModifierEffects.lootQualityChance(activeModifiers)) {
            List<Item> qualityItems = switch (qualityTier) {
                case 1 -> QUALITY_T1;
                case 2 -> QUALITY_T2;
                case 3 -> QUALITY_T3;
                case 4 -> QUALITY_T4;
                default -> QUALITY_T5;
            };
            loot.add(new ItemStack(qualityItems.get(random.nextInt(qualityItems.size()))));
        }
        if (qualityTier >= 4
                && random.nextDouble() < DungeonModifierEffects.rareLootChance(activeModifiers)) {
            loot.add(new ItemStack(
                    RARE_QUALITY_ITEMS.get(random.nextInt(RARE_QUALITY_ITEMS.size()))));
        }
        if (qualityTier > 0
                && random.nextDouble() < DungeonModifierEffects.strongBookChance(activeModifiers)) {
            loot.add(createStrongEnchantedBook(level, random));
        }
        int oreTier = DungeonModifierEffects.tier(activeModifiers, "ore_loot");
        if (oreTier > 0 && random.nextDouble() < DungeonModifierEffects.oreLootChance(activeModifiers)) {
            List<Item> ores = switch (oreTier) {
                case 1 -> LOW_ORES;
                case 2 -> MID_ORES;
                case 3 -> ORES_T3;
                case 4 -> ORES_T4;
                default -> ORES_T5;
            };
            double oreQuantityMultiplier = amountMultiplier * highTierRewardMultiplier(oreTier);
            loot.add(randomOreStack(random, ores,
                    DungeonGenerationConfig.MATERIAL_MIN.getAsInt(), DungeonGenerationConfig.MATERIAL_MAX.getAsInt(),
                    oreQuantityMultiplier));
        }
        int enchantedTier = DungeonModifierEffects.tier(activeModifiers, "enchanted_loot");
        if (enchantedTier > 0
                && random.nextDouble() < DungeonModifierEffects.enchantedLootChance(activeModifiers)) {
            int bookCount = highTierRewardMultiplier(enchantedTier);
            for (int bookIndex = 0; bookIndex < bookCount; bookIndex++) {
                ItemStack book = EnchantmentHelper.enchantItem(
                        RandomSource.create(random.nextLong()), new ItemStack(Items.BOOK),
                        DungeonModifierEffects.enchantmentCost(activeModifiers),
                        level.registryAccess(), java.util.Optional.empty());
                loot.add(book);
            }
        }
        if (DungeonModifierEffects.tier(activeModifiers, "food_loot") > 0
                && random.nextDouble() < DungeonModifierEffects.foodLootChance(activeModifiers)) {
            loot.add(randomStack(random, FOODS,
                    DungeonGenerationConfig.FOOD_MIN.getAsInt(), DungeonGenerationConfig.FOOD_MAX.getAsInt(),
                    amountMultiplier));
        }
        if (loot.isEmpty()) {
            loot.add(randomStack(random, FOODS,
                    DungeonGenerationConfig.FOOD_MIN.getAsInt(), DungeonGenerationConfig.FOOD_MAX.getAsInt(),
                    amountMultiplier));
        }
        return loot;
    }

    private static int highTierRewardMultiplier(int tier) {
        return switch (tier) {
            case 4 -> 2;
            case 5 -> 3;
            default -> 1;
        };
    }

    private static ItemStack createStrongEnchantedBook(ServerLevel level, Random random) {
        return EnchantmentHelper.enchantItem(
                RandomSource.create(random.nextLong()), new ItemStack(Items.BOOK), 40,
                level.registryAccess(), java.util.Optional.empty());
    }

    private static ItemStack randomStack(
            Random random, List<Item> items, int minimum, int maximum, double multiplier) {
        Item item = items.get(random.nextInt(items.size()));
        return new ItemStack(item, scaledCount(between(random, minimum, maximum), multiplier, item));
    }

    private static ItemStack randomOreStack(
            Random random, List<Item> items, int minimum, int maximum, double multiplier) {
        Item item = items.get(random.nextInt(items.size()));
        boolean rare = item == Items.DIAMOND || item == Items.EMERALD;
        int count = rare ? between(random, 1, 2) : between(random, minimum, maximum);
        return new ItemStack(item, scaledCount(count, multiplier, item));
    }

    private static int scaledCount(int count, double multiplier, Item item) {
        return Math.min(item.getDefaultMaxStackSize(), Math.max(1, (int)Math.ceil(count * multiplier)));
    }

    private static int between(Random random, int minimum, int maximum) {
        int lower = Math.min(minimum, maximum);
        int upper = Math.max(minimum, maximum);
        return lower + random.nextInt(upper - lower + 1);
    }
}
