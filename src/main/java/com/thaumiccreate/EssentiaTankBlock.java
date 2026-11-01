package com.thaumiccreate;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class EssentiaTankBlock extends FluidTankBlock {
    public EssentiaTankBlock() { super(BlockBehaviour.Properties.of().strength(3.0F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK), false); }
    @Override public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() { return DeviceRegistry.TANK_ENTITY.get(); }
}
