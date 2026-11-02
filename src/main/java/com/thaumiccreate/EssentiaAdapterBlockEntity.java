package com.thaumiccreate;
import com.thaumicreborn.api.ThaumicRebornApi;
import com.thaumicreborn.api.essentia.EssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class EssentiaAdapterBlockEntity extends com.simibubi.create.foundation.blockEntity.SmartBlockEntity implements EssentiaTransport {
    public static final float MIN_PUMP_SPEED = 16;
    public static final int CAPACITY = 64 * EssentiaFluid.UNIT;
    public final FluidTank buffer = new FluidTank(CAPACITY, EssentiaFluid::valid) {
        @Override protected void onContentsChanged() { changed(); }
    };
    public final IFluidHandler pipe = new IFluidHandler() {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return buffer.getFluid().copy(); }
        public int getTankCapacity(int tank) { return buffer.getCapacity(); }
        public boolean isFluidValid(int tank, FluidStack stack) { return EssentiaFluid.valid(stack); }
        public int fill(FluidStack stack, FluidAction action) { return pumpDirection() > 0 ? buffer.fill(stack, action) : 0; }
        public FluidStack drain(int amount, FluidAction action) { return pumpDirection() < 0 ? buffer.drain(amount, action) : FluidStack.EMPTY; }
        public FluidStack drain(FluidStack stack, FluidAction action) { return pumpDirection() < 0 ? buffer.drain(stack, action) : FluidStack.EMPTY; }
    };
    public EssentiaAdapterBlockEntity(BlockPos pos, BlockState state) { super(DeviceRegistry.ADAPTER_ENTITY.get(), pos, state); }
    @Override public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {}
    public Direction front() { return getBlockState().getValue(EssentiaAdapterBlock.FACING); }
    // The Create port requires a directly attached, aligned pump.
    // Negative: pump draws from this port. Positive: pump pushes into it.
    private int pumpDirection() {
        if (level == null) return 0;
        var adjacent = level.getBlockEntity(worldPosition.relative(front()));
        if (!(adjacent instanceof com.simibubi.create.content.fluids.pump.PumpBlockEntity pump)) return 0;
        Direction side = front().getOpposite();
        if (Math.abs(pump.getSpeed()) < MIN_PUMP_SPEED || !pump.isSideAccessible(side)) return 0;
        return pump.isPullingOnSide(pump.getBlockState().getValue(
            com.simibubi.create.content.fluids.pump.PumpBlock.FACING) == side) ? -1 : 1;
    }
    @Override public void tick() {
        super.tick();
        Level level = this.level;
        if (level == null) return;
        BlockPos pos = worldPosition;
        EssentiaAdapterBlockEntity be = this;
        if (!be.canInputFrom(be.front().getOpposite())) return;
        if (level.isClientSide || level.getGameTime() % 5 != 0 || !ThaumicRebornApi.isAvailable()) return;
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
    public boolean canInputFrom(Direction side) { return isConnectable(side) && pumpDirection() < 0; }
    public boolean canOutputTo(Direction side) { return isConnectable(side) && pumpDirection() >= 0; }
    public void setSuction(String aspect, int amount) {}
    public String suctionType(Direction side) { return isConnectable(side) ? EssentiaFluid.aspect(buffer.getFluid()) : null; }
    public int suctionAmount(Direction side) { return canInputFrom(side) && buffer.getSpace() >= EssentiaFluid.UNIT ? 128 : 0; }
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
    @Override protected void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider, boolean packet) {
        super.write(tag, provider, packet); tag.put("Buffer", buffer.writeToNBT(provider, new CompoundTag()));
    }
    @Override protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider, boolean packet) {
        super.read(tag, provider, packet); buffer.readFromNBT(provider, tag.getCompound("Buffer"));
    }
}
