package com.thaumiccreate;
import com.thaumicreborn.api.ThaumicRebornApi;
import com.thaumicreborn.api.essentia.EssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class EssentiaAdapterBlockEntity extends BlockEntity implements EssentiaTransport {
    public final FluidTank buffer = new FluidTank(EssentiaFluid.BLOCK_CAPACITY, EssentiaFluid::valid) {
        @Override protected void onContentsChanged() { changed(); }
    };
    public final IFluidHandler pipe = new IFluidHandler() {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return buffer.getFluid().copy(); }
        public int getTankCapacity(int tank) { return buffer.getCapacity(); }
        public boolean isFluidValid(int tank, FluidStack stack) { return importing() && EssentiaFluid.valid(stack); }
        public int fill(FluidStack stack, FluidAction action) { return importing() ? buffer.fill(stack, action) : 0; }
        public FluidStack drain(int amount, FluidAction action) { return importing() ? FluidStack.EMPTY : buffer.drain(amount, action); }
        public FluidStack drain(FluidStack stack, FluidAction action) { return importing() ? FluidStack.EMPTY : buffer.drain(stack, action); }
    };
    public EssentiaAdapterBlockEntity(BlockPos pos, BlockState state) { super(DeviceRegistry.ADAPTER_ENTITY.get(), pos, state); }
    public Direction front() { return getBlockState().getValue(EssentiaAdapterBlock.FACING); }
    public boolean importing() { return getBlockState().getValue(EssentiaAdapterBlock.IMPORTING); }
    public static void tick(Level level, BlockPos pos, BlockState state, EssentiaAdapterBlockEntity be) {
        if (level.isClientSide || level.getGameTime() % 5 != 0 || be.importing() || !ThaumicRebornApi.isAvailable()) return;
        Direction rear = be.front().getOpposite();
        var remote = ThaumicRebornApi.essentia().neighbour(level, pos, rear).orElse(null);
        if (remote == null || !remote.canOutputTo(be.front()) || remote.suctionAmount(be.front()) >= be.suctionAmount(rear)
            || be.suctionAmount(rear) < remote.minimumSuction()) return;
        String aspect = remote.essentiaType(be.front());
        if (aspect == null || be.buffer.fill(EssentiaFluid.stack(aspect, EssentiaFluid.UNIT), IFluidHandler.FluidAction.SIMULATE) != EssentiaFluid.UNIT) return;
        int taken = remote.takeEssentia(aspect, 1, be.front());
        if (taken > 0) be.buffer.fill(EssentiaFluid.stack(aspect, taken * EssentiaFluid.UNIT), IFluidHandler.FluidAction.EXECUTE);
    }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public boolean isConnectable(Direction side) { return side == front().getOpposite(); }
    public boolean canInputFrom(Direction side) { return isConnectable(side) && !importing(); }
    public boolean canOutputTo(Direction side) { return isConnectable(side) && importing(); }
    public void setSuction(String aspect, int amount) {}
    public String suctionType(Direction side) { return isConnectable(side) ? EssentiaFluid.aspect(buffer.getFluid()) : null; }
    public int suctionAmount(Direction side) { return canInputFrom(side) && buffer.getSpace() >= EssentiaFluid.UNIT ? 32 : 0; }
    public String essentiaType(Direction side) { return isConnectable(side) ? EssentiaFluid.aspect(buffer.getFluid()) : null; }
    public int essentiaAmount(Direction side) { return isConnectable(side) ? buffer.getFluidAmount() / EssentiaFluid.UNIT : 0; }
    public int minimumSuction() { return 0; }
    public int takeEssentia(String aspect, int amount, Direction side) {
        if (!canOutputTo(side) || amount <= 0 || !java.util.Objects.equals(aspect, essentiaType(side))) return 0;
        int units = Math.min(amount, buffer.getFluidAmount() / EssentiaFluid.UNIT);
        buffer.drain(units * EssentiaFluid.UNIT, IFluidHandler.FluidAction.EXECUTE);
        return units;
    }
    public int addEssentia(String aspect, int amount, Direction side) {
        if (!canInputFrom(side) || amount <= 0 || !EssentiaFluid.valid(EssentiaFluid.stack(aspect, 1))) return 0;
        int units = Math.min(amount, buffer.getSpace() / EssentiaFluid.UNIT);
        FluidStack stack = EssentiaFluid.stack(aspect, units * EssentiaFluid.UNIT);
        int accepted = buffer.fill(stack, IFluidHandler.FluidAction.SIMULATE) / EssentiaFluid.UNIT;
        if (accepted > 0) buffer.fill(EssentiaFluid.stack(aspect, accepted * EssentiaFluid.UNIT), IFluidHandler.FluidAction.EXECUTE);
        return accepted;
    }
    @Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); tag.put("Buffer", buffer.writeToNBT(provider, new CompoundTag()));
    }
    @Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider); buffer.readFromNBT(provider, tag.getCompound("Buffer"));
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
