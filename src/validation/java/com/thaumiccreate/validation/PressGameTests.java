package com.thaumiccreate.validation;

import com.mojang.authlib.GameProfile;
import com.thaumiccreate.PressResearch;
import com.thaumicreborn.knowledge.KnowledgeAccess;
import com.thaumicreborn.research.ResearchPurchaseService;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("thaumic_create_tests")
@PrefixGameTestTemplate(false)
public final class PressGameTests {
    @GameTest(template="empty")
    public static void thaumiumPurchaseGatesRealPress(GameTestHelper helper) { check(helper, false); }
    @GameTest(template="empty")
    public static void voidPurchaseGatesRealPress(GameTestHelper helper) { check(helper, true); }
    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    private static void check(GameTestHelper h, boolean isVoid) {
        String plate = isVoid ? "plate_void" : "plate_thaumium";
        String metal = isVoid ? "void_metal_ingot" : "thaumium_ingot";
        String research = "create_" + plate;
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        h.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(id("create:mechanical_press")).defaultBlockState());
        var press = (MechanicalPressBlockEntity) h.getLevel().getBlockEntity(pos);
        var ingot = new ItemStack(BuiltInRegistries.ITEM.get(id("thaumic_reborn:"+metal)), 3);
        h.assertTrue(press.getRecipe(ingot).isEmpty(), "Unowned press bypassed purchase");
        var owner = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "PressOwner"));
        var knowledge = KnowledgeAccess.get(owner).orElseThrow();
        knowledge.completeResearch("first_steps");
        knowledge.completeResearch("create_integration");
        for (String aspect : new String[]{"metallum", "praecantatio", "vacuos"}) knowledge.addAspectPoints(aspect, 10);
        PressResearch.bind(press, owner);
        h.assertTrue(press.getRecipe(ingot).isEmpty(), "Locked owner could press plates");
        h.assertTrue(ResearchPurchaseService.purchase(owner,research) != ResearchPurchaseService.Result.PURCHASED, "Purchase ignored metal prerequisite");
        knowledge.completeResearch(isVoid ? "voidmetal" : "thaumium");
        com.thaumicreborn.research.ResearchProgressService.reconcile(knowledge);
        var result = ResearchPurchaseService.purchase(owner, research);
        h.assertTrue(result == ResearchPurchaseService.Result.PURCHASED, "Purchase failed: "+result);
        h.assertTrue(knowledge.aspectAmount("metallum")==6 && knowledge.aspectAmount("praecantatio")==8
            && knowledge.aspectAmount("vacuos")== (isVoid ? 8 : 10), "Wrong aspect charge");
        PressResearch.bind(press, owner);
        var recipe = press.getRecipe(ingot);
        h.assertTrue(recipe.isPresent(), "Purchased research did not unlock the real Create recipe");
        var output = recipe.orElseThrow().value().getResultItem(h.getLevel().registryAccess());
        h.assertTrue(BuiltInRegistries.ITEM.getKey(output.getItem()).equals(id("thaumic_create:"+plate)) && output.getCount()==1, "Wrong plate result");
        h.assertTrue(ingot.getCount()==3, "Recipe checks consumed input");
        var stranger=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"Stranger"));
        PressResearch.bind(press,stranger);
        h.assertTrue(press.getRecipe(ingot).isPresent(), "Another player stole ownership");
        var other=new ItemStack(BuiltInRegistries.ITEM.get(id("thaumic_reborn:"+(isVoid ? "thaumium_ingot" : "void_metal_ingot"))));
        h.assertTrue(press.getRecipe(other).isEmpty(), "Purchase unlocked the other metal");
        h.assertTrue(press.getRecipe(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)).isPresent(), "Ordinary Create pressing was blocked");
        h.succeed();
    }
}
