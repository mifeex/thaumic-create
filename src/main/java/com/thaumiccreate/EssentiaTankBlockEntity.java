package com.thaumiccreate;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Create connectivity, with a fixed capacity and an essentia-only inventory. */
public final class EssentiaTankBlockEntity extends FluidTankBlockEntity {
    public EssentiaTankBlockEntity(BlockPos pos, BlockState state) { super(DeviceRegistry.TANK_ENTITY.get(), pos, state); }
    @Override protected SmartFluidTank createInventory() {
        SmartFluidTank tank = new SmartFluidTank(EssentiaFluid.BLOCK_CAPACITY, this::onFluidStackChanged);
        tank.setValidator(EssentiaFluid::valid);
        return tank;
    }
    @Override public int getMaxWidth() { return 2; }
    @Override public int getMaxLength(Direction.Axis axis, int width) { return axis == Direction.Axis.Y ? 10 : 2; }
    @Override public int getTankSize(int tank) { return EssentiaFluid.BLOCK_CAPACITY; }
    @Override public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(Math.max(1, Math.min(40, blocks)) * EssentiaFluid.BLOCK_CAPACITY);
        int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
        if (overflow > 0) tankInventory.drain(overflow, FluidAction.EXECUTE);
        forceFluidLevelUpdate = true;
    }
    @Override protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider, boolean packet) {
        super.read(tag, provider, packet);
        tankInventory.setCapacity(Math.max(1, Math.min(40, getTotalTankSize())) * EssentiaFluid.BLOCK_CAPACITY);
        // Super reads Create's configurable capacity; restore the original payload after fixing ours.
        if (isController() && tag.contains("TankContent")) tankInventory.readFromNBT(provider, tag.getCompound("TankContent"));
    }
    @Override public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> lines, boolean sneaking) {
        var controller = getControllerBE();
        if (controller == null) return false;
        var tank = controller.getTankInventory();
        String aspect = EssentiaFluid.aspect(tank.getFluid());
        lines.add(net.minecraft.network.chat.Component.translatable("block.thaumic_create.essentia_tank"));
        lines.add(net.minecraft.network.chat.Component.literal("  " + (aspect == null ? "—" : aspect)));
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.thaumic_create.essentia_amount",
            tank.getFluidAmount() / EssentiaFluid.UNIT, tank.getCapacity() / EssentiaFluid.UNIT));
        return true;
    }
    @Override public void updateBoilerState() { /* Essentia is never boiler water. */ }
}
