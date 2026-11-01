package com.thaumiccreate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = ThaumicCreate.ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class DeviceClient {
    public static IClientFluidTypeExtensions fluidExtensions() {
        return new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() { return new ResourceLocation("minecraft", "block/water_still"); }
            public ResourceLocation getFlowingTexture() { return new ResourceLocation("minecraft", "block/water_flow"); }
            public int getTintColor(FluidStack stack) { return EssentiaFluid.color(stack); }
        };
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DeviceRegistry.TANK_ENTITY.get(), com.simibubi.create.content.fluids.tank.FluidTankRenderer::new);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        TankConnectedTextures.register();
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.ENGINE.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.TANK.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(DeviceRegistry.ADAPTER.get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
        });
    }
}
