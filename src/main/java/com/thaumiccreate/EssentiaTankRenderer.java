package com.thaumiccreate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class EssentiaTankRenderer extends SafeBlockEntityRenderer<EssentiaTankBlockEntity> {
    public EssentiaTankRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void renderSafe(EssentiaTankBlockEntity tank, float partialTicks,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!tank.isController() || !tank.hasWindows() || tank.getFluidLevel() == null) return;
        var fluid=tank.getTankInventory().getFluid();
        if (fluid.isEmpty()) return;
        float fraction=net.minecraft.util.Mth.clamp(tank.getFluidLevel().getValue(partialTicks), 0f, 1f);
        float height=(tank.getHeight()-0.5625f)*fraction;
        if (height < 1f/512f) return;
        float inset=0.0703125f;
        EssentiaJarVisual.render(pose,buffers,EssentiaFluid.color(fluid),
            inset,0.25f,inset,tank.getWidth()-inset,0.25f+height,tank.getWidth()-inset);
    }
    @Override public boolean shouldRenderOffScreen(EssentiaTankBlockEntity tank) { return true; }
}
