package com.dungeoncraft.dungeon;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.config.DungeonGenerationConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    private DungeonLootGenerator() {
    }

    public static void placeAndFillChest(ServerLevel level, BlockPos position, Random random) {
        level.setBlock(position, Blocks.CHEST.defaultBlockState(), UPDATE_FLAGS);
        if (!(level.getBlockEntity(position) instanceof ChestBlockEntity chest)) {
            DungeonCraft.LOGGER.warn("Could not create dungeon loot chest at {}", position);
            return;
        }

        List<ItemStack> loot = createLoot(random);
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

    private static List<ItemStack> createLoot(Random random) {
        List<ItemStack> loot = new ArrayList<>();
        if (random.nextDouble() < DungeonGenerationConfig.FOOD_CHANCE.get()) {
            loot.add(randomStack(random, FOODS,
                    DungeonGenerationConfig.FOOD_MIN.getAsInt(), DungeonGenerationConfig.FOOD_MAX.getAsInt()));
        }
        if (random.nextDouble() < DungeonGenerationConfig.ARROW_CHANCE.get()) {
            loot.add(new ItemStack(Items.ARROW, between(random,
                    DungeonGenerationConfig.ARROW_MIN.getAsInt(), DungeonGenerationConfig.ARROW_MAX.getAsInt())));
        }
        if (random.nextDouble() < DungeonGenerationConfig.MATERIAL_CHANCE.get()) {
            loot.add(randomStack(random, MATERIALS,
                    DungeonGenerationConfig.MATERIAL_MIN.getAsInt(), DungeonGenerationConfig.MATERIAL_MAX.getAsInt()));
        }
        if (random.nextDouble() < DungeonGenerationConfig.EQUIPMENT_CHANCE.get()) {
            loot.add(new ItemStack(EQUIPMENT.get(random.nextInt(EQUIPMENT.size()))));
        }
        if (loot.isEmpty()) {
            loot.add(randomStack(random, FOODS,
                    DungeonGenerationConfig.FOOD_MIN.getAsInt(), DungeonGenerationConfig.FOOD_MAX.getAsInt()));
        }
        return loot;
    }

    private static ItemStack randomStack(Random random, List<Item> items, int minimum, int maximum) {
        return new ItemStack(items.get(random.nextInt(items.size())), between(random, minimum, maximum));
    }

    private static int between(Random random, int minimum, int maximum) {
        int lower = Math.min(minimum, maximum);
        int upper = Math.max(minimum, maximum);
        return lower + random.nextInt(upper - lower + 1);
    }
}
