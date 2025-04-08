// src/main/java/com/blocklogic/traveldirect/util/TeleportationHelper.java
// Simplified version that stores player-specific dimension positions

package com.blocklogic.traveldirect.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportationHelper {
    // Store last teleport positions for each player
    // Map of: Player UUID -> (Dimension -> Last Position)
    private static final Map<UUID, Map<ResourceKey<Level>, BlockPos>> lastTeleportPositions = new HashMap<>();

    // Store player's entry position for a dimension
    public static void storeLastPosition(ServerPlayer player, ResourceKey<Level> sourceDimension, BlockPos position) {
        UUID playerUUID = player.getUUID();

        // Initialize the dimension map if needed
        if (!lastTeleportPositions.containsKey(playerUUID)) {
            lastTeleportPositions.put(playerUUID, new HashMap<>());
        }

        // Store the position for this dimension
        lastTeleportPositions.get(playerUUID).put(sourceDimension, position);
    }

    // Get the last position in a dimension for a player
    public static BlockPos getLastPosition(ServerPlayer player, ResourceKey<Level> dimension) {
        UUID playerUUID = player.getUUID();

        if (lastTeleportPositions.containsKey(playerUUID)) {
            Map<ResourceKey<Level>, BlockPos> dimensionMap = lastTeleportPositions.get(playerUUID);
            if (dimensionMap.containsKey(dimension)) {
                return dimensionMap.get(dimension);
            }
        }

        return null; // No stored position
    }
}