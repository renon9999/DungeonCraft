package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.config.DungeonGenerationConfig;
import com.dungeoncraft.dungeon.DungeonSequenceData;
import com.dungeoncraft.dungeon.DungeonProgressData;
import com.dungeoncraft.dungeon.DungeonReturnData;
import com.dungeoncraft.dungeon.DungeonCleanupManager;
import com.dungeoncraft.dungeon.PrototypeDungeonGenerator;
import com.dungeoncraft.network.OpenFloorSelectionPayload;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DCPortalBlock extends Block {
    private static final Set<Integer> ALLOWED_FLOOR_COUNTS = Set.of(6, 10, 20, 40);
    private static final double SELECTION_DISTANCE_SQUARED = 64.0;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 7.0);
    private static final List<BlockPos> PARTY_SPAWN_OFFSETS = List.of(
            BlockPos.ZERO,
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1), new BlockPos(-1, 0, 1),
            new BlockPos(1, 0, -1), new BlockPos(-1, 0, -1));

    public DCPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (serverPlayer.level().dimension() != Level.OVERWORLD) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.dungeoncraft.portal.overworld_only"));
            return InteractionResult.SUCCESS_SERVER;
        }

        PacketDistributor.sendToPlayer(serverPlayer, new OpenFloorSelectionPayload(pos));
        return InteractionResult.SUCCESS_SERVER;
    }

    public static void startSelectedDungeon(
            ServerPlayer serverPlayer, BlockPos portalPos, int floorCount) {
        if (!ALLOWED_FLOOR_COUNTS.contains(floorCount)
                || serverPlayer.level().dimension() != Level.OVERWORLD
                || serverPlayer.distanceToSqr(
                        portalPos.getX() + 0.5, portalPos.getY() + 0.5, portalPos.getZ() + 0.5)
                        > SELECTION_DISTANCE_SQUARED
                || !serverPlayer.level().getBlockState(portalPos).is(DungeonCraft.DC_PORTAL.get())) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.dungeoncraft.portal.selection_invalid"));
            return;
        }
        ServerLevel dungeon = serverPlayer.level().getServer().getLevel(DungeonCraft.DUNGEON_LEVEL);
        if (dungeon == null) {
            DungeonCraft.LOGGER.error("Dungeon dimension {} is not available", DungeonCraft.DUNGEON_LEVEL.identifier());
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.dungeoncraft.portal.dimension_missing"));
            return;
        }

        DungeonProgressData progress = DungeonProgressData.get(serverPlayer.level().getServer());
        if (progress.hasActiveRun(serverPlayer.getUUID())) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.dungeoncraft.portal.already_active"));
            return;
        }
        double joinRadius = DungeonGenerationConfig.PARTY_JOIN_RADIUS.get();
        double joinRadiusSquared = joinRadius * joinRadius;
        double portalX = portalPos.getX() + 0.5;
        double portalY = portalPos.getY() + 0.5;
        double portalZ = portalPos.getZ() + 0.5;
        List<ServerPlayer> party = ((ServerLevel)serverPlayer.level()).players().stream()
                .filter(candidate -> candidate == serverPlayer
                        || (candidate.isAlive() && !progress.hasActiveRun(candidate.getUUID())))
                .filter(candidate -> candidate.distanceToSqr(portalX, portalY, portalZ) <= joinRadiusSquared)
                .sorted(Comparator
                        .comparingInt((ServerPlayer candidate) -> candidate == serverPlayer ? 0 : 1)
                        .thenComparingDouble(candidate -> candidate.distanceToSqr(portalX, portalY, portalZ))
                        .thenComparing(candidate -> candidate.getUUID().toString()))
                .limit(DungeonGenerationConfig.MAX_PARTY_SIZE.getAsInt())
                .toList();

        long dungeonId = DungeonSequenceData.get(serverPlayer.level().getServer()).allocateDungeonId();
        long instanceSlot = DungeonCleanupManager.allocateInstance(dungeonId);
        PrototypeDungeonGenerator.GeneratedDungeon generated =
                PrototypeDungeonGenerator.generate(dungeon, dungeonId, instanceSlot, floorCount);
        DungeonCleanupManager.registerInitialFloor(
                dungeonId, 1, generated.cleanupRegions());
        DungeonReturnData returnData = DungeonReturnData.get(serverPlayer.level().getServer());
        party.forEach(member -> returnData.recordReturn(member, generated.returnSwitch()));
        progress.startRun(serverPlayer, party, generated);
        BlockPos spawn = generated.spawn();
        for (int index = 0; index < party.size(); index++) {
            ServerPlayer member = party.get(index);
            BlockPos offset = PARTY_SPAWN_OFFSETS.get(index);
            member.teleportTo(
                    dungeon,
                    spawn.getX() + offset.getX() + 0.5,
                    spawn.getY(),
                    spawn.getZ() + offset.getZ() + 0.5,
                    Set.of(), member.getYRot(), member.getXRot(), true);
            member.sendOverlayMessage(
                    Component.translatable("message.dungeoncraft.portal.entered",
                            dungeonId, generated.floorCount()));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    0.0, 0.05, 0.0);
        }
    }
}
