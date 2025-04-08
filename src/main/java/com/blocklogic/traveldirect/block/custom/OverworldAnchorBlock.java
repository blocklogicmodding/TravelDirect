package com.blocklogic.traveldirect.block.custom;

import com.blocklogic.traveldirect.util.TeleportationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.dimension() == Level.OVERWORLD) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.already_in_overworld")
                            .withStyle(ChatFormatting.GREEN),
                    true);
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            TeleportationHelper.storeLastPosition(
                    serverPlayer,
                    Level.NETHER,
                    pos.above()
            );

            teleportToOverworld(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworldLevel = player.getServer().getLevel(Level.OVERWORLD);
        if (overworldLevel != null) {
            ResourceKey<Level> sourceDimension = player.level().dimension();

            BlockPos lastOverworldPos = TeleportationHelper.getLastPosition(player, Level.OVERWORLD);

            if (lastOverworldPos != null) {
                player.teleportTo(overworldLevel,
                        lastOverworldPos.getX() + 0.5,
                        lastOverworldPos.getY(),
                        lastOverworldPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            } else {
                BlockPos spawnPos = overworldLevel.getSharedSpawnPos();
                player.teleportTo(overworldLevel,
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            }

            player.displayClientMessage(
                    Component.translatable("message.traveldirect.teleport_overworld")
                            .withStyle(ChatFormatting.GREEN),
                    true);

            player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
        }
    }
}