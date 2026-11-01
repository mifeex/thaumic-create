package com.thaumiccreate;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.minecraft.resources.ResourceLocation;

final class TankConnectedTextures {
    private TankConnectedTextures() {}
    static void register() {
        var side = CTSpriteShifter.getCT(AllSpriteShifts.FLUID_TANK.getType(), id("essentia_tank"), id("essentia_tank_connected"));
        var top = CTSpriteShifter.getCT(AllSpriteShifts.FLUID_TANK_TOP.getType(), id("essentia_tank_top"), id("essentia_tank_top_connected"));
        var inner = CTSpriteShifter.getCT(AllSpriteShifts.FLUID_TANK_INNER.getType(), id("essentia_tank_inner"), id("essentia_tank_inner_connected"));
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(DeviceRegistry.TANK.get()),
            model -> new CTModel(model, new FluidTankCTBehaviour(side, top, inner)));
    }
    private static ResourceLocation id(String name) {
        return new ResourceLocation(ThaumicCreate.ID, "block/" + name);
    }
}
