package com.dungeoncraft.dungeon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Runtime-only allocator and time-sliced cleaner for three physical floor slots per dungeon. */
public final class DungeonCleanupManager {
    private static final int PHYSICAL_FLOOR_SLOTS = 3;
    private static final int MAX_BLOCKS_PER_TICK = 2_048;
    private static final long MAX_NANOS_PER_TICK = 2_000_000L;

    private static final Map<Long, ActiveInstance> ACTIVE = new HashMap<>();
    private static final ArrayDeque<Long> FREE_INSTANCE_SLOTS = new ArrayDeque<>();
    private static final ArrayDeque<CleanupJob> JOBS = new ArrayDeque<>();
    private static final Set<SlotKey> CLEANING_SLOTS = new HashSet<>();
    private static final Map<Long, Integer> OUTSTANDING_JOBS = new HashMap<>();
    private static final Set<Long> RETIRED_DUNGEONS = new HashSet<>();
    private static long nextInstanceSlot;

    private DungeonCleanupManager() {
    }

    public static long allocateInstance(long dungeonId) {
        long instanceSlot;
        if (FREE_INSTANCE_SLOTS.isEmpty()) {
            instanceSlot = Math.max(nextInstanceSlot, Math.max(0L, dungeonId - 1L));
            nextInstanceSlot = instanceSlot + 1L;
        } else {
            instanceSlot = FREE_INSTANCE_SLOTS.removeFirst();
        }
        ACTIVE.put(dungeonId, new ActiveInstance(instanceSlot));
        return instanceSlot;
    }

    public static void registerInitialFloor(
            long dungeonId, int floorNumber,
            List<PrototypeDungeonGenerator.CleanupRegion> cleanupRegions) {
        ActiveInstance instance = ACTIVE.get(dungeonId);
        if (instance != null) {
            instance.currentFloor = new FloorAllocation(
                    physicalSlot(floorNumber), List.copyOf(cleanupRegions));
        }
    }

    public static long instanceSlot(long dungeonId) {
        ActiveInstance instance = ACTIVE.get(dungeonId);
        return instance == null ? -1L : instance.instanceSlot;
    }

    public static boolean isFloorSlotReady(long dungeonId, int floorNumber) {
        ActiveInstance instance = ACTIVE.get(dungeonId);
        return instance != null
                && !CLEANING_SLOTS.contains(new SlotKey(
                        instance.instanceSlot, physicalSlot(floorNumber)));
    }

    public static void transitionToFloor(
            long dungeonId, int floorNumber,
            List<PrototypeDungeonGenerator.CleanupRegion> cleanupRegions) {
        ActiveInstance instance = ACTIVE.get(dungeonId);
        if (instance == null) {
            return;
        }
        if (instance.currentFloor != null) {
            enqueue(dungeonId, instance.instanceSlot, instance.currentFloor);
        }
        instance.currentFloor = new FloorAllocation(
                physicalSlot(floorNumber), List.copyOf(cleanupRegions));
    }

    /** Cleans the last active floor, then returns the whole A/B/C instance area to the free pool. */
    public static void retireDungeon(long dungeonId) {
        ActiveInstance instance = ACTIVE.remove(dungeonId);
        if (instance == null) {
            return;
        }
        RETIRED_DUNGEONS.add(dungeonId);
        if (instance.currentFloor != null) {
            enqueue(dungeonId, instance.instanceSlot, instance.currentFloor);
        }
        releaseIfFinished(dungeonId, instance.instanceSlot);
    }

    public static void tick(MinecraftServer server) {
        CleanupJob job = JOBS.pollFirst();
        if (job == null) {
            return;
        }
        ServerLevel level = server.getLevel(com.dungeoncraft.DungeonCraft.DUNGEON_LEVEL);
        if (level == null) {
            JOBS.addFirst(job);
            return;
        }
        long deadline = System.nanoTime() + MAX_NANOS_PER_TICK;
        boolean complete = job.process(level, MAX_BLOCKS_PER_TICK, deadline);
        if (complete) {
            finish(job);
        } else {
            JOBS.addLast(job);
        }
    }

