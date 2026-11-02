package com.thaumiccreate;
import com.thaumicreborn.client.particle.InfusionArcParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import java.util.Map;
import java.util.WeakHashMap;

/** Uses the exact essentia arc particle and fade used by TR essence mirrors. */
public final class EssentiaPumpVisualEffects {
    private static final Map<EssentiaPumpBlockEntity,Long> LAST_TICK=new WeakHashMap<>();
    private EssentiaPumpVisualEffects(){}
    public static void tick(EssentiaPumpBlockEntity pump){
        if(!(pump.getLevel() instanceof ClientLevel level))return;
        long time=level.getGameTime();var previous=LAST_TICK.put(pump,time);
        if(previous!=null&&previous==time)return;
        var source=pump.effectMirror();
        int remaining=(int)Math.max(0,pump.effectUntil()-time);
        if(source==null||remaining<=0||!pump.getBlockState().getValue(EssentiaPumpBlock.OPEN))return;
        float scale=remaining>5?1f:remaining*remaining/25f;
        var target=pump.getBlockPos();
        Minecraft.getInstance().particleEngine.add(InfusionArcParticle.essentia(level,
                source.getX()+.5,source.getY()+.5,source.getZ()+.5,
                target.getX()+.5,target.getY()+.5,target.getZ()+.5,
                (int)time-Math.max(0,5-remaining),pump.effectColor(),scale));
    }
}
