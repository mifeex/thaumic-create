package com.thaumiccreate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** FACING is the outlet, pointing into the receiver. */
public final class EssentiaPumpBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public EssentiaPumpBlock() {
        super(BlockBehaviour.Properties.of().strength(3).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(OPEN, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new EssentiaPumpBlock()); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, OPEN); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssentiaPumpBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?createTickerHelper(type,DeviceRegistry.PUMP_ENTITY.get(),(l,p,s,be)->be.animateValve()):null;
    }
    private static double[] orient(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case SOUTH -> new double[]{x,y,z}; case NORTH -> new double[]{16-x,y,16-z};
            case EAST -> new double[]{z,y,16-x}; case WEST -> new double[]{16-z,y,x};
            case UP -> new double[]{x,z,16-y}; case DOWN -> new double[]{x,16-z,y};
        };
    }
    private static VoxelShape box(Direction facing, double x1,double y1,double z1,double x2,double y2,double z2) {
        // Match the model turn and alignment: outlet centre (8,8), end at Z=16.
        var a=orient(16-x2,y1+1,17-z2,facing); var b=orient(16-x1,y2+1,17-z1,facing);
        return Block.box(Math.min(a[0],b[0]),Math.min(a[1],b[1]),Math.min(a[2],b[2]),Math.max(a[0],b[0]),Math.max(a[1],b[1]),Math.max(a[2],b[2]));
    }
    private static VoxelShape valve(BlockState s) { return s.getValue(OPEN) ? box(s.getValue(FACING),3,12,1,13,16,11) : box(s.getValue(FACING),3,10,1,13,14,11); }
    @Override public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f=s.getValue(FACING);
        return Shapes.or(box(f,4,3,8,12,11,16), box(f,5,4,1,11,10,8), box(f,6,10,4,10,13,8),valve(s));
    }
    public boolean toggleValve(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var local=hit.getLocation().subtract(pos.getX(),pos.getY(),pos.getZ());
        boolean onValve=valve(s).toAabbs().stream().anyMatch(b -> b.inflate(.04).contains(local));
        if (!onValve) return false;
        if (!level.isClientSide) {
            boolean open=!s.getValue(OPEN);
            level.setBlock(pos,s.setValue(OPEN,open),3);
            level.playSound(null,pos,com.thaumicreborn.registry.ModSounds.SQUEEK.get(),net.minecraft.sounds.SoundSource.BLOCKS,.7f,.9f+level.random.nextFloat()*.2f);
        }
        return true;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        return toggleValve(s,l,p,player,hit)?ItemInteractionResult.sidedSuccess(l.isClientSide):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        return toggleValve(s,l,p,player,hit)?InteractionResult.sidedSuccess(l.isClientSide):InteractionResult.PASS;
    }
}
