package com.thaumiccreate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.createmod.catnip.placement.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.Predicate;

public final class EngineCogPlacement implements IPlacementHelper {
    public static final int ID = PlacementHelpers.register(new EngineCogPlacement());
    public static void init() {}
    public Predicate<ItemStack> getItemPredicate(){return ICogWheel::isDedicatedCogItem;}
    public Predicate<BlockState> getStatePredicate(){return s->s.getBlock() instanceof EssentiaEngineBlock && s.getValue(EssentiaEngineBlock.UPPER);}
    public static Direction.Axis axis(BlockState engine, boolean large){
        var facing=engine.getValue(EssentiaEngineBlock.HORIZONTAL_FACING);
        return large?facing.getAxis():facing.getClockWise().getAxis();
    }
    public PlacementOffset getOffset(Player player,Level level,BlockState state,BlockPos pos,BlockHitResult hit){
        return getOffset(player,level,state,pos,hit,player.getMainHandItem());
    }
    @Override public PlacementOffset getOffset(Player player,Level level,BlockState state,BlockPos pos,BlockHitResult hit,ItemStack stack){
        if(!matchesState(state)||!matchesItem(stack)||player.isShiftKeyDown())return PlacementOffset.fail();
        boolean large=ICogWheel.isLargeCogItem(stack);
        var axis=axis(state,large);
        var cog=((BlockItem)stack.getItem()).getBlock().defaultBlockState().setValue(BlockStateProperties.AXIS,axis);
        var directions=large?java.util.List.of(Direction.UP):IPlacementHelper.orderedByDistanceExceptAxis(pos,hit.getLocation(),axis);
        for(var side:directions){
            var target=pos.relative(side);
            if(!level.getBlockState(target).canBeReplaced()||!cog.canSurvive(level,target))continue;
            return PlacementOffset.success(target,s->s.setValue(BlockStateProperties.AXIS,axis)).withGhostState(cog);
        }
        return PlacementOffset.fail();
    }
    public static net.minecraft.world.ItemInteractionResult place(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var helper=PlacementHelpers.get(ID);var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()||!helper.matchesState(state)||!helper.matchesItem(stack))return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var offset=helper.getOffset(player,level,state,pos,hit,stack);
        return offset.isSuccessful()?offset.placeInWorld(level,(BlockItem)stack.getItem(),player,hand,hit):net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
