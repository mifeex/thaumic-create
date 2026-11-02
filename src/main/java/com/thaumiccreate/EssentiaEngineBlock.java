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
    public EssentiaEngineBlock() { super(BlockBehaviour.Properties.of().strength(3).noOcclusion().sound(SoundType.NETHERITE_BLOCK)); registerDefaultState(defaultBlockState().setValue(UPPER,false)); EngineCogPlacement.init(); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { super.createBlockStateDefinition(b);b.add(UPPER); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c) ? super.getStateForPlacement(c).setValue(UPPER,false) : null; }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack) { super.setPlacedBy(l,p,s,e,stack);if(!l.isClientSide) {l.setBlockAndUpdate(p.above(),s.setValue(UPPER,true));if(l.getBlockEntity(p) instanceof EssentiaEngineBlockEntity engine){engine.restoreEngineItem(EngineItemData.read(stack));engine.sendData();}} }
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q) {
        if(d==(s.getValue(UPPER)?Direction.DOWN:Direction.UP) && (!other.is(this)||other.getValue(UPPER)==s.getValue(UPPER))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,d,other,l,p,q);
    }
    private static final net.minecraft.world.phys.shapes.VoxelShape LOWER = net.minecraft.world.phys.shapes.Shapes.or(
            box(0,0,0,16,4,16),box(2,4,2,14,9,14),box(0,9,0,6,16,16),box(10,9,0,16,16,16),box(4,4,0,12,9,2),box(14,5,5,16,11,11),box(0,5,5,2,11,11));
    private static final net.minecraft.world.phys.shapes.VoxelShape UPPER_SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            box(0,0,4,5,12,12),box(11,0,4,16,12,12),box(0,6,6,16,10,10),box(6,0,0,10,16,16));
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        var shape=state.getValue(UPPER)?UPPER_SHAPE:LOWER;
        int turns=(state.getValue(HORIZONTAL_FACING).get2DDataValue()+2)%4;
        for(int i=0;i<turns;i++){
            var rotated=new java.util.ArrayList<net.minecraft.world.phys.shapes.VoxelShape>();
            shape.forAllBoxes((x0,y0,z0,x1,y1,z1)->rotated.add(net.minecraft.world.phys.shapes.Shapes.box(1-z1,y0,x0,1-z0,y1,x1)));
            shape=net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.phys.shapes.Shapes.empty(),rotated.toArray(net.minecraft.world.phys.shapes.VoxelShape[]::new));
        }
        return shape;
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
            BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return getShape(state, world, pos, context);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(switchFuel(state,level,pos,player,hit))return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        var result=EngineCogPlacement.place(state,level,pos,player,hand,hit);
        return result;
    }
    private boolean switchFuel(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(state.getValue(UPPER)||hit.getDirection()!=state.getValue(HORIZONTAL_FACING))return false;
        var local=hit.getLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(pos));
        double across=state.getValue(HORIZONTAL_FACING).getAxis()==Direction.Axis.Z?local.x:local.z;
        if(across<5/16.0||across>11/16.0||local.y<2/16.0||local.y>8/16.0)return false;
        if(level.getBlockEntity(pos) instanceof EssentiaEngineBlockEntity engine){if(!level.isClientSide){engine.cycleFuel();}return true;}return false;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){return switchFuel(state,level,pos,player,hit)?net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide):net.minecraft.world.InteractionResult.PASS;}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        if(state.getValue(UPPER))return java.util.List.of();
        var item=new ItemStack(DeviceRegistry.ENGINE_ITEM.get());
        var entity=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(entity instanceof EssentiaEngineBlockEntity engine)EngineItemData.write(item,engine.engineItemData());
        return java.util.List.of(item);
    }
    @Override public Direction.Axis getRotationAxis(BlockState s) { return s.getValue(HORIZONTAL_FACING).getClockWise().getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader l,BlockPos p,BlockState s,Direction d) { return false; }
    @Override public Class<EssentiaEngineBlockEntity> getBlockEntityClass() { return EssentiaEngineBlockEntity.class; }
    @Override public BlockEntityType<? extends EssentiaEngineBlockEntity> getBlockEntityType() { return DeviceRegistry.ENGINE_ENTITY.get(); }
}
