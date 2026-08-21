package com.dungeoncraft.dungeon;

import com.dungeoncraft.DungeonCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persistent return points used by the temporary entrance-room test switch. */
public final class DungeonReturnData extends SavedData {
    private static final Codec<ReturnTarget> TARGET_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(target -> target.playerId().toString()),
            Codec.STRING.fieldOf("dimension").forGetter(ReturnTarget::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(ReturnTarget::x),
            Codec.DOUBLE.fieldOf("y").forGetter(ReturnTarget::y),
            Codec.DOUBLE.fieldOf("z").forGetter(ReturnTarget::z),
            Codec.INT.fieldOf("switch_x").forGetter(target -> target.switchPos().getX()),
            Codec.INT.fieldOf("switch_y").forGetter(target -> target.switchPos().getY()),
            Codec.INT.fieldOf("switch_z").forGetter(target -> target.switchPos().getZ())
    ).apply(instance, (player, dimension, x, y, z, switchX, switchY, switchZ) ->
            new ReturnTarget(UUID.fromString(player), dimension, x, y, z,
                    new BlockPos(switchX, switchY, switchZ))));

    private static final Codec<DungeonReturnData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TARGET_CODEC.listOf().optionalFieldOf("targets", List.of()).forGetter(DungeonReturnData::targets)
    ).apply(instance, DungeonReturnData::new));

    private static final SavedDataType<DungeonReturnData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DungeonCraft.MOD_ID, "dungeon_returns"),
            DungeonReturnData::new,
            CODEC);

    private final Map<UUID, ReturnTarget> targets = new HashMap<>();

    public DungeonReturnData() {
        this(List.of());
    }

    private DungeonReturnData(List<ReturnTarget> savedTargets) {
        savedTargets.forEach(target -> targets.put(target.playerId(), target));
    }

    public static DungeonReturnData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void recordReturn(ServerPlayer player, BlockPos switchPos) {
        ReturnTarget target = new ReturnTarget(
                player.getUUID(),
                player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ(),
                switchPos.immutable());
        targets.put(player.getUUID(), target);
        setDirty();
    }

    public Optional<ReturnTarget> takeReturn(UUID playerId, BlockPos usedSwitch) {
        ReturnTarget target = targets.get(playerId);
        if (target == null || !target.switchPos().equals(usedSwitch)) {
            return Optional.empty();
        }
        targets.remove(playerId);
        setDirty();
        return Optional.of(target);
    }

    private List<ReturnTarget> targets() {
        return List.copyOf(targets.values());
    }

    public record ReturnTarget(
            UUID playerId, String dimension, double x, double y, double z, BlockPos switchPos) {
    }
}
