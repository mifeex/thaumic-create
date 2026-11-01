package com.thaumiccreate.mixin;
import com.thaumiccreate.*;
import com.simibubi.create.content.kinetics.simpleRelays.*;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=CogWheelBlock.class,remap=false)
public class EngineCogPlacementMixin {
 @Inject(method="getAxisForPlacement",at=@At("HEAD"),cancellable=true)
 private void engineAxis(BlockPlaceContext context,CallbackInfoReturnable<Direction.Axis> result){
  if(context.getPlayer()!=null&&context.getPlayer().isShiftKeyDown())return;
  boolean large=((ICogWheel)(Object)this).isLargeCog();
  for(var side:Direction.values()){
   var target=context.getClickedPos().relative(side);var state=context.getLevel().getBlockState(target);
   if(!(state.getBlock() instanceof EssentiaEngineBlock)||!state.getValue(EssentiaEngineBlock.UPPER))continue;
   var delta=context.getClickedPos().subtract(target);var axis=EngineCogPlacement.axis(state,large);
   if(large?delta.equals(BlockPos.ZERO.above()):axis.choose(delta.getX(),delta.getY(),delta.getZ())==0){result.setReturnValue(axis);return;}
  }
 }
}
