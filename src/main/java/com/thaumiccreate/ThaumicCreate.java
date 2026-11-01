package com.thaumiccreate;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ThaumicCreate.ID)
public final class ThaumicCreate {
    public static final String ID = "thaumic_create";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    static {
        ITEMS.registerSimpleItem("plate_thaumium", new Item.Properties());
        ITEMS.registerSimpleItem("plate_void", new Item.Properties());
    }
    public ThaumicCreate(IEventBus bus) { ITEMS.register(bus); DeviceRegistry.register(bus); net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PressEvents::placed); net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PressEvents::interact); }
}
