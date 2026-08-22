package com.dungeoncraft.dungeon;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** A complete COMBAT Room recipe: fixed dimensions, terrain and an oriented enemy party. */
public enum DungeonCombatEncounter {
    FRONTLINE_MARKSMEN("frontline_marksmen", 1, 21, 19, 7, 24, List.of(
            slot(EnemyKind.ZOMBIE, -2, -1), slot(EnemyKind.ZOMBIE, 2, -1),
            slot(EnemyKind.SKELETON, -5, 5), slot(EnemyKind.SKELETON, 5, 5))),
    HIGH_GROUND_ARCHERS("high_ground_archers", 1, 25, 21, 8, 20, List.of(
            slot(EnemyKind.ZOMBIE, -3, 0), slot(EnemyKind.ZOMBIE, 3, 0),
            slot(EnemyKind.ZOMBIE, 0, 3),
            slot(EnemyKind.SKELETON, -6, 6, 2), slot(EnemyKind.SKELETON, 6, 6, 2))),
    PILLAR_AMBUSH("pillar_ambush", 1, 23, 23, 8, 22, List.of(
            slot(EnemyKind.ZOMBIE, 0, 1), slot(EnemyKind.SPIDER, -6, 2),
            slot(EnemyKind.SPIDER, 6, 2), slot(EnemyKind.SKELETON, 0, 7))),
    CHOKEPOINT_DEFENSE("chokepoint_defense", 1, 19, 27, 7, 20, List.of(
            slot(EnemyKind.ZOMBIE, -1, 0), slot(EnemyKind.ZOMBIE, 1, 2),
            slot(EnemyKind.ZOMBIE, 0, 4),
            slot(EnemyKind.SKELETON, -4, 9), slot(EnemyKind.SKELETON, 4, 9))),
    MONSTER_HOUSE("monster_house", 1, 33, 33, 9, 14, List.of(
            slot(EnemyKind.ZOMBIE, -6, -2), slot(EnemyKind.ZOMBIE, 0, -3),
            slot(EnemyKind.ZOMBIE, 6, -2), slot(EnemyKind.ZOMBIE, -4, 3),
            slot(EnemyKind.ZOMBIE, 4, 3), slot(EnemyKind.ZOMBIE, 0, 7),
            slot(EnemyKind.SKELETON, -10, 7), slot(EnemyKind.SKELETON, 10, 7),
            slot(EnemyKind.SKELETON, 0, 11), slot(EnemyKind.SPIDER, 8, 1))),
    RAIDER_BARRICADE("raider_barricade", 2, 27, 23, 8, 24, List.of(
            slot(EnemyKind.VINDICATOR, -3, 0), slot(EnemyKind.VINDICATOR, 3, 0),
            slot(EnemyKind.VINDICATOR, 0, 3),
            slot(EnemyKind.PILLAGER, -7, 7), slot(EnemyKind.PILLAGER, 0, 9),
            slot(EnemyKind.PILLAGER, 7, 7))),
    NETHER_CROSSFIRE("nether_crossfire", 2, 27, 25, 9, 20, List.of(
            slot(EnemyKind.WITHER_SKELETON, -3, 0),
            slot(EnemyKind.WITHER_SKELETON, 3, 0),
            slot(EnemyKind.WITHER_SKELETON, 0, 4),
            slot(EnemyKind.BLAZE, -7, 7, 2), slot(EnemyKind.BLAZE, 7, 7, 2))),
    EVOKER_COURT("evoker_court", 3, 29, 27, 9, 20, List.of(
            slot(EnemyKind.VINDICATOR, -5, 1), slot(EnemyKind.VINDICATOR, 5, 1),
            slot(EnemyKind.PILLAGER, -8, 7), slot(EnemyKind.PILLAGER, 8, 7),
            slot(EnemyKind.EVOKER, 0, 8, 1), slot(EnemyKind.RAVAGER, 0, 2))),
    ENDER_SHRINE("ender_shrine", 3, 31, 31, 10, 20, List.of(
            slot(EnemyKind.ENDERMITE, -4, 0), slot(EnemyKind.ENDERMITE, 4, 0),
            slot(EnemyKind.ENDERMAN, -8, 5), slot(EnemyKind.ENDERMAN, 8, 5),
            slot(EnemyKind.SHULKER, -6, 10), slot(EnemyKind.SHULKER, 6, 10))),
    INFERNAL_MONSTER_HOUSE("infernal_monster_house", 3, 35, 35, 10, 10, List.of(
            slot(EnemyKind.WITHER_SKELETON, -8, -5),
            slot(EnemyKind.WITHER_SKELETON, 0, -6),
            slot(EnemyKind.WITHER_SKELETON, 8, -5),
            slot(EnemyKind.WITHER_SKELETON, -5, 1),
            slot(EnemyKind.WITHER_SKELETON, 5, 1),
            slot(EnemyKind.BLAZE, -10, 7, 2), slot(EnemyKind.BLAZE, 10, 7, 2),
            slot(EnemyKind.BLAZE, 0, 11, 2),
            slot(EnemyKind.VINDICATOR, -4, 8), slot(EnemyKind.VINDICATOR, 4, 8),
            slot(EnemyKind.PILLAGER, -11, 12), slot(EnemyKind.PILLAGER, 11, 12),
            slot(EnemyKind.ENDERMITE, -7, 3), slot(EnemyKind.ENDERMITE, 7, 3),
            slot(EnemyKind.RAVAGER, 0, 4)));

    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private final String id;
    private final int strength;
    private final int width;
    private final int depth;
    private final int height;
    private final int weight;
    private final List<EnemySlot> enemies;

