package com.dungeoncraft.block;

import com.dungeoncraft.DungeonCraft;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DCPortalBlock extends Block {
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 7.0);

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

        ServerLevel dungeon = serverPlayer.level().getServer().getLevel(DungeonCraft.DUNGEON_LEVEL);
        if (dungeon == null) {
            DungeonCraft.LOGGER.error("Dungeon dimension {} is not available", DungeonCraft.DUNGEON_LEVEL.identifier());
            serverPlayer.sendOverlayMessage(
                    Component.translatable("message.dungeoncraft.portal.dimension_missing"));
            return InteractionResult.SUCCESS_SERVER;
        }

        createTestPlatform(dungeon);
        serverPlayer.teleportTo(dungeon, 0.5, 65.0, 0.5, Set.of(),
                serverPlayer.getYRot(), serverPlayer.getXRot(), true);
        serverPlayer.sendOverlayMessage(
                Component.translatable("message.dungeoncraft.portal.entered"));
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void createTestPlatform(ServerLevel level) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlock(new BlockPos(x, 64, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
                for (int y = 65; y <= 68; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
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
