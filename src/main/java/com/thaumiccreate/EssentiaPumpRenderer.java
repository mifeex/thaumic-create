package com.thaumiccreate;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class EssentiaPumpRenderer extends SafeBlockEntityRenderer<EssentiaPumpBlockEntity>{
    private static final PartialModel VALVE=PartialModel.of(ResourceLocation.fromNamespaceAndPath(ThaumicCreate.ID,"block/essentia_pump_valve"));
    public static void init(){}
    public EssentiaPumpRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(EssentiaPumpBlockEntity pump,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        EssentiaPumpVisualEffects.tick(pump);
        var side=pump.outlet();float rotation=pump.valveRotation(partialTick);
        poses.pushPose();
        poses.translate(.5,.5,.5);
        poses.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(side)));
        poses.mulPose(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(side)));
        poses.translate(-.5,-.5,-.5);
        // Same animation as TR's valve tube: 20 degrees/tick, 1.5x spin,
        // and 0.12 blocks of axial retraction across the full 360-degree travel.
        poses.translate(.5,0,11/16.0);
        poses.mulPose(Axis.YP.rotationDegrees(-rotation*1.5f));
        poses.translate(0,-rotation/360f*.12f,0);
        poses.translate(-.5,0,-11/16.0);
        CachedBuffers.partial(VALVE,pump.getBlockState()).light(light).renderInto(poses,buffers.getBuffer(RenderType.cutoutMipped()));
        poses.popPose();
    }
}
