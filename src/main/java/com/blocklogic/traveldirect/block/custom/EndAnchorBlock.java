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
            ResourceKey<Level> currentDimension = serverPlayer.level().dimension();

            TeleportationHelper.storeLastPosition(
                    serverPlayer,
                    currentDimension,
                    pos.above()
            );

            teleportToEnd(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void teleportToEnd(ServerPlayer player) {
        ServerLevel endLevel = player.getServer().getLevel(Level.END);
        if (endLevel != null) {
            BlockPos endSpawnPos = BlockPos.containing(100, 50, 0);

            BlockPos targetPos = findSafeSpot(endLevel, endSpawnPos);

            player.teleportTo(endLevel,
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot());

            player.displayClientMessage(
                    Component.translatable("message.traveldirect.teleport_end")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);

            player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
        }
    }

    private BlockPos findSafeSpot(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());

        for (int radius = 0; radius < 16; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    mutablePos.set(pos.getX() + x, pos.getY(), pos.getZ() + z);

                    for (int y = pos.getY(); y > level.getMinBuildHeight() + 1; y--) {
                        mutablePos.setY(y);

                        if (!level.getBlockState(mutablePos).isAir() &&
                                level.getBlockState(mutablePos.above()).isAir() &&
                                level.getBlockState(mutablePos.above(2)).isAir()) {
                            return mutablePos.above().immutable();
                        }
                    }
                }
            }
        }

        mutablePos.set(pos.getX(), pos.getY(), pos.getZ());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(
                        mutablePos.offset(x, -1, z),
                        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),
                        3
                );

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

    @Override
    public boolean canSurvive(BlockState state, LevelReader levelReader, BlockPos pos) {
        if (levelReader instanceof Level level) {
            if (level.dimension() == Level.NETHER) {
                return false;
            }
        }
        return super.canSurvive(state, levelReader, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide() && level.dimension() == Level.NETHER && placer instanceof Player player) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.cannot_place_end_in_nether")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
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

                level.addParticle(ParticleTypes.PORTAL, x, y, z, xSpeed, ySpeed, zSpeed);
            }
        }
    }

}