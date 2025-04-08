// src/main/java/com/blocklogic/traveldirect/block/custom/NetherAnchorBlock.java
package com.blocklogic.traveldirect.block.custom;

import com.blocklogic.traveldirect.block.ModBlocks;
import com.blocklogic.traveldirect.util.TeleportationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;


public class NetherAnchorBlock extends Block {
    public NetherAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Only process on the server side
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Check if player is already in the Nether
        if (level.dimension() == Level.NETHER) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.already_in_nether")
                            .withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.SUCCESS;
        }

        // Store the current position before teleporting
        if (player instanceof ServerPlayer serverPlayer) {
            // Store the anchor position - this is where we'll return to
            TeleportationHelper.storeLastPosition(
                    serverPlayer,
                    Level.OVERWORLD,
                    pos.above() // Position above the anchor
            );

            teleportToNether(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void teleportToNether(ServerPlayer player) {
        ServerLevel netherLevel = player.getServer().getLevel(Level.NETHER);
        if (netherLevel != null) {
            // Calculate target position
            BlockPos targetPos = calculateSafeNetherPos(player, netherLevel);

            // Make sure the player won't suffocate
            while (!netherLevel.getBlockState(targetPos).isAir() ||
                    !netherLevel.getBlockState(targetPos.above()).isAir()) {
                targetPos = targetPos.above();
            }

            // Ensure there's a solid block below
            if (netherLevel.getBlockState(targetPos.below()).isAir()) {
                netherLevel.setBlock(targetPos.below(), net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState(), 3);
            }

            // Teleport the player
            player.teleportTo(netherLevel,
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot());

            // Give first-time teleport kit
            giveFirstTimeNetherKit(player);

            // Display success message
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.teleport_nether")
                            .withStyle(ChatFormatting.GOLD),
                    true);

            // Play teleport sound effect
            player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
        }
    }

    private BlockPos calculateSafeNetherPos(ServerPlayer player, ServerLevel netherLevel) {
        // Convert overworld coordinates to nether (divide by 8)
        BlockPos initialPos = new BlockPos(
                (int)(player.getX() / 8),
                (int)player.getY(),
                (int)(player.getZ() / 8));

        // Find a safe spot
        return findSafeSpot(netherLevel, initialPos);
    }

    private BlockPos findSafeSpot(ServerLevel level, BlockPos pos) {
        // Start from a reasonable Y coordinate in the Nether
        int startY = 70; // Good starting point for the Nether

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(pos.getX(), startY, pos.getZ());

        // Search in a spiral pattern to find a safe location without lava
        for (int radius = 0; radius < 16; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only check the outer shell of the cube for this radius
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    mutablePos.set(pos.getX() + x, startY, pos.getZ() + z);

                    // Now search vertically at this x,z coordinate
                    for (int y = startY; y > level.getMinBuildHeight() + 1; y--) {
                        mutablePos.setY(y);

                        // Check if we've found a safe spot
                        if (isSafeLocation(level, mutablePos)) {
                            // Position player on top of the block
                            return mutablePos.above().immutable();
                        }
                    }
                }
            }
        }

        // If we get here, we couldn't find a safe spot in the spiral search
        // Place a safe platform as a last resort
        mutablePos.set(pos.getX(), 70, pos.getZ());
        createSafePlatform(level, mutablePos);
        return mutablePos.above().immutable();
    }

    private boolean isSafeLocation(ServerLevel level, BlockPos pos) {
        // Check if the block below is solid
        if (level.getBlockState(pos).isAir() ||
                !level.getBlockState(pos).isSolid()) {
            return false;
        }

        // Check if there are 2 air blocks above for the player to stand
        if (!level.getBlockState(pos.above()).isAir() ||
                !level.getBlockState(pos.above(2)).isAir()) {
            return false;
        }

        // Check that we're not on top of lava
        if (level.getBlockState(pos).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.north()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.south()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.east()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.west()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA)) {
            return false;
        }

        // This location passes all safety checks
        return true;
    }

    private void createSafePlatform(ServerLevel level, BlockPos pos) {
        // Create a 3x3 obsidian platform to ensure safety
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(
                        pos.offset(x, 0, z),
                        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),
                        3
                );
            }
        }

        // Clear the area above the platform
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 1; y <= 2; y++) {
                    level.setBlock(
                            pos.offset(x, y, z),
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                            3
                    );
                }
            }
        }
    }

    private void giveFirstTimeNetherKit(ServerPlayer player) {
        // Check if the player has already received the kit
        // We'll store this as a tag in the player's persistent data
        CompoundTag persistentData = player.getPersistentData();

        // If they haven't received it yet
        if (!persistentData.contains("traveldirect.received_nether_kit")) {
            // Give the player an Overworld Anchor
            player.getInventory().add(new ItemStack(ModBlocks.OVERWORLD_ANCHOR.get()));

            // Display a message
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.first_time_kit_simple")
                            .withStyle(ChatFormatting.GOLD),
                    false);

            // Mark the player as having received the kit
            persistentData.putBoolean("traveldirect.received_nether_kit", true);
        }
    }
}