    DungeonCombatEncounter(
            String id, int strength, int width, int depth, int height, int weight,
            List<EnemySlot> enemies) {
        this.id = id;
        this.strength = strength;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.weight = weight;
        this.enemies = List.copyOf(enemies);
    }

    public String id() { return id; }
    public int strength() { return strength; }
    public int width() { return width; }
    public int depth() { return depth; }
    public int height() { return height; }
    public List<EnemySlot> enemies() { return enemies; }

    public static DungeonCombatEncounter choose(
            Random random, int maximumStrength, int strengthModifierTier) {
        int clampedMaximum = Math.max(1, Math.min(3, maximumStrength));
        int totalWeight = 0;
        for (DungeonCombatEncounter encounter : values()) {
            if (encounter.strength <= clampedMaximum) {
                totalWeight += effectiveWeight(encounter, clampedMaximum, strengthModifierTier);
            }
        }
        int roll = random.nextInt(totalWeight);
        for (DungeonCombatEncounter encounter : values()) {
            if (encounter.strength > clampedMaximum) {
                continue;
            }
            roll -= effectiveWeight(encounter, clampedMaximum, strengthModifierTier);
            if (roll < 0) {
                return encounter;
            }
        }
        return FRONTLINE_MARKSMEN;
    }

    private static int effectiveWeight(
            DungeonCombatEncounter encounter, int maximumStrength, int strengthModifierTier) {
        int multiplier = encounter.strength == maximumStrength
                ? (maximumStrength == 2 && strengthModifierTier >= 4 ? 4 : 3)
                : 1;
        int result = encounter.weight * multiplier;
        if (maximumStrength == 1 && encounter == MONSTER_HOUSE) {
            result += encounter.weight * Math.max(0, Math.min(2, strengthModifierTier)) / 2;
        }
        return result;
    }

    public static DungeonCombatEncounter byId(String id) {
        for (DungeonCombatEncounter encounter : values()) {
            if (encounter.id.equals(id)) {
                return encounter;
            }
        }
        return FRONTLINE_MARKSMEN;
    }

    public void decorate(ServerLevel level, int centerX, int centerZ, Direction entrance) {
        switch (this) {
            case FRONTLINE_MARKSMEN -> {
                addLowWall(level, centerX, centerZ, entrance, -6, -3, 4);
                addLowWall(level, centerX, centerZ, entrance, 3, 6, 4);
            }
            case HIGH_GROUND_ARCHERS -> {
                addAccessiblePlatform(level, centerX, centerZ, entrance, -6, 6, 5, 5, 2);
                addAccessiblePlatform(level, centerX, centerZ, entrance, 6, 6, 5, 5, 2);
            }
            case PILLAR_AMBUSH -> {
                for (int side : List.of(-6, 0, 6)) {
                    for (int depth : List.of(0, 6)) {
                        addPillar(level, centerX, centerZ, entrance, side, depth, 5);
                    }
                }
            }
            case CHOKEPOINT_DEFENSE -> {
                addLaneWall(level, centerX, centerZ, entrance, -3);
                addLaneWall(level, centerX, centerZ, entrance, 3);
            }
            case MONSTER_HOUSE -> {
                for (int side : List.of(-10, 10)) {
                    for (int depth : List.of(-3, 5, 11)) {
                        addPillar(level, centerX, centerZ, entrance, side, depth, 4);
                    }
                }
                addLowWall(level, centerX, centerZ, entrance, -5, 5, 8);
            }
            case RAIDER_BARRICADE -> {
                addLowWall(level, centerX, centerZ, entrance, -10, -3, 6);
                addLowWall(level, centerX, centerZ, entrance, 3, 10, 6);
                addLowWall(level, centerX, centerZ, entrance, -5, 5, 10);
            }
            case NETHER_CROSSFIRE -> {
                addAccessiblePlatform(level, centerX, centerZ, entrance, -7, 7, 5, 5, 2);
                addAccessiblePlatform(level, centerX, centerZ, entrance, 7, 7, 5, 5, 2);
                for (int side : List.of(-10, 10)) {
                    addMaterialPillar(level, centerX, centerZ, entrance,
                            side, 1, 6, Blocks.POLISHED_BASALT.defaultBlockState());
                }
            }
            case EVOKER_COURT -> {
                addPlatform(level, centerX, centerZ, entrance, 0, 8, 7, 5, 1);
                for (int side : List.of(-9, 9)) {
                    addMaterialPillar(level, centerX, centerZ, entrance,
                            side, 7, 6, Blocks.DARK_OAK_LOG.defaultBlockState());
                }
                addLowWall(level, centerX, centerZ, entrance, -7, 7, 4);
            }
            case ENDER_SHRINE -> {
                for (int side : List.of(-9, -3, 3, 9)) {
                    addMaterialPillar(level, centerX, centerZ, entrance,
                            side, 9, 5, Blocks.PURPUR_PILLAR.defaultBlockState());
                }
                addPlatform(level, centerX, centerZ, entrance, 0, 4, 5, 5, 1);
            }
            case INFERNAL_MONSTER_HOUSE -> {
                for (int side : List.of(-12, 0, 12)) {
                    for (int depth : List.of(-3, 7, 13)) {
                        addMaterialPillar(level, centerX, centerZ, entrance,
                                side, depth, 6, Blocks.NETHER_BRICKS.defaultBlockState());
                    }
                }
                addAccessiblePlatform(level, centerX, centerZ, entrance, -10, 8, 5, 5, 2);
                addAccessiblePlatform(level, centerX, centerZ, entrance, 10, 8, 5, 5, 2);
            }
        }
    }

