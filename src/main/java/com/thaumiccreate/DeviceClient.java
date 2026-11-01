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
        event.registerBlockEntityRenderer(DeviceRegistry.ENGINE_ENTITY.get(), EssentiaEngineRenderer::new);
        event.registerBlockEntityRenderer(DeviceRegistry.TANK_ENTITY.get(), com.simibubi.create.content.fluids.tank.FluidTankRenderer::new);
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        TankConnectedTextures.register();
        EssentiaEngineRenderer.init();
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.ENGINE.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.TANK.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.ADAPTER.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
        });
    }
}