    private static void enqueue(
            long dungeonId, long instanceSlot, FloorAllocation floor) {
        SlotKey slotKey = new SlotKey(instanceSlot, floor.physicalSlot());
        if (!CLEANING_SLOTS.add(slotKey)) {
            return;
        }
        JOBS.addLast(new CleanupJob(
                dungeonId, instanceSlot, slotKey, new ArrayList<>(floor.cleanupRegions())));
        OUTSTANDING_JOBS.merge(dungeonId, 1, Integer::sum);
    }

    private static void finish(CleanupJob job) {
        CLEANING_SLOTS.remove(job.slotKey);
        int remaining = OUTSTANDING_JOBS.getOrDefault(job.dungeonId, 1) - 1;
        if (remaining <= 0) {
            OUTSTANDING_JOBS.remove(job.dungeonId);
        } else {
            OUTSTANDING_JOBS.put(job.dungeonId, remaining);
        }
        releaseIfFinished(job.dungeonId, job.instanceSlot);
    }

    private static void releaseIfFinished(long dungeonId, long instanceSlot) {
        if (RETIRED_DUNGEONS.contains(dungeonId)
                && !OUTSTANDING_JOBS.containsKey(dungeonId)) {
            RETIRED_DUNGEONS.remove(dungeonId);
            FREE_INSTANCE_SLOTS.addLast(instanceSlot);
        }
    }

    private static int physicalSlot(int floorNumber) {
        return Math.floorMod(floorNumber - 1, PHYSICAL_FLOOR_SLOTS);
    }

    private static final class ActiveInstance {
        private final long instanceSlot;
        private FloorAllocation currentFloor;

        private ActiveInstance(long instanceSlot) {
            this.instanceSlot = instanceSlot;
        }
    }

    private record FloorAllocation(
            int physicalSlot,
            List<PrototypeDungeonGenerator.CleanupRegion> cleanupRegions) {
    }

    private record SlotKey(long instanceSlot, int physicalSlot) {
    }

    private static final class CleanupJob {
        private final long dungeonId;
        private final long instanceSlot;
        private final SlotKey slotKey;
        private final List<PrototypeDungeonGenerator.CleanupRegion> regions;
        private int regionIndex;
        private int x;
        private int y;
        private int z;
        private boolean regionPrepared;

        private CleanupJob(
                long dungeonId, long instanceSlot, SlotKey slotKey,
                List<PrototypeDungeonGenerator.CleanupRegion> regions) {
            this.dungeonId = dungeonId;
            this.instanceSlot = instanceSlot;
            this.slotKey = slotKey;
            this.regions = regions;
        }

        private boolean process(ServerLevel level, int blockBudget, long deadline) {
            int processed = 0;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            while (processed < blockBudget && System.nanoTime() < deadline) {
                if (regionIndex >= regions.size()) {
                    return true;
                }
                PrototypeDungeonGenerator.CleanupRegion region = regions.get(regionIndex);
                if (!regionPrepared) {
                    prepareRegion(level, region);
                    x = region.minX();
                    y = region.minY();
                    z = region.minZ();
                    regionPrepared = true;
                }
                cursor.set(x, y, z);
                if (!level.getBlockState(cursor).isAir()) {
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                processed++;
                advance(region);
            }
            return regionIndex >= regions.size();
        }

        private void prepareRegion(
                ServerLevel level, PrototypeDungeonGenerator.CleanupRegion region) {
            AABB bounds = new AABB(
                    region.minX(), region.minY(), region.minZ(),
                    region.maxX() + 1.0, region.maxY() + 1.0, region.maxZ() + 1.0);
            level.getEntities((Entity)null, bounds, entity -> !(entity instanceof Player))
                    .forEach(Entity::discard);
        }

        private void advance(PrototypeDungeonGenerator.CleanupRegion region) {
            x++;
            if (x <= region.maxX()) {
                return;
            }
            x = region.minX();
            z++;
            if (z <= region.maxZ()) {
                return;
            }
            z = region.minZ();
            y++;
            if (y <= region.maxY()) {
                return;
            }
            regionIndex++;
            regionPrepared = false;
        }
    }
}
