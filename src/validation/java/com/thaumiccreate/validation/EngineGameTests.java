package com.thaumiccreate.validation;
import com.thaumiccreate.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("thaumic_create_tests") @PrefixGameTestTemplate(false)
public class EngineGameTests {
 @GameTest(template="empty",timeoutTicks=100)
 public static void infusionResearchRequiredForAddonInfusionEntries(GameTestHelper h) {
  var ids=new String[]{"create_essentia_engine","create_engine_arbor","create_engine_praecantatio"};
  for (String id:ids) {
   var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"InfusionGate"));
   var knowledge=com.thaumicreborn.knowledge.KnowledgeAccess.get(player).orElseThrow();
   var entry=com.thaumicreborn.research.ResearchRegistry.find(id).orElseThrow();
   h.assertTrue(entry.hiddenParents().contains("infusion"),id+" has no explicit infusion prerequisite");
   for(String parent:entry.parents()) knowledge.completeResearch(parent);
   for(String parent:entry.hiddenParents()) if(!parent.equals("infusion")) knowledge.completeResearch(parent);
   knowledge.revealResearch(id);
   h.assertTrue(!com.thaumicreborn.research.ResearchProgressService.isAvailable(entry,knowledge),id+" available without infusion");
   knowledge.completeResearch("infusion");
   h.assertTrue(com.thaumicreborn.research.ResearchProgressService.isAvailable(entry,knowledge),id+" still unavailable after infusion");
  }
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=100)
 public static void engineSidePortsHaveCollisionOnAllFacings(GameTestHelper h) {
  var p=h.absolutePos(new BlockPos(1,2,1)); var level=h.getLevel();
  for (Direction facing : new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}) {
   var state=DeviceRegistry.ENGINE.get().defaultBlockState().setValue(EssentiaEngineBlock.HORIZONTAL_FACING,facing);
   level.setBlockAndUpdate(p,state);
   var shape=state.getShape(level,p);
   int turns=(facing.get2DDataValue()+2)%4;
   double x=.0625,z=.5;
   for(int n=0;n<turns;n++){double old=x;x=1-z;z=old;}
   var probe=net.minecraft.world.phys.shapes.Shapes.box(x-.01,.32,z-.01,x+.01,.34,z+.01);
   h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,probe,net.minecraft.world.phys.shapes.BooleanOp.AND),"Missing new port on "+facing);
  }
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=200)
 public static void engineConsumesExactFractionsAndDrivesShaft(GameTestHelper h){
  h.assertTrue(com.thaumicreborn.research.ResearchRegistry.find("create_essentia_engine").isPresent(),"Engine research missing");
  var p=h.absolutePos(new BlockPos(1,2,1));var l=h.getLevel();var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  var base=(EssentiaEngineBlockEntity)l.getBlockEntity(p);var top=(EssentiaEngineBlockEntity)l.getBlockEntity(p.above());
  h.runAfterDelay(2,()->{
   h.assertTrue(top.getGeneratedSpeed()==0,"Unfuelled engine running");
   h.assertTrue(base.rpm.getValue()==8,"Wrong initial RPM");
   h.assertTrue(base.addEssentia("ignis",1,Direction.WEST)==0,"Accepted wrong fuel");
   h.assertTrue(base.addEssentia("potentia",1,Direction.WEST)==1,"Fuel rejected");
  });
  h.runAfterDelay(3,()->{h.assertTrue(top.getGeneratedSpeed()==8,"No generated rotation");h.assertTrue(base.storedQuarters()==2,"Wrong 0.5 unit charge");});
  h.runAfterDelay(40,()->h.assertTrue(base.storedQuarters()==2,"Charged before 40 tick interval"));
  h.runAfterDelay(44,()->h.assertTrue(base.storedQuarters()==0,"Fractional charges rounded incorrectly"));
  h.runAfterDelay(84,()->{h.assertTrue(top.getGeneratedSpeed()==0,"Engine kept running without fuel");
   h.assertTrue(EssentiaEngineBlockEntity.costQuarters(7)==1&&EssentiaEngineBlockEntity.costQuarters(8)==2&&EssentiaEngineBlockEntity.costQuarters(16)==2&&EssentiaEngineBlockEntity.costQuarters(17)==4&&EssentiaEngineBlockEntity.costQuarters(33)==8&&EssentiaEngineBlockEntity.costQuarters(65)==16&&EssentiaEngineBlockEntity.costQuarters(97)==32&&EssentiaEngineBlockEntity.costQuarters(128)==32,"Tariff boundaries wrong");
   base.rpm.setValue(128);base.addEssentia("potentia",8,Direction.WEST);
  });
  h.runAfterDelay(85,()->{h.assertTrue(top.getGeneratedSpeed()==128,"Maximum speed failed");h.assertTrue(base.storedQuarters()==0,"Maximum charge was not 8 units");
   var saved=base.saveWithoutMetadata(l.registryAccess());base.loadWithComponents(saved,l.registryAccess());
   h.assertTrue(base.rpm.getValue()==128,"Speed lost on save");h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=160)
 public static void enginePullsFromFilteredJarThroughTubes(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));
  var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  var jarPos=p.west(2).below();
  l.setBlockAndUpdate(jarPos,com.thaumicreborn.registry.ModBlocks.WARDED_JAR.get().defaultBlockState());
  var jar=l.getBlockEntity(jarPos);
  var payload=new net.minecraft.nbt.CompoundTag();payload.putString("Aspect","potentia");payload.putString("AspectFilter","potentia");payload.putInt("Amount",64);
  jar.loadWithComponents(payload, l.registryAccess());
  var tube=com.thaumicreborn.registry.ModBlocks.ESSENTIA_TUBE.get().defaultBlockState();
  l.setBlockAndUpdate(jarPos.above(),tube);l.setBlockAndUpdate(p.west(),tube);
  h.runAfterDelay(120,()->{
   var fuel=(com.thaumicreborn.api.essentia.EssentiaTransport)jar;
   h.assertTrue(fuel.essentiaAmount(Direction.UP)<64,"Filtered jar never drained through tubes");
   var top=(EssentiaEngineBlockEntity)l.getBlockEntity(p.above());
   h.assertTrue(top.getGeneratedSpeed()==8&&top.getSpeed()==8,"Engine did not start kinetic rotation");
   h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesSmallCogNorth(GameTestHelper h){checkCogMesh(h,Direction.NORTH,false);}
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesLargeCogNorth(GameTestHelper h){checkCogMesh(h,Direction.NORTH,true);}
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesSmallCogEast(GameTestHelper h){checkCogMesh(h,Direction.EAST,false);}
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesLargeCogEast(GameTestHelper h){checkCogMesh(h,Direction.EAST,true);}
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesSmallCogSouth(GameTestHelper h){checkCogMesh(h,Direction.SOUTH,false);}
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesLargeCogSouth(GameTestHelper h){checkCogMesh(h,Direction.SOUTH,true);}
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesSmallCogWest(GameTestHelper h){checkCogMesh(h,Direction.WEST,false);}
 @GameTest(template="empty",timeoutTicks=80)
 public static void engineMeshesLargeCogWest(GameTestHelper h){checkCogMesh(h,Direction.WEST,true);}
 private static void checkCogMesh(GameTestHelper h,Direction facing,boolean large){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));
  var state=DeviceRegistry.ENGINE.get().defaultBlockState().setValue(EssentiaEngineBlock.HORIZONTAL_FACING,facing);
  var upper=state.setValue(EssentiaEngineBlock.UPPER,true);
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),upper);
  var engineAxis=DeviceRegistry.ENGINE.get().getRotationAxis(upper);
  var cogAxis=large ? facing.getAxis() : engineAxis;
  var cogPos=p.above(2);
  var cog=(large ? com.simibubi.create.AllBlocks.LARGE_COGWHEEL : com.simibubi.create.AllBlocks.COGWHEEL).getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,cogAxis);
  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l);
  var stack=(large?com.simibubi.create.AllBlocks.LARGE_COGWHEEL:com.simibubi.create.AllBlocks.COGWHEEL).asStack();
  player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
  var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p.above()).add(0,.5,0),Direction.UP,p.above(),false);
  var context=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,stack,hit);
  var automatic=((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock().getStateForPlacement(context);
  h.assertTrue(automatic!=null&&automatic.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)==cogAxis,"Automatic placement chose disconnected axis");
  var helper=net.createmod.catnip.placement.PlacementHelpers.get(EngineCogPlacement.ID);
  var preview=helper.getOffset(player,l,upper,p.above(),hit,stack);
  h.assertTrue(preview.isSuccessful()&&preview.getBlockPos().equals(cogPos)&&preview.getGhostState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)==cogAxis,"Missing or incorrect cog placement preview");
  h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(upper.getCollisionShape(l,p.above()),net.minecraft.world.phys.shapes.Shapes.block(),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Collision still a full cube");
  h.assertTrue(cog.canSurvive(l,cogPos),"Cog placement rejected above engine");
  l.setBlockAndUpdate(cogPos,cog);
  h.runAfterDelay(2,()->((EssentiaEngineBlockEntity)l.getBlockEntity(p)).addEssentia("potentia",16,Direction.DOWN));
  h.runAfterDelay(12,()->{
   var engine=(EssentiaEngineBlockEntity)l.getBlockEntity(p.above());
   var cogBe=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)l.getBlockEntity(cogPos);
   h.assertTrue(cogBe!=null&&cogBe.getSpeed()==(large ? 8 : -8),"Cog teeth not driven by engine");
   h.assertTrue(com.simibubi.create.content.kinetics.RotationPropagator.isConnected(engine,cogBe)&&com.simibubi.create.content.kinetics.RotationPropagator.isConnected(cogBe,engine),"Cog mesh not symmetric");
   h.succeed();
  });
 }

 @GameTest(template="empty",timeoutTicks=180)
 public static void engineNeverStopsBetweenFuelPayments(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  h.runAfterDelay(2,()->((EssentiaEngineBlockEntity)l.getBlockEntity(p)).addEssentia("potentia",64,Direction.DOWN));
  for(int tick=4;tick<=130;tick++){
   final int sampled=tick;
   h.runAfterDelay(tick,()->{
    var top=(EssentiaEngineBlockEntity)l.getBlockEntity(p.above());
    h.assertTrue(top.getGeneratedSpeed()==8&&top.getSpeed()==8,"Rotation interrupted at tick "+sampled+": generated="+top.getGeneratedSpeed()+", actual="+top.getSpeed());
   });
  }
  h.runAfterDelay(132,h::succeed);
 }

 @GameTest(template="empty",timeoutTicks=100)
 public static void engineUsesLastSixPotentiaAtMaximumSpeed(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  h.runAfterDelay(2,()->{
   var base=(EssentiaEngineBlockEntity)l.getBlockEntity(p);base.rpm.setValue(128);
   base.addEssentia("potentia",6,Direction.DOWN);
  });
  for(int tick=4;tick<=9;tick++){
   final int sampled=tick;
   h.runAfterDelay(tick,()->{
    var top=(EssentiaEngineBlockEntity)l.getBlockEntity(p.above());
    h.assertTrue(top.getGeneratedSpeed()==128&&top.getSpeed()==128,"Remaining fuel not used at tick "+sampled);
   });
  }
  h.runAfterDelay(38,()->{
   var base=(EssentiaEngineBlockEntity)l.getBlockEntity(p);var top=(EssentiaEngineBlockEntity)l.getBlockEntity(p.above());
   h.assertTrue(base.storedQuarters()==0,"Fuel remained stranded");
   h.assertTrue(top.getGeneratedSpeed()==0&&top.getSpeed()==0,"Empty engine still rotating");
   var saved=base.saveWithoutMetadata(l.registryAccess());
   h.assertTrue(saved.getInt("PaidEnergy")==0,"Partial tick accounting lost precision");
   base.loadWithComponents(saved,l.registryAccess());
   h.assertTrue(base.saveWithoutMetadata(l.registryAccess()).getInt("PaidEnergy")==0,"Saved fractional balance changed");
   h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=120)
 public static void maximumSpeedRemainsContinuousWithEnoughFuel(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  h.runAfterDelay(2,()->{var base=(EssentiaEngineBlockEntity)l.getBlockEntity(p);base.rpm.setValue(128);base.addEssentia("potentia",64,Direction.DOWN);});
  for(int tick=4;tick<=80;tick++){
   final int sampled=tick;
   h.runAfterDelay(tick,()->{
    var top=(EssentiaEngineBlockEntity)l.getBlockEntity(p.above());
    h.assertTrue(top.getGeneratedSpeed()==128&&top.getSpeed()==128,"Maximum rotation interrupted at tick "+sampled);
   });
  }
  h.runAfterDelay(82,h::succeed);
 }

 @GameTest(template="empty",timeoutTicks=180)
 public static void engineBulkLoadsReplacementJarsAndRefillsWithoutOverflow(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  var jarPos=p.below();
  Runnable freshJar=()->{
   l.setBlockAndUpdate(jarPos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
   l.setBlockAndUpdate(jarPos,com.thaumicreborn.registry.ModBlocks.WARDED_JAR.get().defaultBlockState());
   var payload=new net.minecraft.nbt.CompoundTag();payload.putString("Aspect","potentia");payload.putInt("Amount",64);
   l.getBlockEntity(jarPos).loadWithComponents(payload,l.registryAccess());
  };
  freshJar.run();
  for(int replacement=0;replacement<4;replacement++){
   final int count=replacement+1;
   if(replacement>0)h.runAfterDelay(replacement*4,()->freshJar.run());
   h.runAfterDelay(replacement*4+3,()->{
    var source=(com.thaumicreborn.api.essentia.EssentiaTransport)l.getBlockEntity(jarPos);
    var engine=(EssentiaEngineBlockEntity)l.getBlockEntity(p);
    h.assertTrue(source.essentiaAmount(Direction.UP)==0,"Jar "+count+" was not drained in bulk");
    h.assertTrue(engine.storedEssentia()>count*64-1,"Reserve did not accumulate jar "+count+": "+engine.storedEssentia());
   });
  }
  h.runAfterDelay(16,()->freshJar.run());
  for(int tick=17;tick<=140;tick++)h.runAfterDelay(tick,()->{
   var engine=(EssentiaEngineBlockEntity)l.getBlockEntity(p);
   h.assertTrue(engine.storedEssentia()<=256,"Reserve exceeded 256 including prepaid fuel");
  });
  h.runAfterDelay(140,()->{
   var source=(com.thaumicreborn.api.essentia.EssentiaTransport)l.getBlockEntity(jarPos);
   var engine=(EssentiaEngineBlockEntity)l.getBlockEntity(p);
   h.assertTrue(source.essentiaAmount(Direction.UP)<64,"No automatic top-up after consuming reserve");
   h.assertTrue(engine.storedEssentia()>254,"Reserve was not kept topped up");
   var saved=engine.saveWithoutMetadata(l.registryAccess());double before=engine.storedEssentia();
   engine.loadWithComponents(saved,l.registryAccess());
   h.assertTrue(engine.storedEssentia()==before,"Reserve changed on save/load");
   h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=50)
 public static void engineReserveCapIncludesPaidFuel(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  h.runAfterDelay(2,()->{
   var engine=(EssentiaEngineBlockEntity)l.getBlockEntity(p);engine.rpm.setValue(128);
   h.assertTrue(engine.addEssentia("potentia",1000,Direction.DOWN)==256,"Wrong initial reserve capacity");
  });
  h.runAfterDelay(3,()->{
   var engine=(EssentiaEngineBlockEntity)l.getBlockEntity(p);
   h.assertTrue(engine.storedEssentia()<=256,"Paid interval exceeds total cap");
   h.assertTrue(engine.addEssentia("potentia",8,Direction.DOWN)==0,"Prepaid fuel not counted towards reserve");
   h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=80)
 public static void infusionFuelUpgradesStackAndPreserveReserves(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var state=DeviceRegistry.ENGINE.get().defaultBlockState();
  l.setBlockAndUpdate(p,state);l.setBlockAndUpdate(p.above(),state.setValue(EssentiaEngineBlock.UPPER,true));
  h.runAfterDelay(2,()->{
   var original=new net.minecraft.world.item.ItemStack(DeviceRegistry.ENGINE_ITEM.get());
   original.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Upgraded generator"));
   h.assertTrue(com.thaumicreborn.research.ResearchRegistry.find("create_engine_arbor").isPresent()&&com.thaumicreborn.research.ResearchRegistry.find("create_engine_praecantatio").isPresent(),"Upgrade book nodes missing");
   var wood=com.thaumicreborn.infusion.InfusionRecipeRegistry.find(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumic_create","engine_arbor")).orElseThrow();
   var magic=com.thaumicreborn.infusion.InfusionRecipeRegistry.find(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumic_create","engine_praecantatio")).orElseThrow();
   h.assertTrue(wood.instability()==2&&magic.instability()==2,"Upgrade instability is not low");
   var woodItem=wood.createResult(original,l.registryAccess());var both=magic.createResult(woodItem,l.registryAccess());
   var reverse=wood.createResult(magic.createResult(original,l.registryAccess()),l.registryAccess());
   h.assertTrue(EngineItemData.fuels(EngineItemData.read(both)).size()==3&&EngineItemData.fuels(EngineItemData.read(reverse)).size()==3,"Upgrade erased previous fuel support");
   h.assertTrue(both.getHoverName().getString().equals("Upgraded generator"),"Infusion lost item name");
   var engine=(EssentiaEngineBlockEntity)l.getBlockEntity(p);engine.restoreEngineItem(EngineItemData.read(both));
   h.assertTrue(engine.addEssentia("potentia",8,Direction.DOWN)==8,"Default fuel missing");
   engine.cycleFuel();h.assertTrue(engine.activeFuel().equals("arbor"),"Arbor selection failed");
   h.assertTrue(engine.addEssentia("potentia",1,Direction.DOWN)==0&&engine.addEssentia("arbor",6,Direction.DOWN)==6,"Wrong fuel accepted");
   engine.cycleFuel();h.assertTrue(engine.activeFuel().equals("praecantatio")&&engine.addEssentia("praecantatio",4,Direction.DOWN)==4,"Magic fuel missing");
   engine.cycleFuel();h.assertTrue(engine.storedAspects().size()==3&&engine.storedEssentia()==8,"Switch lost fuel");
   var saved=engine.engineItemData();engine.restoreEngineItem(saved);h.assertTrue(engine.storedAspects().get("arbor")==6&&engine.storedAspects().get("praecantatio")==4,"Item roundtrip lost reserves");
   h.succeed();
  });
 }
}
