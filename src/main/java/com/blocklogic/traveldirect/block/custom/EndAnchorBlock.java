package com.blocklogic.traveldirect.block.custom;

import com.blocklogic.traveldirect.util.TeleportationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class EndAnchorBlock extends Block {
    public EndAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.dimension() == Level.END) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.already_in_end")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            // Store the current position as the return position
            ServerLevel currentLevel = serverPlayer.serverLevel();
            BlockPos safePos = TeleportationHelper.findSafeSpotNearPosition(currentLevel, pos);
            if (safePos == null) {
                safePos = pos.above();
            }

            TeleportationHelper.storeReturnPosition(serverPlayer, safePos);
            teleportToEnd(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void teleportToEnd(ServerPlayer player) {
        ServerLevel endLevel = player.getServer().getLevel(Level.END);
        if (endLevel != null) {
            TeleportationHelper.safelyTeleportToEnd(player, endLevel);

            player.displayClientMessage(
                    Component.translatable("message.traveldirect.teleport_end")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);

            player.playNotifySound(
                    SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) < 40) {
            for (int i = 0; i < random.nextInt(3) + 2; i++) {
                double xOffset = random.nextDouble();
                double zOffset = random.nextDouble();

                if (random.nextBoolean()) {
                    xOffset = xOffset < 0.5 ? 0.1 : 0.9;
                } else {
                    zOffset = zOffset < 0.5 ? 0.1 : 0.9;
                }

                double x = pos.getX() + xOffset;
                double y = pos.getY() + 0.5D + random.nextDouble() * 0.5D;
                double z = pos.getZ() + zOffset;

                double xSpeed = 0;
                double ySpeed = random.nextDouble() * 0.1D;
                double zSpeed = 0;

                level.addParticle(ParticleTypes.PORTAL, x, y, z, xSpeed, ySpeed, zSpeed);
            }
        }
    }
}