package com.blocklogic.traveldirect.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.material.Fluids;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportationHelper {
    private static final Map<UUID, BlockPos> returnPositions = new HashMap<>();

    public static void storeReturnPosition(ServerPlayer player, BlockPos position) {
        returnPositions.put(player.getUUID(), position);
    }

    public static BlockPos getReturnPosition(ServerPlayer player) {
        return returnPositions.get(player.getUUID());
    }

    public static boolean safelyTeleportToEnd(ServerPlayer player, ServerLevel endLevel) {
        BlockPos endSpawnPos = BlockPos.containing(100, 50, 0);

        BlockPos safePos = findSafeSpotNearPosition(endLevel, endSpawnPos);
        if (safePos == null) {
            safePos = createSafePlatform(endLevel, endSpawnPos);
        }

        player.teleportTo(endLevel,
                safePos.getX() + 0.5,
                safePos.getY(),
                safePos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot());
        return true;
    }

    public static BlockPos findSafeSpotNearPosition(ServerLevel level, BlockPos startPos) {
        // First check if the starting position is already safe
        if (isSafeTeleportLocation(level, startPos)) {
            return startPos;
        }

        for (int radius = 1; radius <= 10; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    BlockPos checkPos = startPos.offset(x, 0, z);

                    if (isSafeTeleportLocation(level, checkPos)) {
                        return checkPos;
                    }

                    for (int y = 1; y <= 3; y++) {
                        BlockPos upPos = checkPos.above(y);
                        if (isSafeTeleportLocation(level, upPos)) {
                            return upPos;
                        }
                    }

                    for (int y = 1; y <= 3; y++) {
                        BlockPos downPos = checkPos.below(y);
                        if (isSafeTeleportLocation(level, downPos)) {
                            return downPos;
                        }
                    }
                }
            }

            for (int y = -radius; y <= radius; y++) {
                if (y == 0) continue;

                BlockPos checkPos = startPos.above(y);
                if (isSafeTeleportLocation(level, checkPos)) {
                    return checkPos;
                }
            }
        }

        return null;
    }

    public static boolean isSafeTeleportLocation(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        BlockState blockBelow = level.getBlockState(pos.below());
        if (!blockBelow.isSolid() || blockBelow.getFluidState().is(Fluids.LAVA)) {
            return false;
        }

        if (level.getBlockState(pos.north()).getFluidState().is(Fluids.LAVA) ||
                level.getBlockState(pos.south()).getFluidState().is(Fluids.LAVA) ||
                level.getBlockState(pos.east()).getFluidState().is(Fluids.LAVA) ||
                level.getBlockState(pos.west()).getFluidState().is(Fluids.LAVA)) {
            return false;
        }

        AABB playerSpace = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
        return level.noCollision(playerSpace);
    }

    public static BlockPos createSafePlatform(ServerLevel level, BlockPos pos) {
        // Create a 3x3 obsidian platform
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(
                        pos.offset(x, -1, z),
                        Blocks.OBSIDIAN.defaultBlockState(),
                        3
                );
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(
                            pos.offset(x, y, z),
                            Blocks.AIR.defaultBlockState(),
                            3
                    );
                }
            }
        }

        return pos;
    }
}