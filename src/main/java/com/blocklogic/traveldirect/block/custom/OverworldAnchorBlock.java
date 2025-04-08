// src/main/java/com/blocklogic/traveldirect/block/custom/OverworldAnchorBlock.java
package com.blocklogic.traveldirect.block.custom;

import com.blocklogic.traveldirect.block.ModBlocks;
import com.blocklogic.traveldirect.util.TeleportationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class OverworldAnchorBlock extends Block {
    public OverworldAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Only process on the server side
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Check if player is already in the Overworld
        if (level.dimension() == Level.OVERWORLD) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.already_in_overworld")
                            .withStyle(ChatFormatting.GREEN),
                    true);
            return InteractionResult.SUCCESS;
        }

        // Teleport player to Overworld
        if (player instanceof ServerPlayer serverPlayer) {
            teleportToOverworld(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworldLevel = player.getServer().getLevel(Level.OVERWORLD);
        if (overworldLevel != null) {
            // Get the current dimension the player is in
            ResourceKey<Level> sourceDimension = player.level().dimension();

            // Try to get the last position from this dimension
            BlockPos lastOverworldPos = TeleportationHelper.getLastPosition(player, Level.OVERWORLD);

            if (lastOverworldPos != null) {
                // We have a stored position, teleport there
                player.teleportTo(overworldLevel,
                        lastOverworldPos.getX() + 0.5,
                        lastOverworldPos.getY(),
                        lastOverworldPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            } else {
                // No stored position, fall back to spawn
                BlockPos spawnPos = overworldLevel.getSharedSpawnPos();
                player.teleportTo(overworldLevel,
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            }

            // Display success message
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.teleport_overworld")
                            .withStyle(ChatFormatting.GREEN),
                    true);

            // Play teleport sound effect
            player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
        }
    }
}