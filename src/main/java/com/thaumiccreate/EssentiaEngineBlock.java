package com.thaumiccreate;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EssentiaEngineBlock extends HorizontalKineticBlock implements IBE<EssentiaEngineBlockEntity> {
    public static final BooleanProperty UPPER=BooleanProperty.create("upper");
    public EssentiaEngineBlock() { super(BlockBehaviour.Properties.of().strength(3).noOcclusion().sound(SoundType.NETHERITE_BLOCK)); registerDefaultState(defaultBlockState().setValue(UPPER,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { super.createBlockStateDefinition(b);b.add(UPPER); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c) ? super.getStateForPlacement(c).setValue(UPPER,false) : null; }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack) { super.setPlacedBy(l,p,s,e,stack);if(!l.isClientSide) l.setBlockAndUpdate(p.above(),s.setValue(UPPER,true)); }
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q) {
        if(d==(s.getValue(UPPER)?Direction.DOWN:Direction.UP) && (!other.is(this)||other.getValue(UPPER)==s.getValue(UPPER))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,d,other,l,p,q);
    }
    // Leave the same clearance around the upper output as Create's speed controller.
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return state.getValue(UPPER) ? com.simibubi.create.AllShapes.SPEED_CONTROLLER
                : super.getShape(state, world, pos, context);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
            BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return getShape(state, world, pos, context);
    }
    @Override public Direction.Axis getRotationAxis(BlockState s) { return s.getValue(HORIZONTAL_FACING).getClockWise().getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader l,BlockPos p,BlockState s,Direction d) { return false; }
    @Override public Class<EssentiaEngineBlockEntity> getBlockEntityClass() { return EssentiaEngineBlockEntity.class; }
    @Override public BlockEntityType<? extends EssentiaEngineBlockEntity> getBlockEntityType() { return DeviceRegistry.ENGINE_ENTITY.get(); }
}
