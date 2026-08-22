package com.dungeoncraft.dungeon;

import com.dungeoncraft.config.DungeonGenerationConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/** Spawns the oriented enemy party belonging to a COMBAT Room encounter pattern. */
public final class DungeonRoomEnemySpawner {
    private DungeonRoomEnemySpawner() {
    }

    public static SpawnedEnemies spawn(
            ServerLevel level, DungeonProgressData.EnteredRoom room,
            List<DungeonFloorModifier.AppliedModifier> activeModifiers) {
        DungeonCombatEncounter encounter = DungeonCombatEncounter.byId(room.encounterId());
        Direction entrance = Direction.byName(room.entranceDirection());
        if (entrance == null || !entrance.getAxis().isHorizontal()) {
            entrance = Direction.NORTH;
        }
        int basePartySize = encounter.enemies().size()
                * DungeonGenerationConfig.COMBAT_ENEMY_COUNT.getAsInt();
        int targetPartySize = Math.max(1, (int)Math.round(
                basePartySize * DungeonModifierEffects.enemyCountMultiplier(activeModifiers)));
        ArrayList<UUID> enemyIds = new ArrayList<>();
        for (int index = 0; index < targetPartySize; index++) {
            DungeonCombatEncounter.EnemySlot slot =
                    encounter.enemies().get(index % encounter.enemies().size());
            int copy = index / encounter.enemies().size();
            int sideSpread = copy == 0 ? 0 : (copy % 2 == 0 ? 1 : -1) * ((copy + 1) / 2);
            BlockPos spawnPos = DungeonCombatEncounter.position(
                    room.centerX(), room.centerZ(), entrance,
                    slot.side() + sideSpread, slot.depth(), slot.yOffset());
            BlockPos safeSpawnPos = findSafeSpawnPosition(level, room, spawnPos, slot.kind());
            if (safeSpawnPos == null) {
                continue;
            }
            Mob enemy = spawnEnemy(level, safeSpawnPos, slot.kind());
            if (enemy == null) {
                continue;
            }
            DungeonEnemyModifierApplier.apply(level, enemy, slot.kind(), activeModifiers);
            enemy.addTag("dungeoncraft_room_enemy");
            enemy.addTag("dungeoncraft_dungeon_" + room.dungeonId());
            enemy.addTag("dungeoncraft_room_" + room.roomId());
            enemyIds.add(enemy.getUUID());
        }
        return new SpawnedEnemies(java.util.List.copyOf(enemyIds), encounter.id());
    }

    /**
     * Keeps fixed encounter layouts while recovering from walls, pillars and added decorations.
     * Candidates are checked from the requested position outwards and never leave the room interior.
     */
    private static BlockPos findSafeSpawnPosition(
            ServerLevel level, DungeonProgressData.EnteredRoom room,
            BlockPos requested, DungeonCombatEncounter.EnemyKind kind) {
        for (int radius = 0; radius <= 4; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                int zOffset = radius - Math.abs(xOffset);
                BlockPos first = requested.offset(xOffset, 0, zOffset);
                if (isSafeSpawnPosition(level, room, first, kind)) {
                    return first;
                }
                if (zOffset != 0) {
                    BlockPos second = requested.offset(xOffset, 0, -zOffset);
                    if (isSafeSpawnPosition(level, room, second, kind)) {
                        return second;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeSpawnPosition(
            ServerLevel level, DungeonProgressData.EnteredRoom room,
            BlockPos position, DungeonCombatEncounter.EnemyKind kind) {
        int horizontalClearance = requiresWideClearance(kind) ? 1 : 0;
        int height = requiredHeight(kind);
        if (position.getX() - horizontalClearance < room.minX()
                || position.getX() + horizontalClearance > room.maxX()
                || position.getZ() - horizontalClearance < room.minZ()
                || position.getZ() + horizontalClearance > room.maxZ()) {
            return false;
        }
        for (int x = position.getX() - horizontalClearance;
             x <= position.getX() + horizontalClearance; x++) {
            for (int z = position.getZ() - horizontalClearance;
                 z <= position.getZ() + horizontalClearance; z++) {
                BlockPos floor = new BlockPos(x, position.getY() - 1, z);
                if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
                    return false;
                }
                for (int yOffset = 0; yOffset < height; yOffset++) {
                    BlockPos body = new BlockPos(x, position.getY() + yOffset, z);
                    if (!level.getBlockState(body).getCollisionShape(level, body).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean requiresWideClearance(DungeonCombatEncounter.EnemyKind kind) {
        return kind == DungeonCombatEncounter.EnemyKind.SPIDER
                || kind == DungeonCombatEncounter.EnemyKind.RAVAGER;
    }

    private static int requiredHeight(DungeonCombatEncounter.EnemyKind kind) {
        return switch (kind) {
            case ENDERMAN, RAVAGER, WITHER_SKELETON -> 3;
            case SPIDER, ENDERMITE, SHULKER -> 1;
            default -> 2;
        };
    }

    private static Mob spawnEnemy(
            ServerLevel level, BlockPos position, DungeonCombatEncounter.EnemyKind kind) {
        return switch (kind) {
            case ZOMBIE -> EntityTypes.ZOMBIE.spawn(level, position, EntitySpawnReason.EVENT);
            case SKELETON -> EntityTypes.SKELETON.spawn(level, position, EntitySpawnReason.EVENT);
            case SPIDER -> EntityTypes.SPIDER.spawn(level, position, EntitySpawnReason.EVENT);
            case PILLAGER -> EntityTypes.PILLAGER.spawn(level, position, EntitySpawnReason.EVENT);
            case VINDICATOR -> EntityTypes.VINDICATOR.spawn(level, position, EntitySpawnReason.EVENT);
            case EVOKER -> EntityTypes.EVOKER.spawn(level, position, EntitySpawnReason.EVENT);
            case RAVAGER -> EntityTypes.RAVAGER.spawn(level, position, EntitySpawnReason.EVENT);
            case WITHER_SKELETON -> EntityTypes.WITHER_SKELETON.spawn(
                    level, position, EntitySpawnReason.EVENT);
            case BLAZE -> EntityTypes.BLAZE.spawn(level, position, EntitySpawnReason.EVENT);
            case ENDERMAN -> EntityTypes.ENDERMAN.spawn(level, position, EntitySpawnReason.EVENT);
            case ENDERMITE -> EntityTypes.ENDERMITE.spawn(level, position, EntitySpawnReason.EVENT);
            case SHULKER -> EntityTypes.SHULKER.spawn(level, position, EntitySpawnReason.EVENT);
        };
    }

    public record SpawnedEnemies(java.util.List<UUID> enemyIds, String encounterId) {
        public int enemyCount() {
            return enemyIds.size();
        }
    }
}
