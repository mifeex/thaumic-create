package com.thaumiccreate.mixin;
import com.thaumiccreate.EssentiaEngineBlockEntity;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RotationPropagator.class, remap = false)
public class EngineCogConnectionMixin {
    @Inject(method = "getRotationSpeedModifier", at = @At("HEAD"), cancellable = true)
    private static void engineCogRatio(KineticBlockEntity from, KineticBlockEntity to,
            CallbackInfoReturnable<Float> result) {
        boolean forward = from instanceof EssentiaEngineBlockEntity;
        if (!forward && !(to instanceof EssentiaEngineBlockEntity)) return;
        var engine = (EssentiaEngineBlockEntity)(forward ? from : to);
        var cog = forward ? to : from;
        var state = cog.getBlockState();
        if (!engine.upper() || !(state.getBlock() instanceof ICogWheel)) return;
        var delta = cog.getBlockPos().subtract(engine.getBlockPos());
        if (delta.distManhattan(net.minecraft.core.BlockPos.ZERO) != 1) return;
        var engineAxis = ((IRotate)engine.getBlockState().getBlock()).getRotationAxis(engine.getBlockState());
        var cogAxis = ((IRotate)state.getBlock()).getRotationAxis(state);
        if (ICogWheel.isSmallCog(state) && cogAxis == engineAxis
                && engineAxis.choose(delta.getX(),delta.getY(),delta.getZ()) == 0) {
            result.setReturnValue(-1f);
        } else if (ICogWheel.isLargeCog(state) && delta.equals(net.minecraft.core.BlockPos.ZERO.above())
                && cogAxis.isHorizontal() && cogAxis != engineAxis) {
            // Speed-controller style: a large cog directly above the exposed rotor.
            result.setReturnValue(1f);
        }
    }
}
