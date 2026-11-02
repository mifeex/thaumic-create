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
    private static final java.util.Map<String,PartialModel> PANELS=java.util.Map.of("potentia",PartialModel.of(ResourceLocation.fromNamespaceAndPath(ThaumicCreate.ID, "block/engine_panel_potentia")),"arbor",PartialModel.of(ResourceLocation.fromNamespaceAndPath(ThaumicCreate.ID, "block/engine_panel_arbor")),"praecantatio",PartialModel.of(ResourceLocation.fromNamespaceAndPath(ThaumicCreate.ID, "block/engine_panel_praecantatio")));
    public static void init() {}
    public EssentiaEngineRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(EssentiaEngineBlockEntity engine, float partialTicks,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!engine.upper()) {
            CachedBuffers.partialFacing(PANELS.getOrDefault(engine.activeFuel(),PANELS.get("potentia")),engine.getBlockState(),engine.getBlockState().getValue(EssentiaEngineBlock.HORIZONTAL_FACING).getOpposite()).color(engine.activeFuel().equals("arbor")?0x876531:engine.activeFuel().equals("praecantatio")?0x9700C0:0xC0FFFF).light(light).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            return;
        }
        // This addon has no Flywheel visual: render even when instancing is enabled.
        renderRotatingBuffer(engine, CachedBuffers.partialFacing(ROTOR, engine.getBlockState(),
                        engine.getBlockState().getValue(EssentiaEngineBlock.HORIZONTAL_FACING)),
                pose, buffers.getBuffer(RenderType.cutoutMipped()), light);
    }
}
