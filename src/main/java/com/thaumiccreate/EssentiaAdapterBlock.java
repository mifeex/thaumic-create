package com.thaumiccreate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class EssentiaAdapterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty IMPORTING = BooleanProperty.create("importing");
    public EssentiaAdapterBlock() {
        super(BlockBehaviour.Properties.of().strength(3).sound(SoundType.NETHERITE_BLOCK).noOcclusion());
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(IMPORTING, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING, IMPORTING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssentiaAdapterBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, DeviceRegistry.ADAPTER_ENTITY.get(), EssentiaAdapterBlockEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        toggle(state, level, pos, player); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private void toggle(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        if (player.isShiftKeyDown()) {
            state = state.cycle(IMPORTING); level.setBlockAndUpdate(pos, state);
        }
        var be = (EssentiaAdapterBlockEntity) level.getBlockEntity(pos);
        String aspect = EssentiaFluid.aspect(be.buffer.getFluid());
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
            state.getValue(IMPORTING) ? "message.thaumic_create.import" : "message.thaumic_create.export",
            aspect == null ? "—" : aspect, be.buffer.getFluidAmount() / EssentiaFluid.UNIT), true);
    }
}
