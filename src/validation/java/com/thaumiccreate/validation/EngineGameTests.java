package com.thaumiccreate.validation;
import com.thaumiccreate.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("thaumic_create_tests") @PrefixGameTestTemplate(false)
public class EngineGameTests {
 @GameTest(template="empty",timeoutTicks=100)
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
  h.runAfterDelay(14,()->h.assertTrue(base.storedQuarters()==0,"Fractional charges rounded incorrectly"));
  h.runAfterDelay(24,()->{h.assertTrue(top.getGeneratedSpeed()==0,"Engine kept running without fuel");
   h.assertTrue(EssentiaEngineBlockEntity.costQuarters(7)==1&&EssentiaEngineBlockEntity.costQuarters(8)==2&&EssentiaEngineBlockEntity.costQuarters(16)==2&&EssentiaEngineBlockEntity.costQuarters(17)==4&&EssentiaEngineBlockEntity.costQuarters(33)==8&&EssentiaEngineBlockEntity.costQuarters(65)==16&&EssentiaEngineBlockEntity.costQuarters(97)==32&&EssentiaEngineBlockEntity.costQuarters(128)==32,"Tariff boundaries wrong");
   base.rpm.setValue(128);base.addEssentia("potentia",8,Direction.WEST);
  });
  h.runAfterDelay(25,()->{h.assertTrue(top.getGeneratedSpeed()==128,"Maximum speed failed");h.assertTrue(base.storedQuarters()==0,"Maximum charge was not 8 units");
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
}
