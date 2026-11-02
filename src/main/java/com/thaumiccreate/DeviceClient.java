package com.thaumiccreate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

@net.neoforged.fml.common.EventBusSubscriber(modid = ThaumicCreate.ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class DeviceClient {
    public static IClientFluidTypeExtensions fluidExtensions() {
        return new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() { return ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still"); }
            public ResourceLocation getFlowingTexture() { return ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow"); }
            public int getTintColor(FluidStack stack) { return EssentiaFluid.color(stack); }
        };
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerFluidType(fluidExtensions(), DeviceRegistry.ESSENTIA_TYPE.get());
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DeviceRegistry.PUMP_ENTITY.get(), EssentiaPumpRenderer::new);
        event.registerBlockEntityRenderer(DeviceRegistry.ENGINE_ENTITY.get(), EssentiaEngineRenderer::new);
        event.registerBlockEntityRenderer(DeviceRegistry.ADAPTER_ENTITY.get(), EssentiaAdapterRenderer::new);
        event.registerBlockEntityRenderer(DeviceRegistry.TANK_ENTITY.get(), EssentiaTankRenderer::new);
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        TankConnectedTextures.register();
        EssentiaEngineRenderer.init();
        EssentiaPumpRenderer.init();
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.PUMP.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.ENGINE.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.TANK.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.ADAPTER.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
        });
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void registerEngineHud(net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            var hud = com.thaumicreborn.api.client.ThaumicRebornClientApi.hud();
            hud.registerAspectContainer(EssentiaEngineBlockEntity.class, (engine, hit) -> {
                var aspects=engine.storedAspects().entrySet().stream().map(e->new com.thaumicreborn.api.client.HudAspect(e.getKey(),(int)Math.ceil(e.getValue()))).toList();
                return aspects.isEmpty()?java.util.Optional.empty():java.util.Optional.of(new com.thaumicreborn.api.client.HudReadout(aspects,hud.onHitFace(hit)));
            });
            hud.registerAspectContainer(EssentiaAdapterBlockEntity.class, (adapter, hit) -> {
                var fluid = adapter.buffer.getFluid();
                String aspect = EssentiaFluid.aspect(fluid);
                if (aspect == null || fluid.isEmpty()) return java.util.Optional.empty();
                return java.util.Optional.of(new com.thaumicreborn.api.client.HudReadout(
                    java.util.List.of(new com.thaumicreborn.api.client.HudAspect(aspect,
                        (int)Math.ceil(fluid.getAmount() / (double)EssentiaFluid.UNIT))), hud.onHitFace(hit)));
            });
            hud.registerAspectContainer(EssentiaTankBlockEntity.class, (tank, hit) -> {
                var controller = tank.getControllerBE();
                if (controller == null) return java.util.Optional.empty();
                var fluid = controller.getTankInventory().getFluid();
                String aspect = EssentiaFluid.aspect(fluid);
                if (aspect == null || fluid.isEmpty()) return java.util.Optional.empty();
                return java.util.Optional.of(new com.thaumicreborn.api.client.HudReadout(
                    java.util.List.of(new com.thaumicreborn.api.client.HudAspect(aspect,
                        (int)Math.ceil(fluid.getAmount() / (double)EssentiaFluid.UNIT))), hud.onHitFace(hit)));
            });
        });
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void engineAspectColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> 0xFF876531, DeviceRegistry.ARBOR_ICON.get());
        event.register((stack, layer) -> 0xFF9700C0, DeviceRegistry.MAGIC_ICON.get());
    }
}
