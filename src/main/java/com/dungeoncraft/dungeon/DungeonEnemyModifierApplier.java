package com.dungeoncraft.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Applies persistent equipment-related modifiers without changing an encounter's combat role. */
public final class DungeonEnemyModifierApplier {
    private static final List<EquipmentSlot> ENCHANTABLE_SLOTS = List.of(
            EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private DungeonEnemyModifierApplier() {
    }

    public static void apply(
            ServerLevel level, Mob enemy, DungeonCombatEncounter.EnemyKind kind,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers) {
        if (!supportsEquipment(kind)) {
            return;
        }

        RandomSource random = RandomSource.create(
                enemy.getUUID().getMostSignificantBits() ^ enemy.getUUID().getLeastSignificantBits());
        int equipmentTier = DungeonModifierEffects.tier(activeModifiers, "enemy_equipment");
        if (equipmentTier > 0
                && random.nextDouble() < DungeonModifierEffects.enemyEquipmentChance(activeModifiers)) {
            equipArmorSet(enemy, equipmentTier);
            if (enemy.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                setModifierEquipment(enemy, EquipmentSlot.MAINHAND,
                        new ItemStack(meleeWeapon(equipmentTier)));
            }
        }

        int enchantedTier = DungeonModifierEffects.tier(activeModifiers, "enchanted_enemies");
        if (enchantedTier <= 0
                || random.nextDouble() >= DungeonModifierEffects.enchantedEnemyChance(activeModifiers)) {
            return;
        }

        ensureEnchantableEquipment(enemy);
        List<EquipmentSlot> occupied = new ArrayList<>();
        for (EquipmentSlot slot : ENCHANTABLE_SLOTS) {
            if (!enemy.getItemBySlot(slot).isEmpty()) {
                occupied.add(slot);
            }
        }
        if (occupied.isEmpty()) {
            return;
        }

        EquipmentSlot selectedSlot = occupied.get(random.nextInt(occupied.size()));
        ItemStack enchanted = EnchantmentHelper.enchantItem(
                random, enemy.getItemBySlot(selectedSlot).copy(),
                DungeonModifierEffects.enemyEnchantmentCost(activeModifiers),
                level.registryAccess(), Optional.empty());
        setModifierEquipment(enemy, selectedSlot, enchanted);
    }

    private static boolean supportsEquipment(DungeonCombatEncounter.EnemyKind kind) {
        return switch (kind) {
            case ZOMBIE, SKELETON, PILLAGER, VINDICATOR, EVOKER, WITHER_SKELETON -> true;
            default -> false;
        };
    }

    private static void equipArmorSet(Mob enemy, int tier) {
        if (tier >= 5) {
            setModifierEquipment(enemy, EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            setModifierEquipment(enemy, EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            setModifierEquipment(enemy, EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            setModifierEquipment(enemy, EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        } else if (tier >= 3) {
            setModifierEquipment(enemy, EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            setModifierEquipment(enemy, EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            setModifierEquipment(enemy, EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            setModifierEquipment(enemy, EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        } else {
            setModifierEquipment(enemy, EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            setModifierEquipment(enemy, EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            setModifierEquipment(enemy, EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            setModifierEquipment(enemy, EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        }
    }

    private static net.minecraft.world.item.Item meleeWeapon(int tier) {
        if (tier >= 5) {
            return Items.NETHERITE_SWORD;
        }
        if (tier >= 3) {
            return Items.DIAMOND_SWORD;
        }
        return Items.IRON_SWORD;
    }

    private static void ensureEnchantableEquipment(Mob enemy) {
        boolean hasEquipment = ENCHANTABLE_SLOTS.stream()
                .anyMatch(slot -> !enemy.getItemBySlot(slot).isEmpty());
        if (!hasEquipment) {
            setModifierEquipment(enemy, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    private static void setModifierEquipment(Mob enemy, EquipmentSlot slot, ItemStack stack) {
        enemy.setItemSlot(slot, stack);
        enemy.setDropChance(slot, 0.085F);
    }
}
