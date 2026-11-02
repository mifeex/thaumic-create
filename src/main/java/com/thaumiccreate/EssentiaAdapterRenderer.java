package com.thaumiccreate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Displays the aspect-coloured fluid behind the two side windows. */
public final class EssentiaAdapterRenderer extends SafeBlockEntityRenderer<EssentiaAdapterBlockEntity> {
    public EssentiaAdapterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override protected void renderSafe(EssentiaAdapterBlockEntity adapter, float partialTicks,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var fluid = adapter.buffer.getFluid();
        if (fluid.isEmpty()) return;
        float fraction = Math.min(1f, fluid.getAmount() / (float) EssentiaAdapterBlockEntity.CAPACITY);
        pose.pushPose();
        // The model's windows are on X for north/south and on Z for east/west.
        if (adapter.front().getAxis() == net.minecraft.core.Direction.Axis.X) {
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
            pose.translate(-0.5, 0, -0.5);
        }
        EssentiaJarVisual.render(pose, buffers, EssentiaFluid.color(fluid),
            0.065f, 0.251f, 0.255f, 0.935f, 0.251f + 0.498f * fraction, 0.745f);
        pose.popPose();
    }
}
