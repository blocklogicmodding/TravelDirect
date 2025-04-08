// src/main/java/com/blocklogic/traveldirect/block/custom/EndAnchorBlock.java
package com.blocklogic.traveldirect.block.custom;

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

public class EndAnchorBlock extends Block {
    public EndAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Only process on the server side
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Check if player is already in the End
        if (level.dimension() == Level.END) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.already_in_end")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
            return InteractionResult.SUCCESS;
        }

        // Save current dimension for return
        if (player instanceof ServerPlayer serverPlayer) {
            ResourceKey<Level> currentDimension = serverPlayer.level().dimension();

            // Store the anchor position - this is where we'll return to
            TeleportationHelper.storeLastPosition(
                    serverPlayer,
                    currentDimension,
                    pos.above() // Position above the anchor
            );

            teleportToEnd(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void teleportToEnd(ServerPlayer player) {
        ServerLevel endLevel = player.getServer().getLevel(Level.END);
        if (endLevel != null) {
            // For the End, we'll teleport near the central island
            // This is similar to how vanilla Minecraft handles End teleportation
            BlockPos endSpawnPos = BlockPos.containing(100, 50, 0);

            // Find a safe spot near the End spawn
            BlockPos targetPos = findSafeSpot(endLevel, endSpawnPos);

            // Teleport the player
            player.teleportTo(endLevel,
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot());

            // Display success message
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.teleport_end")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);

            // Play teleport sound effect
            player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
        }
    }

    private BlockPos findSafeSpot(ServerLevel level, BlockPos pos) {
        // Start searching from the suggested position
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());

        // Search in a spiral pattern to find a safe location
        for (int radius = 0; radius < 16; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only check the outer shell of the cube for this radius
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    mutablePos.set(pos.getX() + x, pos.getY(), pos.getZ() + z);

                    // Search vertically at this x,z coordinate
                    for (int y = pos.getY(); y > level.getMinBuildHeight() + 1; y--) {
                        mutablePos.setY(y);

                        // Check if we've found a safe spot (solid block with air above)
                        if (!level.getBlockState(mutablePos).isAir() &&
                                level.getBlockState(mutablePos.above()).isAir() &&
                                level.getBlockState(mutablePos.above(2)).isAir()) {
                            // Position player on top of the block
                            return mutablePos.above().immutable();
                        }
                    }
                }
            }
        }

        // If we couldn't find a safe spot, create a small obsidian platform
        mutablePos.set(pos.getX(), pos.getY(), pos.getZ());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(
                        mutablePos.offset(x, -1, z),
                        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),
                        3
                );

                // Clear air space above
                level.setBlock(
                        mutablePos.offset(x, 0, z),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        3
                );
                level.setBlock(
                        mutablePos.offset(x, 1, z),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        3
                );
            }
        }

        return mutablePos.immutable();
    }
}