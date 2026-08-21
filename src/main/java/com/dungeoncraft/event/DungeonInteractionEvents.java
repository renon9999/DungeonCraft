package com.dungeoncraft.event;

import com.dungeoncraft.DungeonCraft;
import com.dungeoncraft.dungeon.DungeonReturnData;
import java.util.Set;
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

public final class DungeonInteractionEvents {
    private DungeonInteractionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().dimension() != DungeonCraft.DUNGEON_LEVEL
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.LEVER)) {
            return;
        }

        DungeonReturnData data = DungeonReturnData.get(player.level().getServer());
        data.takeReturn(player.getUUID(), event.getPos()).ifPresent(target -> {
            ResourceKey<Level> returnKey = ResourceKey.create(
                    Registries.DIMENSION, Identifier.parse(target.dimension()));
            ServerLevel returnLevel = player.level().getServer().getLevel(returnKey);
            if (returnLevel == null) {
                returnLevel = player.level().getServer().overworld();
            }
            player.teleportTo(returnLevel, target.x(), target.y(), target.z(), Set.of(),
                    player.getYRot(), player.getXRot(), true);
            player.sendOverlayMessage(Component.translatable("message.dungeoncraft.returned"));
        });

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }
}
