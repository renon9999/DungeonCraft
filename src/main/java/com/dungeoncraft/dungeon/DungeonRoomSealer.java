package com.dungeoncraft.dungeon;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Places and removes the bedrock walls controlling progression from a room. */
public final class DungeonRoomSealer {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;

    private DungeonRoomSealer() {
    }

    public static void close(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos position : positions) {
            level.setBlock(position, Blocks.BEDROCK.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    public static void open(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos position : positions) {
            if (level.getBlockState(position).is(Blocks.BEDROCK)) {
                level.setBlock(position, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }
        }
    }
}
