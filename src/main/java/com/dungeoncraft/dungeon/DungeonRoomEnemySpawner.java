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
            Mob enemy = spawnEnemy(level, spawnPos, slot.kind());
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
