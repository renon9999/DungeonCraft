package com.dungeoncraft.event;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.dungeon.DungeonReturnData;
import com.dungeoncraft.dungeon.DungeonProgressData;
import com.dungeoncraft.dungeon.DungeonModifierEffects;
import com.dungeoncraft.dungeon.DungeonRoomEnemySpawner;
import com.dungeoncraft.dungeon.DungeonRoomSealer;
import com.dungeoncraft.dungeon.PrototypeDungeonGenerator;
import com.dungeoncraft.network.ActiveModifiersHudPayload;
import com.dungeoncraft.network.FloorChoiceHudPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DungeonInteractionEvents {
    private static final double CHOICE_HUD_SHOW_DISTANCE_SQUARED = 16.0;
    private static final double CHOICE_HUD_HIDE_DISTANCE_SQUARED = 25.0;
    private static final Map<java.util.UUID, ChoiceHudKey> VISIBLE_CHOICES = new HashMap<>();
    private static final Map<java.util.UUID, List<ActiveModifierKey>> ACTIVE_MODIFIER_STATES = new HashMap<>();

    private DungeonInteractionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().dimension() != DungeonCraft.DUNGEON_LEVEL) {
            return;
        }

        DungeonProgressData progress = DungeonProgressData.get(player.level().getServer());
        updateChoiceHud(player, progress);
        updateActiveModifierHud(player, progress);
        progress.enterRoom(player.getUUID(), player.blockPosition())
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

    private static void updateActiveModifierHud(ServerPlayer player, DungeonProgressData progress) {
        if (player.tickCount % 5 != 0) {
            return;
        }
        List<ActiveModifierKey> next = progress.activeModifiers(player.getUUID()).stream()
                .map(modifier -> new ActiveModifierKey(
                        modifier.id(), modifier.tier(), modifier.positive()))
                .toList();
        if (next.equals(ACTIVE_MODIFIER_STATES.get(player.getUUID()))) {
            return;
        }
        ACTIVE_MODIFIER_STATES.put(player.getUUID(), next);
        var payloadModifiers = next.stream()
                .map(modifier -> new ActiveModifiersHudPayload.ModifierEntry(
                        modifier.id(), modifier.tier(), modifier.positive()))
                .toList();
        PacketDistributor.sendToPlayer(player, new ActiveModifiersHudPayload(true, payloadModifiers));
    }

    private static void updateChoiceHud(ServerPlayer player, DungeonProgressData progress) {
        if (player.tickCount % 5 != 0) {
            return;
        }
        ChoiceHudKey current = VISIBLE_CHOICES.get(player.getUUID());
        double radiusSquared = current == null
                ? CHOICE_HUD_SHOW_DISTANCE_SQUARED
                : CHOICE_HUD_HIDE_DISTANCE_SQUARED;
        var nearby = progress.nearestFloorChoice(
                player.getUUID(), player.blockPosition(), radiusSquared);
        if (nearby.isEmpty()) {
            if (VISIBLE_CHOICES.remove(player.getUUID()) != null) {
                PacketDistributor.sendToPlayer(player, FloorChoiceHudPayload.hidden());
            }
            return;
        }

        var choice = nearby.orElseThrow();
        ChoiceHudKey next = new ChoiceHudKey(choice.floorNumber(), choice.choiceIndex());
        if (next.equals(current)) {
            return;
        }
        VISIBLE_CHOICES.put(player.getUUID(), next);
        var modifiers = choice.modifiers().stream()
                .map(modifier -> new FloorChoiceHudPayload.ModifierEntry(
                        modifier.id(), modifier.tier(), modifier.positive()))
                .toList();
        PacketDistributor.sendToPlayer(player, new FloorChoiceHudPayload(
                true, choice.choiceIndex(), choice.modifierScore(), modifiers));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().dimension() == DungeonCraft.DUNGEON_LEVEL) {
            DungeonProgressData.get(player.level().getServer()).endRun(player.getUUID());
            VISIBLE_CHOICES.remove(player.getUUID());
            ACTIVE_MODIFIER_STATES.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player, ActiveModifiersHudPayload.hidden());
            DungeonReturnData.get(player.level().getServer()).discardReturn(player.getUUID());
            return;
        }
        handleRoomEnemyRemoved(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player)
                || player.level().dimension() != DungeonCraft.DUNGEON_LEVEL
                || !event.getEntity().entityTags().contains("dungeoncraft_room_enemy")) {
            return;
        }
        var modifiers = DungeonProgressData.get(player.level().getServer())
                .activeModifiers(player.getUUID());
        double multiplier = DungeonModifierEffects.experienceMultiplier(modifiers);
        event.setDroppedExperience((int)Math.round(event.getOriginalExperience() * multiplier));
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
        VISIBLE_CHOICES.remove(player.getUUID());
        ACTIVE_MODIFIER_STATES.remove(player.getUUID());
        DungeonReturnData.get(player.level().getServer()).discardReturn(player.getUUID());
        TeleportTransition respawn = player.findRespawnPositionAndUseSpawnBlock(
                false, TeleportTransition.DO_NOTHING);
        player.teleport(respawn);
    }

    private record ChoiceHudKey(int floorNumber, int choiceIndex) {
    }

    private record ActiveModifierKey(String id, int tier, boolean positive) {
    }

    private static void handleRoomEnemyRemoved(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || level.dimension() != DungeonCraft.DUNGEON_LEVEL
                || !entity.entityTags().contains("dungeoncraft_room_enemy")) {
            return;
        }

        DungeonProgressData.get(level.getServer()).removeRoomEnemy(entity.getUUID()).ifPresent(cleared -> {
            DungeonRoomSealer.open(level, cleared.exitSealBlocks());
            for (var memberId : cleared.memberIds()) {
                ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberId);
                if (member != null) {
                    member.sendOverlayMessage(Component.translatable(
                            "message.dungeoncraft.room.cleared", cleared.roomId() + 1));
                }
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
            ServerLevel dungeonLevel = (ServerLevel)player.level();
            var nextModifiers = progress.modifiersAfterSelection(
                    player.getUUID(), action.modifiers());
            PrototypeDungeonGenerator.GeneratedFloor nextFloor =
                    PrototypeDungeonGenerator.generateNextFloor(
                            dungeonLevel, action.dungeonId(),
                            action.floorNumber() + 1, action.nextRoomId(),
                            action.choiceIndex(), action.floorNumber() + 1 >= action.floorCount(),
                            nextModifiers);
            if (!progress.advanceToFloor(
                    player.getUUID(), action.floorNumber(), nextFloor, action.modifiers())) {
                return;
            }
            BlockPos nextSpawn = nextFloor.spawn();
            player.teleportTo(dungeonLevel,
                    nextSpawn.getX() + 0.5, nextSpawn.getY(), nextSpawn.getZ() + 0.5,
                    Set.of(), player.getYRot(), player.getXRot(), true);
            player.sendOverlayMessage(Component.translatable(
                    "message.dungeoncraft.floor.choice_entered",
                    action.choiceIndex() + 1, action.floorNumber() + 1, action.floorCount()));
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
        VISIBLE_CHOICES.remove(player.getUUID());
        ACTIVE_MODIFIER_STATES.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, ActiveModifiersHudPayload.hidden());
        player.teleportTo(returnLevel, target.x(), target.y(), target.z(), Set.of(),
                player.getYRot(), player.getXRot(), true);
        player.sendOverlayMessage(Component.translatable(dungeonExit
                ? "message.dungeoncraft.dungeon.completed"
                : "message.dungeoncraft.returned"));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }
}