    public static BlockPos position(
            int centerX, int centerZ, Direction entrance, int side, int depth, int yOffset) {
        Direction away = entrance.getOpposite();
        int awayX = away.getStepX();
        int awayZ = away.getStepZ();
        int rightX = -awayZ;
        int rightZ = awayX;
        return new BlockPos(
                centerX + rightX * side + awayX * depth,
                PrototypeDungeonGenerator.PLAYER_Y + yOffset,
                centerZ + rightZ * side + awayZ * depth);
    }

    private static void addLowWall(
            ServerLevel level, int centerX, int centerZ, Direction entrance,
            int sideStart, int sideEnd, int depth) {
        for (int side = sideStart; side <= sideEnd; side++) {
            set(level, position(centerX, centerZ, entrance, side, depth, 0),
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        }
    }

    private static void addPillar(
            ServerLevel level, int centerX, int centerZ, Direction entrance,
            int side, int depth, int pillarHeight) {
        addMaterialPillar(level, centerX, centerZ, entrance, side, depth, pillarHeight,
                Blocks.CHISELED_DEEPSLATE.defaultBlockState());
    }

    private static void addMaterialPillar(
            ServerLevel level, int centerX, int centerZ, Direction entrance,
            int side, int depth, int pillarHeight, BlockState state) {
        BlockPos base = position(centerX, centerZ, entrance, side, depth, 0);
        for (int y = 0; y < pillarHeight; y++) {
            set(level, base.above(y), state);
        }
    }

    private static void addPlatform(
            ServerLevel level, int centerX, int centerZ, Direction entrance,
            int sideCenter, int depthCenter, int width, int platformDepth, int platformHeight) {
        int halfWidth = width / 2;
        int halfDepth = platformDepth / 2;
        for (int side = sideCenter - halfWidth; side <= sideCenter + halfWidth; side++) {
            for (int depth = depthCenter - halfDepth; depth <= depthCenter + halfDepth; depth++) {
                for (int y = 0; y < platformHeight; y++) {
                    set(level, position(centerX, centerZ, entrance, side, depth, y),
                            Blocks.DEEPSLATE_TILES.defaultBlockState());
                }
            }
        }
    }

    /** Builds one-block rises from the floor to the platform so melee players can always reach it. */
    private static void addAccessiblePlatform(
            ServerLevel level, int centerX, int centerZ, Direction entrance,
            int sideCenter, int depthCenter, int width, int platformDepth, int platformHeight) {
        addPlatform(level, centerX, centerZ, entrance,
                sideCenter, depthCenter, width, platformDepth, platformHeight);
        int nearEdge = depthCenter - platformDepth / 2;
        for (int step = 1; step < platformHeight; step++) {
            int stepDepth = nearEdge - (platformHeight - step);
            for (int y = 0; y < step; y++) {
                set(level, position(centerX, centerZ, entrance, sideCenter, stepDepth, y),
                        Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
    }

    private static void addLaneWall(
            ServerLevel level, int centerX, int centerZ, Direction entrance, int side) {
        for (int depth = -5; depth <= 7; depth++) {
            for (int y = 0; y < 2; y++) {
                set(level, position(centerX, centerZ, entrance, side, depth, y),
                        Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
            }
        }
    }

    private static void set(ServerLevel level, BlockPos position, BlockState state) {
        level.setBlock(position, state, UPDATE_FLAGS);
    }

    private static EnemySlot slot(EnemyKind kind, int side, int depth) {
        return slot(kind, side, depth, 0);
    }

    private static EnemySlot slot(EnemyKind kind, int side, int depth, int yOffset) {
        return new EnemySlot(kind, side, depth, yOffset);
    }

    public enum EnemyKind {
        ZOMBIE, SKELETON, SPIDER,
        PILLAGER, VINDICATOR, EVOKER, RAVAGER,
        WITHER_SKELETON, BLAZE,
        ENDERMAN, ENDERMITE, SHULKER
    }

    public record EnemySlot(EnemyKind kind, int side, int depth, int yOffset) {
    }
}
