package com.blocklogic.traveldirect.block.custom;

import com.blocklogic.traveldirect.block.ModBlocks;
import com.blocklogic.traveldirect.util.TeleportationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level.dimension() == Level.NETHER) {
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.already_in_nether")
                            .withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            TeleportationHelper.storeLastPosition(
                    serverPlayer,
                    Level.OVERWORLD,
                    pos.above()
            );

            teleportToNether(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void teleportToNether(ServerPlayer player) {
        ServerLevel netherLevel = player.getServer().getLevel(Level.NETHER);
        if (netherLevel != null) {
            BlockPos lastNetherPos = TeleportationHelper.getLastPosition(player, Level.NETHER);

            if (lastNetherPos != null) {
                player.teleportTo(netherLevel,
                        lastNetherPos.getX() + 0.5,
                        lastNetherPos.getY(),
                        lastNetherPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            } else {
                BlockPos targetPos = calculateSafeNetherPos(player, netherLevel);

                while (!netherLevel.getBlockState(targetPos).isAir() ||
                        !netherLevel.getBlockState(targetPos.above()).isAir()) {
                    targetPos = targetPos.above();
                }

                if (netherLevel.getBlockState(targetPos.below()).isAir()) {
                    netherLevel.setBlock(targetPos.below(), net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState(), 3);
                }

                player.teleportTo(netherLevel,
                        targetPos.getX() + 0.5,
                        targetPos.getY(),
                        targetPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            }

            giveFirstTimeNetherKit(player);

            player.displayClientMessage(
                    Component.translatable("message.traveldirect.teleport_nether")
                            .withStyle(ChatFormatting.GOLD),
                    true);

            player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
        }
    }

    private BlockPos calculateSafeNetherPos(ServerPlayer player, ServerLevel netherLevel) {
        BlockPos initialPos = new BlockPos(
                (int)(player.getX() / 8),
                (int)player.getY(),
                (int)(player.getZ() / 8));

        return findSafeSpot(netherLevel, initialPos);
    }

    private BlockPos findSafeSpot(ServerLevel level, BlockPos pos) {
        int startY = 70;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(pos.getX(), startY, pos.getZ());

        for (int radius = 0; radius < 16; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {

                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    mutablePos.set(pos.getX() + x, startY, pos.getZ() + z);

                    for (int y = startY; y > level.getMinBuildHeight() + 1; y--) {
                        mutablePos.setY(y);

                        if (isSafeLocation(level, mutablePos)) {
                            return mutablePos.above().immutable();
                        }
                    }
                }
            }
        }

        mutablePos.set(pos.getX(), 70, pos.getZ());
        createSafePlatform(level, mutablePos);
        return mutablePos.above().immutable();
    }

    private boolean isSafeLocation(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir() ||
                !level.getBlockState(pos).isSolid()) {
            return false;
        }

        if (!level.getBlockState(pos.above()).isAir() ||
                !level.getBlockState(pos.above(2)).isAir()) {
            return false;
        }

        if (level.getBlockState(pos).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.north()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.south()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.east()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) ||
                level.getBlockState(pos.west()).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA)) {
            return false;
        }

        return true;
    }

    private void createSafePlatform(ServerLevel level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(
                        pos.offset(x, 0, z),
                        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),
                        3
                );
            }
        }

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
        CompoundTag persistentData = player.getPersistentData();

        if (!persistentData.contains("traveldirect.received_nether_kit")) {
            player.getInventory().add(new ItemStack(ModBlocks.OVERWORLD_ANCHOR.get()));

            // Display a message
            player.displayClientMessage(
                    Component.translatable("message.traveldirect.first_time_kit_simple")
                            .withStyle(ChatFormatting.GOLD),
                    false);

            persistentData.putBoolean("traveldirect.received_nether_kit", true);
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