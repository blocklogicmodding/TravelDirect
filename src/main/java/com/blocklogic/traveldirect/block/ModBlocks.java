package com.blocklogic.traveldirect.block;

import com.blocklogic.traveldirect.TravelDirect;
import com.blocklogic.traveldirect.block.custom.EndAnchorBlock;
import com.blocklogic.traveldirect.block.custom.NetherAnchorBlock;
import com.blocklogic.traveldirect.block.custom.OverworldAnchorBlock;
import com.blocklogic.traveldirect.item.ModItems;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TravelDirect.MODID);

    public static final DeferredBlock<Block> OVERWORLD_ANCHOR = registerBlock("overworld_anchor",
            () -> new OverworldAnchorBlock(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.LODESTONE)){
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    if(Screen.hasShiftDown()) {
                        tooltipComponents.add(Component.translatable("tooltip.traveldirect.overworld_anchor.shift_down"));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.traveldirect.overworld_anchor"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });

    public static final DeferredBlock<Block> NETHER_ANCHOR = registerBlock("nether_anchor",
            () -> new NetherAnchorBlock(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.LODESTONE)));

    public static final DeferredBlock<Block> END_ANCHOR = registerBlock("end_anchor",
            () -> new EndAnchorBlock(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.LODESTONE)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
