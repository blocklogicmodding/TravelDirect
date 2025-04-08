package com.blocklogic.traveldirect.item;

import com.blocklogic.traveldirect.TravelDirect;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TravelDirect.MODID);

    public static final DeferredItem<Item> OVERWORLD_ACTIVATOR = ITEMS.register("overworld_activator",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> NETHER_ACTIVATOR = ITEMS.register("nether_activator",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> END_ACTIVATOR = ITEMS.register("end_activator",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ACTIVATOR_POUCH = ITEMS.register("activator_pouch",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
