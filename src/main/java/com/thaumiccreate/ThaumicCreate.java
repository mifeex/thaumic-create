package com.thaumiccreate;

import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(ThaumicCreate.ID)
public final class ThaumicCreate {
    public static final String ID = "thaumic_create";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    static {
        ITEMS.register("plate_thaumium", () -> new Item(new Item.Properties()));
        ITEMS.register("plate_void", () -> new Item(new Item.Properties()));
    }
    public ThaumicCreate() { ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus()); DeviceRegistry.register(FMLJavaModLoadingContext.get().getModEventBus()); ModCreativeTabs.register(FMLJavaModLoadingContext.get().getModEventBus()); net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(PressEvents::placed); net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(PressEvents::interact); }
}
