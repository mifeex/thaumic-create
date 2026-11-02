package com.thaumiccreate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumicCreate.ID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thaumic_create"))
                .icon(() -> DeviceRegistry.ENGINE_ITEM.get().getDefaultInstance())
                .displayItems((parameters, output) -> BuiltInRegistries.ITEM.stream()
                        .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(ThaumicCreate.ID))
                        .filter(item -> item != DeviceRegistry.ARBOR_ICON.get() && item != DeviceRegistry.MAGIC_ICON.get())
                        .forEach(output::accept))
                .build());
    }

    private ModCreativeTabs() {}

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        TABS.register(bus);
    }
}
