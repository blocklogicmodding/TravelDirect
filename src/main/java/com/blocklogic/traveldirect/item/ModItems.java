package com.blocklogic.traveldirect.item;

import com.blocklogic.traveldirect.TravelDirect;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TravelDirect.MODID);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
