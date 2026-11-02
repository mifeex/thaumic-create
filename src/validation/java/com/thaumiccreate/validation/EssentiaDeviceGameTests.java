package com.thaumiccreate.validation;

import com.thaumiccreate.*;
import com.thaumicreborn.api.ThaumicRebornApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumic_create_tests")
@PrefixGameTestTemplate(false)
public final class EssentiaDeviceGameTests {
    @GameTest(template="empty", timeoutTicks=100)
    public static void singleTankStoresExactly64(GameTestHelper h) { tank(h,1,1); }
    @GameTest(template="empty", timeoutTicks=100)
    public static void fourTankStoresExactly256(GameTestHelper h) { tank(h,2,1); }
    @GameTest(template="empty", timeoutTicks=150)
    public static void maximumTankStoresExactly2560(GameTestHelper h) { tank(h,2,10); }
    private static void tank(GameTestHelper h,int width,int height) {
        BlockPos origin=h.absolutePos(new BlockPos(1,2,1));
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)for(int z=0;z<width;z++)
            h.getLevel().setBlockAndUpdate(origin.offset(x,y,z),DeviceRegistry.TANK.get().defaultBlockState());
        h.runAfterDelay(30,()->{
            var be=(EssentiaTankBlockEntity)h.getLevel().getBlockEntity(origin);
            var controller=be.getControllerBE();
            h.assertTrue(controller!=null,"No multiblock controller");
            var tank=controller.getTankInventory();
            int capacity=width*width*height*64*125;
            h.assertTrue(tank.getCapacity()==capacity,"Capacity "+tank.getCapacity()+" expected "+capacity);
            var handler=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,origin,Direction.NORTH);
            h.assertTrue(handler!=null,"No fluid pipe capability");
            h.assertTrue(handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Tank accepted water");
            h.assertTrue(handler.fill(EssentiaFluid.stack("aer",capacity+125),FluidAction.EXECUTE)==capacity,"Wrong essentia capacity");
            h.assertTrue(handler.fill(EssentiaFluid.stack("ignis",125),FluidAction.EXECUTE)==0,"Mixed aspects");
            var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
            be.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(be.getControllerBE().getTankInventory().getFluidAmount()==capacity,"Tank save lost essentia");
            var output=handler.drain(capacity,FluidAction.EXECUTE);
            h.assertTrue(output.getAmount()==capacity && "aer".equals(EssentiaFluid.aspect(output)),"Lost or changed essentia");
            h.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void adapterConservesFractionalFluidAndSides(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));
        h.getLevel().setBlockAndUpdate(pos,DeviceRegistry.ADAPTER.get().defaultBlockState().setValue(EssentiaAdapterBlock.FACING,Direction.EAST));
        var be=(EssentiaAdapterBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(ThaumicRebornApi.essentia().transport(h.getLevel(),pos).orElse(null)==be,"TR API cannot discover adapter");
        pressurizeTestPipe(h,pos);
        h.assertTrue(be.addEssentia("aer",64,Direction.EAST)==0,"TR accepted front input");
        h.assertTrue(be.addEssentia("aer",64,Direction.WEST)==64,"TR input failed");
        h.assertTrue(be.addEssentia("ignis",1,Direction.WEST)==0,"Mixed adapter aspects");
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.WEST)==null,"Create capability exposed on TR side");
        var pipe=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.EAST);
        h.assertTrue(pipe!=null,"No Create port");
        h.assertTrue(pipe.getTankCapacity(0)==8000 && pipe.fill(EssentiaFluid.stack("aer",125),FluidAction.SIMULATE)==0,"Adapter capacity is not 64 units");
        var fluid=pipe.drain(1,FluidAction.SIMULATE);
        h.assertTrue(fluid.getAmount()==1 && be.buffer.getFluidAmount()==8000,"Simulation changed state");
        fluid=pipe.drain(1,FluidAction.EXECUTE);
        h.assertTrue(fluid.getAmount()==1 && be.buffer.getFluidAmount()==7999,"Fractional drain rounded");
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
        be.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(be.buffer.getFluidAmount()==7999 && "aer".equals(EssentiaFluid.aspect(be.buffer.getFluid())),"Save lost payload");
        h.getLevel().setBlockAndUpdate(pos.east(),com.simibubi.create.AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.WEST));
        ((com.simibubi.create.content.fluids.pump.PumpBlockEntity)h.getLevel().getBlockEntity(pos.east())).setSpeed(16);
        pipe=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.EAST);
        h.assertTrue(pipe.fill(fluid,FluidAction.EXECUTE)==1,"Cannot return fractional residue");
        h.assertTrue(be.takeEssentia("aer",100,Direction.WEST)==64,"Return to TR was not exact");
        h.assertTrue(be.buffer.isEmpty(),"Round trip duplicated or lost fluid");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void adapterRequires16RpmButAllowsPassiveTrOutput(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));
        h.getLevel().setBlockAndUpdate(pos,DeviceRegistry.ADAPTER.get().defaultBlockState().setValue(EssentiaAdapterBlock.FACING,Direction.EAST));
        var be=(EssentiaAdapterBlockEntity)h.getLevel().getBlockEntity(pos);
        for (Direction facing : new Direction[]{Direction.EAST,Direction.WEST}) {
            h.getLevel().setBlockAndUpdate(pos.east(),com.simibubi.create.AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,facing));
            var pump=(com.simibubi.create.content.fluids.pump.PumpBlockEntity)h.getLevel().getBlockEntity(pos.east());
            for (float speed : new float[]{0,15,-15,16,-16}) {
                pump.setSpeed(speed);
                be.buffer.drain(8000,FluidAction.EXECUTE);
                be.buffer.fill(EssentiaFluid.stack("aer",250),FluidAction.EXECUTE);
                boolean active=Math.abs(speed)>=16;
                boolean pulling=pump.isPullingOnSide(facing==Direction.WEST);
                h.assertTrue(be.pipe.fill(EssentiaFluid.stack("aer",125),FluidAction.SIMULATE)==(active&&!pulling?125:0),"Wrong Create input at "+speed);
                h.assertTrue(be.pipe.drain(125,FluidAction.SIMULATE).getAmount()==(active&&pulling?125:0),"Wrong Create output at "+speed);
                h.assertTrue(be.addEssentia("aer",1,Direction.WEST)==(active&&pulling?1:0),"Wrong TR input at "+speed);
                h.assertTrue(be.takeEssentia("aer",1,Direction.WEST)==(!active||!pulling?1:0),"Wrong TR output at "+speed);
            }
        }
        h.getLevel().setBlockAndUpdate(pos.east(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.assertTrue(be.takeEssentia("aer",1,Direction.WEST)==1,"Stored essentia cannot leave through TR without pump");
        h.succeed();
    }
    private static void pressurizeTestPipe(GameTestHelper h, BlockPos adapter) {
        var level=h.getLevel();
        var pipe=adapter.east();
        level.setBlockAndUpdate(pipe,com.simibubi.create.AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.EAST));
        ((com.simibubi.create.content.fluids.pump.PumpBlockEntity)level.getBlockEntity(pipe)).setSpeed(16);
    }

    @GameTest(template="empty", timeoutTicks=300)
    public static void pipeCannotReplaceDirectPump(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));
        h.getLevel().setBlockAndUpdate(pos,DeviceRegistry.ADAPTER.get().defaultBlockState().setValue(EssentiaAdapterBlock.FACING,Direction.EAST));
        var be=(EssentiaAdapterBlockEntity)h.getLevel().getBlockEntity(pos);
        h.getLevel().setBlockAndUpdate(pos.east(),com.simibubi.create.AllBlocks.FLUID_PIPE.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.PipeBlock.EAST,true).setValue(net.minecraft.world.level.block.PipeBlock.WEST,true));
        var behavior=com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(h.getLevel(),pos.east(),com.simibubi.create.content.fluids.FluidTransportBehaviour.TYPE);
        behavior.addPressure(Direction.WEST,true,128);
        be.buffer.fill(EssentiaFluid.stack("aer",125),FluidAction.EXECUTE);
        h.assertTrue(be.addEssentia("aer",1,Direction.WEST)==0 && be.pipe.drain(125,FluidAction.EXECUTE).isEmpty(),"Pipe bypassed direct pump requirement");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=300)
    public static void adjacentPumpMovesTypedEssentia(GameTestHelper h) {
        pumpChain(h, true);
    }
    @GameTest(template="empty", timeoutTicks=300)
    public static void pumpTransfersTankToJarWithoutMode(GameTestHelper h) {
        var level=h.getLevel();
        var pos=h.absolutePos(new BlockPos(3,3,1));
        var jarPos=pos.west(2).below();
        level.setBlockAndUpdate(jarPos,com.thaumicreborn.registry.ModBlocks.WARDED_JAR.get().defaultBlockState());
        var tube=com.thaumicreborn.registry.ModBlocks.ESSENTIA_TUBE.get().defaultBlockState();
        level.setBlockAndUpdate(jarPos.above(),tube);
        level.setBlockAndUpdate(pos.west(),tube);
        // Existing worlds may contain either value of the old, now ignored property.
        level.setBlockAndUpdate(pos,DeviceRegistry.ADAPTER.get().defaultBlockState()
            .setValue(EssentiaAdapterBlock.FACING,Direction.EAST).setValue(EssentiaAdapterBlock.IMPORTING,false));
        level.setBlockAndUpdate(pos.east(),com.simibubi.create.AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.WEST));
        level.setBlockAndUpdate(pos.east(2),com.simibubi.create.AllBlocks.FLUID_PIPE.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.PipeBlock.EAST,true)
            .setValue(net.minecraft.world.level.block.PipeBlock.WEST,true));
        level.setBlockAndUpdate(pos.east(3),DeviceRegistry.TANK.get().defaultBlockState());
        h.runAfterDelay(15,()->{
            var tank=(EssentiaTankBlockEntity)level.getBlockEntity(pos.east(3));
            h.assertTrue(tank.getControllerBE().getTankInventory().fill(EssentiaFluid.stack("aer",8000),FluidAction.EXECUTE)==8000,"Cannot fill source tank");
            var pump=(com.simibubi.create.content.fluids.pump.PumpBlockEntity)level.getBlockEntity(pos.east());
            pump.setSpeed(128); pump.onSpeedChanged(0); pump.updatePipesOnSide(Direction.EAST); pump.updatePipesOnSide(Direction.WEST);
        });
        h.runAfterDelay(180,()->{
            var jar=(com.thaumicreborn.api.essentia.EssentiaTransport)level.getBlockEntity(jarPos);
            var tank=(EssentiaTankBlockEntity)level.getBlockEntity(pos.east(3));
            h.assertTrue(jar.essentiaAmount(Direction.UP)>0 && "aer".equals(jar.essentiaType(Direction.UP)),"Pump did not move tank essentia into jar");
            h.assertTrue(tank.getControllerBE().getTankInventory().getFluidAmount()<8000,"Source tank was not drained");
            h.succeed();
        });
    }
    private static void pumpChain(GameTestHelper h, boolean adjacent) {
        var start=h.absolutePos(new BlockPos(3,3,1));
        var level=h.getLevel();
        var jarPos=start.west(2).below();
        level.setBlockAndUpdate(jarPos,com.thaumicreborn.registry.ModBlocks.WARDED_JAR.get().defaultBlockState());
        var jar=level.getBlockEntity(jarPos);
        var payload=new net.minecraft.nbt.CompoundTag();
        payload.putString("Aspect","aer"); payload.putString("AspectFilter","aer"); payload.putInt("Amount",64);
        jar.loadWithComponents(payload,level.registryAccess());
        var tube=com.thaumicreborn.registry.ModBlocks.ESSENTIA_TUBE.get().defaultBlockState();
        level.setBlockAndUpdate(jarPos.above(),tube);
        level.setBlockAndUpdate(start.west(),tube);
        level.setBlockAndUpdate(start,DeviceRegistry.ADAPTER.get().defaultBlockState()
            .setValue(EssentiaAdapterBlock.FACING,Direction.EAST).setValue(EssentiaAdapterBlock.IMPORTING,adjacent));
        var adapter=(EssentiaAdapterBlockEntity)level.getBlockEntity(start);
        int pumpDistance=adjacent?1:2;
        int tankDistance=adjacent?3:4;
        level.setBlockAndUpdate(start.east(tankDistance),DeviceRegistry.TANK.get().defaultBlockState());
        var pipe=com.simibubi.create.AllBlocks.FLUID_PIPE.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.PipeBlock.EAST,true)
            .setValue(net.minecraft.world.level.block.PipeBlock.WEST,true);
        if (!adjacent) level.setBlockAndUpdate(start.east(),pipe);
        level.setBlockAndUpdate(start.east(tankDistance-1),pipe);
        level.setBlockAndUpdate(start.east(pumpDistance),com.simibubi.create.AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.EAST));
        h.runAfterDelay(25,()->{
            var source=(com.thaumicreborn.api.essentia.EssentiaTransport)jar;
            h.assertTrue(source.essentiaAmount(Direction.UP)==64 && adapter.buffer.isEmpty(),"Stopped pump drained jar");
            var pump=(com.simibubi.create.content.fluids.pump.PumpBlockEntity)level.getBlockEntity(start.east(pumpDistance));
            pump.setSpeed(128);pump.onSpeedChanged(0);pump.updatePipesOnSide(Direction.EAST);pump.updatePipesOnSide(Direction.WEST);
        });
        h.runAfterDelay(150,()->{
            var tank=(EssentiaTankBlockEntity)level.getBlockEntity(start.east(tankDistance));
            var fluid=tank.getControllerBE().getTankInventory().getFluid();
            h.assertTrue(fluid.getAmount()>0,"Create pump moved no essentia");
            h.assertTrue("aer".equals(EssentiaFluid.aspect(fluid)),"Pump discarded aspect payload");
            var source=(com.thaumicreborn.api.essentia.EssentiaTransport)jar;
            h.assertTrue(source.essentiaAmount(Direction.UP)<64,"Pump chain did not drain the jar");
            h.assertTrue(fluid.getAmount()+adapter.buffer.getFluidAmount()+source.essentiaAmount(Direction.UP)*125<=8000,"Pump duplicated essentia");
            var pump=(com.simibubi.create.content.fluids.pump.PumpBlockEntity)level.getBlockEntity(start.east(pumpDistance));
            pump.setSpeed(0); pump.onSpeedChanged(128);
            pump.updatePipesOnSide(Direction.EAST); pump.updatePipesOnSide(Direction.WEST);
            h.runAfterDelay(40,()->{
                int remaining=source.essentiaAmount(Direction.UP);
                h.runAfterDelay(25,()->{
                    h.assertTrue(source.essentiaAmount(Direction.UP)==remaining,"Jar drained after pump stopped");
                    h.succeed();
                });
            });
        });
    }
}
