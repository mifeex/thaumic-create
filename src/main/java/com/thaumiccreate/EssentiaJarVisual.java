package com.thaumiccreate;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import org.joml.Matrix4f;

/** Uses Thaumic Reborn's animated jar sprite and matching translucent render state. */
public final class EssentiaJarVisual extends RenderStateShard {
    private static final RenderType LIQUID = RenderType.create("thaumic_create:jar_essentia",
        DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
        RenderType.CompositeState.builder()
            .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
            .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
            .setCullState(NO_CULL).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
    private EssentiaJarVisual() { super("thaumic_create_jar_essentia", () -> {}, () -> {}); }
    public static void render(PoseStack pose, MultiBufferSource buffers, int color,
            float x0,float y0,float z0,float x1,float y1,float z1) {
        if (y1 <= y0) return;
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumic_reborn", "block/animatedglow"));
        var out=buffers.getBuffer(LIQUID); var matrix=pose.last().pose();
        int r=(color >> 16)&255, g=(color >> 8)&255, b=color&255;
        quad(out,matrix,sprite,r,g,b,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1);
        quad(out,matrix,sprite,r,g,b,x0,y0,z1,x1,y0,z1,x1,y0,z0,x0,y0,z0);
        quad(out,matrix,sprite,r,g,b,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0);
        quad(out,matrix,sprite,r,g,b,x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1);
        quad(out,matrix,sprite,r,g,b,x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0);
        quad(out,matrix,sprite,r,g,b,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1);
    }
    private static void quad(VertexConsumer out,Matrix4f matrix,TextureAtlasSprite s,int r,int g,int b,
            float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3) {
        vertex(out,matrix,x0,y0,z0,s.getU0(),s.getV1(),r,g,b);
        vertex(out,matrix,x1,y1,z1,s.getU1(),s.getV1(),r,g,b);
        vertex(out,matrix,x2,y2,z2,s.getU1(),s.getV0(),r,g,b);
        vertex(out,matrix,x3,y3,z3,s.getU0(),s.getV0(),r,g,b);
    }
    private static void vertex(VertexConsumer out,Matrix4f matrix,float x,float y,float z,float u,float v,int red,int green,int blue) {
        out.addVertex(matrix, x, y, z).setColor(red, green, blue, 255).setUv(u, v);
    }
}
