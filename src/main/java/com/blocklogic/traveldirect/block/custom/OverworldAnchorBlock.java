package com.blocklogic.traveldirect.block.custom;

import com.blocklogic.traveldirect.util.TeleportationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    @Override
    public boolean canSurvive(BlockState state, LevelReader levelReader, BlockPos pos) {
        if (levelReader instanceof Level level) {
            if (level.dimension() == Level.END) {
                return false;
            }
        }
        return super.canSurvive(state, levelReader, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide() && level.dimension() == Level.END && placer instanceof Player player) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.cannot_place_overworld_in_end")
                            .withStyle(ChatFormatting.GREEN),
                    true);
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

                level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, xSpeed, ySpeed, zSpeed);
            }
        }
    }
}