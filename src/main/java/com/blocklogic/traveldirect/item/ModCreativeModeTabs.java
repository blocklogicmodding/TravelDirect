package com.blocklogic.traveldirect.item;

import com.blocklogic.traveldirect.TravelDirect;
import com.blocklogic.traveldirect.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TravelDirect.MODID);

    public static final Supplier<CreativeModeTab> TRAVEL_DIRECT = CREATIVE_MODE_TAB.register("travel_direct",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.END_ANCHOR.get()))
                    .title(Component.translatable("creativetab.traveldirect.travel_direct"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.END_ANCHOR);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}