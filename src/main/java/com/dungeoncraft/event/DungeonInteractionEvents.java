package com.dungeoncraft.event;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.dungeon.DungeonReturnData;
import com.dungeoncraft.dungeon.DungeonProgressData;
import com.dungeoncraft.dungeon.DungeonRoomEnemySpawner;
import com.dungeoncraft.dungeon.DungeonRoomSealer;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class DungeonInteractionEvents {
    private DungeonInteractionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().dimension() != DungeonCraft.DUNGEON_LEVEL) {
            return;
        }

        DungeonProgressData.get(player.level().getServer())
                .enterRoom(player.getUUID(), player.blockPosition())
                .ifPresent(room -> {
                    if (room.isCombat()) {
                        DungeonRoomEnemySpawner.SpawnedEnemies enemies =
                                DungeonRoomEnemySpawner.spawn((ServerLevel)player.level(), room);
                        DungeonProgressData.get(player.level().getServer())
                                .startRoomCombat(player.getUUID(), room.roomId(), enemies.enemyIds());
                        player.sendOverlayMessage(Component.translatable(
                                "message.dungeoncraft.combat.started",
                                room.roomId() + 1, enemies.enemyCount()));
                        if (enemies.enemyCount() == 0) {
                            DungeonRoomSealer.open((ServerLevel)player.level(),
                                    DungeonProgressData.get(player.level().getServer())
                                            .clearRoom(player.getUUID(), room.roomId()));
                        }
                    } else {
                        DungeonRoomSealer.open((ServerLevel)player.level(), room.exitSealBlocks());
                        player.sendOverlayMessage(Component.translatable(
                                "message.dungeoncraft.room.entered", room.roomId() + 1, room.role()));
                    }
                });
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().dimension() == DungeonCraft.DUNGEON_LEVEL) {
            DungeonProgressData.get(player.level().getServer()).endRun(player.getUUID());
            DungeonReturnData.get(player.level().getServer()).discardReturn(player.getUUID());
            return;
        }
        handleRoomEnemyRemoved(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getLevel().getServer() != null
                && event.getLevel().getServer().isRunning()) {
            handleRoomEnemyRemoved(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().dimension() != DungeonCraft.DUNGEON_LEVEL) {
            return;
        }

        DungeonProgressData.get(player.level().getServer()).endRun(player.getUUID());
        DungeonReturnData.get(player.level().getServer()).discardReturn(player.getUUID());
        TeleportTransition respawn = player.findRespawnPositionAndUseSpawnBlock(
                false, TeleportTransition.DO_NOTHING);
        player.teleport(respawn);
    }

    private static void handleRoomEnemyRemoved(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || level.dimension() != DungeonCraft.DUNGEON_LEVEL
                || !entity.entityTags().contains("dungeoncraft_room_enemy")) {
            return;
        }

        DungeonProgressData.get(level.getServer()).removeRoomEnemy(entity.getUUID()).ifPresent(cleared -> {
            DungeonRoomSealer.open(level, cleared.exitSealBlocks());
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(cleared.playerId());
            if (player != null) {
                player.sendOverlayMessage(Component.translatable(
                        "message.dungeoncraft.room.cleared", cleared.roomId() + 1));
            }
        });
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().dimension() != DungeonCraft.DUNGEON_LEVEL
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.LEVER)) {
            return;
        }

        DungeonProgressData progress = DungeonProgressData.get(player.level().getServer());
        var floorExit = progress.findFloorExit(player.getUUID(), event.getPos());
        if (floorExit.isPresent() && !floorExit.orElseThrow().isFinalFloor()) {
            DungeonProgressData.FloorExitAction action = floorExit.orElseThrow();
            BlockPos nextSpawn = action.nextFloorSpawn().orElseThrow();
            player.teleportTo((ServerLevel)player.level(),
                    nextSpawn.getX() + 0.5, nextSpawn.getY(), nextSpawn.getZ() + 0.5,
                    Set.of(), player.getYRot(), player.getXRot(), true);
            player.sendOverlayMessage(Component.translatable(
                    "message.dungeoncraft.floor.entered", action.floorNumber() + 1, action.floorCount()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
            return;
        }

        boolean dungeonExit = floorExit.isPresent();
        DungeonReturnData data = DungeonReturnData.get(player.level().getServer());
        var returnTarget = dungeonExit
                ? data.takeReturn(player.getUUID())
                : data.takeReturn(player.getUUID(), event.getPos());
        if (returnTarget.isEmpty()) {
            return;
        }

        DungeonReturnData.ReturnTarget target = returnTarget.orElseThrow();
        ResourceKey<Level> returnKey = ResourceKey.create(
                Registries.DIMENSION, Identifier.parse(target.dimension()));
        ServerLevel returnLevel = player.level().getServer().getLevel(returnKey);
        if (returnLevel == null) {
            returnLevel = player.level().getServer().overworld();
        }
        progress.endRun(player.getUUID());
        player.teleportTo(returnLevel, target.x(), target.y(), target.z(), Set.of(),
                player.getYRot(), player.getXRot(), true);
        player.sendOverlayMessage(Component.translatable(dungeonExit
                ? "message.dungeoncraft.dungeon.completed"
                : "message.dungeoncraft.returned"));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }
}
