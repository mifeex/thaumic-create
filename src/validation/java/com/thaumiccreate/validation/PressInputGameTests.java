package com.thaumiccreate.validation;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.thaumiccreate.PressEvents;
import com.thaumicreborn.knowledge.KnowledgeAccess;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.UUID;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@GameTestHolder("thaumic_create_tests") @PrefixGameTestTemplate(false)
public final class PressInputGameTests {
    @GameTest(template="empty", timeoutTicks=200)
    public static void oldPressUsesResearchOfPlayerPlacingIngot(GameTestHelper h) {
        var level=h.getLevel(); var depot=h.absolutePos(new BlockPos(1,2,1));
        level.setBlockAndUpdate(depot,com.simibubi.create.AllBlocks.DEPOT.get().defaultBlockState());
        level.setBlockAndUpdate(depot.above(2),com.simibubi.create.AllBlocks.MECHANICAL_PRESS.get().defaultBlockState());
        var press=(MechanicalPressBlockEntity)level.getBlockEntity(depot.above(2));
        var ingot=new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumic_reborn:thaumium_ingot")));
        var untrained=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"Untrained"));
        var trained=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"Trained"));
        KnowledgeAccess.get(trained).orElseThrow().completeResearch("create_plate_thaumium");
        var hit=new BlockHitResult(Vec3.atCenterOf(depot),Direction.UP,depot,false);
        untrained.setItemInHand(InteractionHand.MAIN_HAND,ingot.copy());
        trained.setItemInHand(InteractionHand.MAIN_HAND,ingot.copy());
        PressEvents.interact(new PlayerInteractEvent.RightClickBlock(untrained,InteractionHand.MAIN_HAND,depot,hit));
        h.assertTrue(press.getRecipe(ingot).isEmpty(),"Untrained player could press an ingot");
        PressEvents.interact(new PlayerInteractEvent.RightClickBlock(trained,InteractionHand.MAIN_HAND,depot,hit));
        h.assertTrue(press.getRecipe(ingot).isPresent(),"Trained player could not use the existing press");
        PressEvents.interact(new PlayerInteractEvent.RightClickBlock(untrained,InteractionHand.MAIN_HAND,depot,hit));
        h.assertTrue(press.getRecipe(ingot).isEmpty(),"Previous player's research leaked to next player");
        PressEvents.interact(new PlayerInteractEvent.RightClickBlock(trained,InteractionHand.MAIN_HAND,depot,hit));
        var depotEntity=(com.simibubi.create.content.logistics.depot.DepotBlockEntity)level.getBlockEntity(depot);
        h.runAfterDelay(5,()->{
            press.setSpeed(64); press.onSpeedChanged(0); depotEntity.setHeldItem(ingot.copy());
        });
        h.runAfterDelay(130,()->{
            var result=depotEntity.getHeldItem();
            h.assertTrue(result.getItem()==BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumic_create:plate_thaumium")), "Press did not process depot ingot: "+result);
            h.succeed();
        });
    }
}
