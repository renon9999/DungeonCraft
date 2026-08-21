package com.dungeoncraft.dungeon;

import com.dungeoncraft.config.DungeonGenerationConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Spawns one configurable enemy group when a COMBAT room is entered. */
public final class DungeonRoomEnemySpawner {
    private static final List<BlockPos> SPAWN_OFFSETS = List.of(
            new BlockPos(-3, 0, -3), new BlockPos(3, 0, -3),
            new BlockPos(-3, 0, 3), new BlockPos(3, 0, 3),
            new BlockPos(0, 0, -4), new BlockPos(4, 0, 0),
            new BlockPos(0, 0, 4), new BlockPos(-4, 0, 0));

    private DungeonRoomEnemySpawner() {
    }

    public static SpawnedEnemies spawn(ServerLevel level, DungeonProgressData.EnteredRoom room) {
        int targetCount = DungeonGenerationConfig.COMBAT_ENEMY_COUNT.getAsInt();
        List<UUID> enemyIds = new ArrayList<>();
        for (int index = 0; index < targetCount; index++) {
            BlockPos offset = SPAWN_OFFSETS.get(index % SPAWN_OFFSETS.size());
            BlockPos spawnPos = new BlockPos(
                    room.centerX() + offset.getX(),
                    PrototypeDungeonGenerator.PLAYER_Y,
                    room.centerZ() + offset.getZ());
            Zombie zombie = EntityTypes.ZOMBIE.spawn(level, spawnPos, EntitySpawnReason.EVENT);
            if (zombie != null) {
                zombie.addTag("dungeoncraft_room_enemy");
                zombie.addTag("dungeoncraft_dungeon_" + room.dungeonId());
                zombie.addTag("dungeoncraft_room_" + room.roomId());
                enemyIds.add(zombie.getUUID());
            }
        }
        return new SpawnedEnemies(List.copyOf(enemyIds));
    }

    public record SpawnedEnemies(List<UUID> enemyIds) {
        public int enemyCount() {
            return enemyIds.size();
        }
    }
}
