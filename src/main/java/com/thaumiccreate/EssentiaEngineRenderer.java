package com.thaumiccreate;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class EssentiaEngineRenderer extends KineticBlockEntityRenderer<EssentiaEngineBlockEntity> {
    private static final PartialModel ROTOR = PartialModel.of(ResourceLocation.fromNamespaceAndPath(ThaumicCreate.ID, "block/essentia_engine_rotor"));
    public static void init() {}
    public EssentiaEngineRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(EssentiaEngineBlockEntity engine, float partialTicks,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!engine.upper()) return;
        // This addon has no Flywheel visual: render even when instancing is enabled.
        renderRotatingBuffer(engine, CachedBuffers.partialFacing(ROTOR, engine.getBlockState()),
                pose, buffers.getBuffer(RenderType.cutoutMipped()), light);
    }
}
