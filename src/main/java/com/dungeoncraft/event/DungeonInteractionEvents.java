package com.dungeoncraft.event;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.dungeon.DungeonReturnData;
import com.dungeoncraft.dungeon.DungeonProgressData;
import com.dungeoncraft.dungeon.DungeonCleanupManager;
import com.dungeoncraft.dungeon.DungeonModifierEffects;
import com.dungeoncraft.dungeon.DungeonRoomEnemySpawner;
import com.dungeoncraft.dungeon.DungeonRoomSealer;
import com.dungeoncraft.dungeon.PrototypeDungeonGenerator;
import com.dungeoncraft.network.ActiveModifiersHudPayload;
import com.dungeoncraft.network.FloorChoiceHudPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
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
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DungeonInteractionEvents {
    private static final double CHOICE_HUD_SHOW_DISTANCE_SQUARED = 16.0;
    private static final double CHOICE_HUD_HIDE_DISTANCE_SQUARED = 25.0;
    private static final Map<java.util.UUID, ChoiceHudKey> VISIBLE_CHOICES = new HashMap<>();
    private static final Map<java.util.UUID, ActiveHudKey> ACTIVE_MODIFIER_STATES = new HashMap<>();
    private static final List<BlockPos> PARTY_SPAWN_OFFSETS = List.of(
            BlockPos.ZERO,
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1), new BlockPos(-1, 0, 1),
            new BlockPos(1, 0, -1), new BlockPos(-1, 0, -1));

    private DungeonInteractionEvents() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().dimension() == DungeonCraft.DUNGEON_LEVEL) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().dimension() != DungeonCraft.DUNGEON_LEVEL) {
            return;
        }

        DungeonProgressData progress = DungeonProgressData.get(player.level().getServer());
        if (player.tickCount % 20 == 0) {
            player.causeFoodExhaustion((float)DungeonModifierEffects.hungerExhaustionPerSecond(
                    progress.activeModifiers(player.getUUID())));
        }
        updateChoiceHud(player, progress);
        updateActiveModifierHud(player, progress);
        progress.enterRoom(player.getUUID(), player.blockPosition())
                .ifPresent(room -> {
                    if (room.isCombat()) {
                        DungeonRoomEnemySpawner.SpawnedEnemies enemies =
                                DungeonRoomEnemySpawner.spawn(
                                        (ServerLevel)player.level(), room,
                                        progress.activeModifiers(player.getUUID()));
                        DungeonProgressData.get(player.level().getServer())
                                .startRoomCombat(player.getUUID(), room.roomId(), enemies.enemyIds());
                        if (enemies.enemyCount() == 0) {
                            DungeonRoomSealer.open((ServerLevel)player.level(),
                                    DungeonProgressData.get(player.level().getServer())
                                            .clearRoom(player.getUUID(), room.roomId()));
                        }
                    } else {
                        DungeonRoomSealer.open((ServerLevel)player.level(), room.exitSealBlocks());
                    }
                });
    }

    private static void updateActiveModifierHud(ServerPlayer player, DungeonProgressData progress) {
        if (player.tickCount % 5 != 0) {
            return;
        }
        List<ActiveModifierKey> modifiers = progress.activeModifiers(player.getUUID()).stream()
                .map(modifier -> new ActiveModifierKey(
                        modifier.id(), modifier.tier(), modifier.positive()))
                .toList();
        var floorProgress = progress.floorProgress(player.getUUID());
        if (floorProgress.isEmpty()) {
            if (ACTIVE_MODIFIER_STATES.remove(player.getUUID()) != null) {
                PacketDistributor.sendToPlayer(player, ActiveModifiersHudPayload.hidden());
            }
            return;
        }
        DungeonProgressData.FloorProgress floor = floorProgress.orElseThrow();
        ActiveHudKey next = new ActiveHudKey(floor.currentFloor(), floor.floorCount(), modifiers);
        if (next.equals(ACTIVE_MODIFIER_STATES.get(player.getUUID()))) {
            return;
        }
        ACTIVE_MODIFIER_STATES.put(player.getUUID(), next);
        var payloadModifiers = modifiers.stream()
                .map(modifier -> new ActiveModifiersHudPayload.ModifierEntry(
                        modifier.id(), modifier.tier(), modifier.positive()))
                .toList();
        PacketDistributor.sendToPlayer(player, new ActiveModifiersHudPayload(
                true, floor.currentFloor(), floor.floorCount(), payloadModifiers));
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
            eliminatePartyMember(player, false);
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

        eliminatePartyMember(player, true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        DungeonCleanupManager.tick(event.getServer());
    }

    private record ChoiceHudKey(int floorNumber, int choiceIndex) {
    }

    private record ActiveModifierKey(String id, int tier, boolean positive) {
    }

    private record ActiveHudKey(
            int currentFloor, int floorCount, List<ActiveModifierKey> modifiers) {
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
            int nextFloorNumber = action.floorNumber() + 1;
            if (!DungeonCleanupManager.isFloorSlotReady(action.dungeonId(), nextFloorNumber)) {
                player.sendOverlayMessage(Component.translatable(
                        "message.dungeoncraft.floor.cleanup_pending"));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
                return;
            }
            long instanceSlot = DungeonCleanupManager.instanceSlot(action.dungeonId());
            if (instanceSlot < 0L) {
                return;
            }
            var nextModifiers = progress.modifiersAfterSelection(
                    player.getUUID(), action.modifiers());
            PrototypeDungeonGenerator.GeneratedFloor nextFloor =
                    PrototypeDungeonGenerator.generateNextFloor(
                            dungeonLevel, action.dungeonId(), instanceSlot,
                            nextFloorNumber, action.nextRoomId(),
                            action.choiceIndex(), nextFloorNumber >= action.floorCount(),
                            nextModifiers);
            if (!progress.advanceToFloor(
                    player.getUUID(), action.floorNumber(), nextFloor, action.modifiers())) {
                return;
            }
            BlockPos nextSpawn = nextFloor.spawn();
            teleportPartyToFloor(
                    player, progress.partyMemberIds(player.getUUID()), dungeonLevel, nextSpawn,
                    Component.translatable(
                            "message.dungeoncraft.floor.choice_entered",
                            action.choiceIndex() + 1, action.floorNumber() + 1, action.floorCount()));
            DungeonCleanupManager.transitionToFloor(
                    action.dungeonId(), nextFloorNumber,
                    nextFloor.cleanupRegions());
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
            return;
        }

        boolean dungeonExit = floorExit.isPresent();
        DungeonReturnData data = DungeonReturnData.get(player.level().getServer());
        if (!dungeonExit && !data.matchesReturnSwitch(player.getUUID(), event.getPos())) {
            return;
        }

        returnParty(player, progress, data, dungeonExit);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }

    private static void teleportPartyToFloor(
            ServerPlayer activatingPlayer, List<UUID> memberIds,
            ServerLevel dungeonLevel, BlockPos spawn, Component message) {
        MinecraftServer server = activatingPlayer.level().getServer();
        for (int index = 0; index < memberIds.size(); index++) {
            UUID memberId = memberIds.get(index);
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member == null) {
                continue;
            }
            clearClientHud(member);
            BlockPos offset = PARTY_SPAWN_OFFSETS.get(Math.min(index, PARTY_SPAWN_OFFSETS.size() - 1));
            member.teleportTo(
                    dungeonLevel,
                    spawn.getX() + offset.getX() + 0.5,
                    spawn.getY(),
                    spawn.getZ() + offset.getZ() + 0.5,
                    Set.of(), member.getYRot(), member.getXRot(), true);
            member.sendOverlayMessage(message);
        }
    }

    private static void returnParty(
            ServerPlayer activatingPlayer, DungeonProgressData progress,
            DungeonReturnData returnData, boolean completed) {
        MinecraftServer server = activatingPlayer.level().getServer();
        List<UUID> memberIds = progress.partyMemberIds(activatingPlayer.getUUID());
        if (memberIds.isEmpty()) {
            return;
        }
        Map<UUID, Optional<DungeonReturnData.ReturnTarget>> targets = new HashMap<>();
        memberIds.forEach(memberId -> targets.put(memberId, returnData.takeReturn(memberId)));
        long dungeonId = progress.activeDungeonId(activatingPlayer.getUUID()).orElse(-1L);
        progress.endRun(activatingPlayer.getUUID());
        for (UUID memberId : memberIds) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member == null) {
                continue;
            }
            clearClientHud(member);
            Optional<DungeonReturnData.ReturnTarget> returnTarget = targets.get(memberId);
            if (returnTarget != null && returnTarget.isPresent()) {
                teleportToReturn(server, member, returnTarget.orElseThrow());
            } else {
                teleportToRespawn(member);
            }
            member.sendOverlayMessage(Component.translatable(completed
                    ? "message.dungeoncraft.dungeon.completed"
                    : "message.dungeoncraft.returned"));
        }
        ServerLevel dungeonLevel = server.getLevel(DungeonCraft.DUNGEON_LEVEL);
        if (dungeonLevel != null && dungeonId >= 0L) {
            DungeonCleanupManager.retireDungeon(dungeonId);
        }
    }

    private static void eliminatePartyMember(
            ServerPlayer eliminatedPlayer, boolean teleportToSpawn) {
        MinecraftServer server = eliminatedPlayer.level().getServer();
        DungeonProgressData progress = DungeonProgressData.get(server);
        var departure = progress.removePartyMember(eliminatedPlayer.getUUID());
        if (departure.isEmpty()) {
            return;
        }
        DungeonReturnData returnData = DungeonReturnData.get(server);
        returnData.discardReturn(eliminatedPlayer.getUUID());
        clearClientHud(eliminatedPlayer);
        if (teleportToSpawn) {
            teleportToRespawn(eliminatedPlayer);
        }
        DungeonProgressData.PartyDeparture result = departure.orElseThrow();
        for (UUID memberId : result.remainingMemberIds()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendOverlayMessage(Component.translatable(
                        "message.dungeoncraft.party.member_eliminated",
                        eliminatedPlayer.getScoreboardName()));
            }
        }
        if (result.abandoned()) {
            ServerLevel dungeonLevel = server.getLevel(DungeonCraft.DUNGEON_LEVEL);
            if (dungeonLevel != null) {
                DungeonCleanupManager.retireDungeon(result.dungeonId());
            }
        }
    }

    private static void teleportToReturn(
            MinecraftServer server, ServerPlayer player, DungeonReturnData.ReturnTarget target) {
        ResourceKey<Level> returnKey = ResourceKey.create(
                Registries.DIMENSION, Identifier.parse(target.dimension()));
        ServerLevel returnLevel = server.getLevel(returnKey);
        if (returnLevel == null) {
            returnLevel = server.overworld();
        }
        player.teleportTo(returnLevel, target.x(), target.y(), target.z(), Set.of(),
                player.getYRot(), player.getXRot(), true);
    }

    private static void teleportToRespawn(ServerPlayer player) {
        TeleportTransition respawn = player.findRespawnPositionAndUseSpawnBlock(
                false, TeleportTransition.DO_NOTHING);
        player.teleport(respawn);
    }

    private static void clearClientHud(ServerPlayer player) {
        VISIBLE_CHOICES.remove(player.getUUID());
        ACTIVE_MODIFIER_STATES.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, FloorChoiceHudPayload.hidden());
        PacketDistributor.sendToPlayer(player, ActiveModifiersHudPayload.hidden());
    }
}
