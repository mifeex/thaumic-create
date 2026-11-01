package com.thaumiccreate.validation;

import com.thaumiccreate.*;
import com.thaumicreborn.api.ThaumicRebornApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
        h.assertTrue(com.thaumicreborn.crucible.CrucibleRecipeRegistry.all().stream().anyMatch(r -> r.id().toString().equals("kubejs:thaumic_create/brass")), "Migrated brass recipe missing");
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
            var handler=h.getLevel().getBlockEntity(origin).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.NORTH).orElse(null);
            h.assertTrue(handler!=null,"No fluid pipe capability");
            h.assertTrue(handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Tank accepted water");
            h.assertTrue(handler.fill(EssentiaFluid.stack("aer",capacity+125),FluidAction.EXECUTE)==capacity,"Wrong essentia capacity");
            h.assertTrue(handler.fill(EssentiaFluid.stack("ignis",125),FluidAction.EXECUTE)==0,"Mixed aspects");
            var saved=be.saveWithoutMetadata();
            be.load(saved);
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
        h.assertTrue(be.addEssentia("aer",64,Direction.EAST)==0,"TR accepted front input");
        h.assertTrue(be.addEssentia("aer",64,Direction.WEST)==64,"TR input failed");
        h.assertTrue(be.addEssentia("ignis",1,Direction.WEST)==0,"Mixed adapter aspects");
        h.assertTrue(h.getLevel().getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElse(null)==null,"Create capability exposed on TR side");
        var pipe=h.getLevel().getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.EAST).orElse(null);
        h.assertTrue(pipe!=null,"No Create port");
        var fluid=pipe.drain(1,FluidAction.SIMULATE);
        h.assertTrue(fluid.getAmount()==1 && be.buffer.getFluidAmount()==8000,"Simulation changed state");
        fluid=pipe.drain(1,FluidAction.EXECUTE);
        h.assertTrue(fluid.getAmount()==1 && be.buffer.getFluidAmount()==7999,"Fractional drain rounded");
        var saved=be.saveWithoutMetadata();
        be.load(saved);
        h.assertTrue(be.buffer.getFluidAmount()==7999 && "aer".equals(EssentiaFluid.aspect(be.buffer.getFluid())),"Save lost payload");
        h.getLevel().setBlockAndUpdate(pos,be.getBlockState().setValue(EssentiaAdapterBlock.IMPORTING,true));

        pipe=h.getLevel().getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.EAST).orElse(null);
        h.assertTrue(pipe.fill(fluid,FluidAction.EXECUTE)==1,"Cannot return fractional residue");
        h.assertTrue(be.takeEssentia("aer",100,Direction.WEST)==64,"Return to TR was not exact");
        h.assertTrue(be.buffer.isEmpty(),"Round trip duplicated or lost fluid");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void adapterPullsFromRealTrTube(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));
        h.getLevel().setBlockAndUpdate(pos,DeviceRegistry.ADAPTER.get().defaultBlockState().setValue(EssentiaAdapterBlock.FACING,Direction.EAST));
        var tubePos=pos.west();
        h.getLevel().setBlockAndUpdate(tubePos,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation("thaumic_reborn:essentia_tube")).defaultBlockState());
        var tube=ThaumicRebornApi.essentia().transport(h.getLevel(),tubePos).orElseThrow();
        h.assertTrue(tube.addEssentia("aer",1,Direction.WEST)==1,"Cannot prime tube");
        h.runAfterDelay(20,()->{
            var be=(EssentiaAdapterBlockEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(be.buffer.getFluidAmount()==125,"Real tube did not transfer one unit: "+be.buffer.getFluidAmount());
            h.assertTrue("aer".equals(EssentiaFluid.aspect(be.buffer.getFluid())),"Aspect changed");
            h.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=200)
    public static void createPumpMovesTypedEssentia(GameTestHelper h) {
        var start=h.absolutePos(new BlockPos(1,2,1));
        var level=h.getLevel();
        level.setBlockAndUpdate(start,DeviceRegistry.ADAPTER.get().defaultBlockState().setValue(EssentiaAdapterBlock.FACING,Direction.EAST));
        var adapter=(EssentiaAdapterBlockEntity)level.getBlockEntity(start);
        adapter.addEssentia("aer",64,Direction.WEST);
        level.setBlockAndUpdate(start.east(4),DeviceRegistry.TANK.get().defaultBlockState());
        var pipe=com.simibubi.create.AllBlocks.FLUID_PIPE.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.PipeBlock.EAST,true)
            .setValue(net.minecraft.world.level.block.PipeBlock.WEST,true);
        level.setBlockAndUpdate(start.east(),pipe);
        level.setBlockAndUpdate(start.east(3),pipe);
        level.setBlockAndUpdate(start.east(2),com.simibubi.create.AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.EAST));
        h.runAfterDelay(10,()->{
            var pump=(com.simibubi.create.content.fluids.pump.PumpBlockEntity)level.getBlockEntity(start.east(2));
            pump.setSpeed(128);pump.onSpeedChanged(0);pump.updatePipesOnSide(Direction.EAST);pump.updatePipesOnSide(Direction.WEST);
        });
        h.runAfterDelay(100,()->{
            var tank=(EssentiaTankBlockEntity)level.getBlockEntity(start.east(4));
            var fluid=tank.getControllerBE().getTankInventory().getFluid();
            h.assertTrue(fluid.getAmount()>0,"Create pump moved no essentia");
            h.assertTrue("aer".equals(EssentiaFluid.aspect(fluid)),"Pump discarded aspect payload");
            h.assertTrue(fluid.getAmount()+adapter.buffer.getFluidAmount()==8000,"Pump duplicated or lost essentia");
            h.succeed();
        });
    }
}
