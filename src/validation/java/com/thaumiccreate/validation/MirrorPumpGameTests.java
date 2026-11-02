package com.thaumiccreate.validation;

import com.thaumiccreate.*;
import com.thaumicreborn.api.ThaumicRebornApi;
import com.thaumicreborn.api.essentia.EssentiaTransport;
import com.thaumicreborn.registry.ModBlocks;
import com.thaumicreborn.mirror.MirrorLink;
import com.thaumicreborn.world.block.MagicMirrorBlock;
import com.thaumicreborn.world.block.entity.EssenceMirrorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("thaumic_create_tests") @PrefixGameTestTemplate(false)
public final class MirrorPumpGameTests {
    private record Setup(BlockPos pump,BlockPos receiver,BlockPos source,BlockPos localMirror,BlockPos remoteMirror) { }
    private static Setup setup(GameTestHelper h,Direction outlet,String aspect,boolean centrifuge) {
        var level=h.getLevel();
        var pump=h.absolutePos(new BlockPos(3,3,3));var receiver=pump.relative(outlet);
        var local=pump.relative(outlet.getOpposite(),3);var remote=pump.east(14);var source=remote.east(2);
        level.setBlockAndUpdate(pump,DeviceRegistry.PUMP.get().defaultBlockState().setValue(EssentiaPumpBlock.FACING,outlet));
        if(centrifuge) level.setBlockAndUpdate(receiver,ModBlocks.ESSENTIA_CENTRIFUGE.get().defaultBlockState());
        else {
            var engine=DeviceRegistry.ENGINE.get().defaultBlockState();
            level.setBlockAndUpdate(receiver,engine);
            level.setBlockAndUpdate(receiver.above(),engine.setValue(EssentiaEngineBlock.UPPER,true));
        }
        // Mirrors need solid backing; both source jars and mirror chunks are loaded.
        for(var p:new BlockPos[]{local,remote}) {
            level.setBlockAndUpdate(p.west(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(p,ModBlocks.ESSENTIA_MIRROR.get().defaultBlockState().setValue(MagicMirrorBlock.FACING,Direction.EAST));
        }
        var a=(EssenceMirrorBlockEntity)level.getBlockEntity(local);var b=(EssenceMirrorBlockEntity)level.getBlockEntity(remote);
        a.setDestination(MirrorLink.of(level,remote));b.setDestination(MirrorLink.of(level,local));
        level.setBlockAndUpdate(source,ModBlocks.WARDED_JAR.get().defaultBlockState());
        var tag=new CompoundTag();tag.putString("Aspect",aspect);tag.putInt("Amount",32);
        level.getBlockEntity(source).loadWithComponents(tag,level.registryAccess());
        return new Setup(pump,receiver,source,local,remote);
    }
    private static int amount(GameTestHelper h,BlockPos p) { return ((EssentiaTransport)h.getLevel().getBlockEntity(p)).essentiaAmount(Direction.UP); }
    @GameTest(template="mirror_pump_empty",timeoutTicks=160)
    public static void pumpFeedsEngineThroughLinkedMirrors(GameTestHelper h) {
        var s=setup(h,Direction.EAST,"potentia",false);
        h.runAfterDelay(50,()->{
            h.assertTrue(amount(h,s.source)<32,"Remote source never drained");
            var effectPump=(EssentiaPumpBlockEntity)h.getLevel().getBlockEntity(s.pump);
            h.assertTrue(s.localMirror.equals(effectPump.effectMirror()),"Transfer particles start at wrong mirror");
            h.assertTrue(effectPump.effectColor()==0xC0FFFF,"Transfer particles have wrong aspect colour");
            h.assertTrue(effectPump.effectUntil()>h.getLevel().getGameTime(),"Successful transfer did not start particles");
            h.assertTrue(effectPump.getUpdatePacket()!=null,"Particle effect was not synchronized to client");
            h.assertTrue(((EssentiaEngineBlockEntity)h.getLevel().getBlockEntity(s.receiver)).storedEssentia()>0,"Engine received no fuel");
            h.assertTrue(ThaumicRebornApi.essentia().transport(h.getLevel(),s.pump).orElse(null)==h.getLevel().getBlockEntity(s.pump),"Pump invisible to API");
            h.succeed();
        });
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=140)
    public static void closedPumpStopsAndReopensWithoutLoss(GameTestHelper h) {
        var s=setup(h,Direction.EAST,"potentia",false);var level=h.getLevel();
        var state=level.getBlockState(s.pump);level.setBlockAndUpdate(s.pump,state.setValue(EssentiaPumpBlock.OPEN,false));
        h.runAfterDelay(25,()->{
            h.assertTrue(amount(h,s.source)==32,"Closed pump drained source");
            h.assertTrue(((EssentiaPumpBlockEntity)level.getBlockEntity(s.pump)).effectMirror()==null,"Closed pump emitted a transfer effect");
            level.setBlockAndUpdate(s.pump,state);
        });
        h.runAfterDelay(50,()->{
            h.assertTrue(amount(h,s.source)<32,"Reopened pump did not resume");
            level.setBlockAndUpdate(s.pump,state.setValue(EssentiaPumpBlock.OPEN,false));
            int remaining=amount(h,s.source);
            var be=(EssentiaPumpBlockEntity)level.getBlockEntity(s.pump);
            var saved=be.saveWithoutMetadata(level.registryAccess());be.loadWithComponents(saved,level.registryAccess());
            h.runAfterDelay(20,()->{h.assertTrue(amount(h,s.source)==remaining,"Closed/reloaded pump drained source");h.succeed();});
        });
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=120)
    public static void untypedCentrifugeChoosesCompoundWithoutDrainingPrimals(GameTestHelper h) {
        var s=setup(h,Direction.UP,"potentia",true);var level=h.getLevel();
        var primal=s.source.south();level.setBlockAndUpdate(primal,ModBlocks.WARDED_JAR.get().defaultBlockState());
        var tag=new CompoundTag();tag.putString("Aspect","aer");tag.putInt("Amount",20);level.getBlockEntity(primal).loadWithComponents(tag,level.registryAccess());
        h.runAfterDelay(70,()->{
            h.assertTrue(amount(h,s.source)<32,"Untyped centrifuge did not pull compound");
            h.assertTrue(amount(h,primal)==20,"Pump drained rejected primal");h.succeed();
        });
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=100)
    public static void pumpRejectsJarsAndWrongPorts(GameTestHelper h) {
        var s=setup(h,Direction.EAST,"potentia",false);var level=h.getLevel();
        level.setBlockAndUpdate(s.receiver,ModBlocks.WARDED_JAR.get().defaultBlockState());
        var pump=(EssentiaPumpBlockEntity)level.getBlockEntity(s.pump);
        for(Direction side:Direction.values())h.assertTrue(pump.takeEssentia("potentia",1,side)==0,"Jar/wrong side took essence");
        h.runAfterDelay(30,()->{h.assertTrue(amount(h,s.source)==32,"Pump fed excluded jar");h.succeed();});
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=100)
    public static void missingAspectAndBrokenLinkDoNotDrain(GameTestHelper h) {
        var s=setup(h,Direction.EAST,"aer",false);var level=h.getLevel();
        h.runAfterDelay(25,()->{
            h.assertTrue(amount(h,s.source)==32,"Wrong aspect drained");
            var tag=new CompoundTag();tag.putString("Aspect","potentia");tag.putInt("Amount",32);level.getBlockEntity(s.source).loadWithComponents(tag,level.registryAccess());
            ((EssenceMirrorBlockEntity)level.getBlockEntity(s.remoteMirror)).invalidatePair();
        });
        h.runAfterDelay(50,()->{h.assertTrue(amount(h,s.source)==32,"Broken mirror link drained");h.succeed();});
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=160)
    public static void pumpFeedsActualTinkererRestorer(GameTestHelper h) {
        var s=setup(h,Direction.EAST,"instrumentum",false);var level=h.getLevel();
        level.setBlockAndUpdate(s.receiver.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(s.receiver,com.thaumictinkerer.machine.TTContent.RESTORER.get().defaultBlockState()
            .setValue(com.thaumictinkerer.machine.MachineBlock.FACING,Direction.WEST));
        var machine=(com.thaumictinkerer.machine.MachineEntity)level.getBlockEntity(s.receiver);
        var tool=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);tool.setDamageValue(100);machine.setItem(0,tool);
        h.runAfterDelay(60,()->{
            h.assertTrue(machine.getItem(0).getDamageValue()<100,"Actual TT restorer did not repair tool");
            h.assertTrue(amount(h,s.source)<32,"TT restorer did not drain mirror supply");h.succeed();
        });
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=160)
    public static void pumpFeedsActualHorizonsVat(GameTestHelper h) {
        var s=setup(h,Direction.EAST,"sano",false);var level=h.getLevel();
        level.setBlockAndUpdate(s.receiver,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(s.receiver.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var origin=s.receiver.north();
        for(int n=0;n<36;n++)level.setBlock(origin.offset(com.thaumichorizons.vat.VatStructure.offset(n)),
            com.thaumichorizons.vat.VatBlocks.VAT.get().defaultBlockState().setValue(com.thaumichorizons.vat.VatBlock.PART,n),2);
        var pig=net.minecraft.world.entity.EntityType.PIG.create(level);
        pig.moveTo(origin.getX()+1.5,origin.getY()+1,origin.getZ()+1.5);pig.setHealth(2);pig.setNoAi(true);level.addFreshEntity(pig);
        h.runAfterDelay(50,()->{
            h.assertTrue(pig.getHealth()>2,"Actual Horizons vat did not heal occupant");
            h.assertTrue(amount(h,s.source)<32,"Actual Horizons vat did not drain mirror supply");h.succeed();
        });
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=100)
    public static void pumpValveHitAndAllSixAttachmentFaces(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,2,2));
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        for(Direction facing:Direction.values()) {
            var state=DeviceRegistry.PUMP.get().defaultBlockState().setValue(EssentiaPumpBlock.FACING,facing);
            level.setBlockAndUpdate(pos,state);
            h.assertTrue(!state.getShape(level,pos).isEmpty(),"No shape for "+facing);
            // The outlet must meet a standard receiver at the exact face centre,
            // both with the valve open and closed. The former 1px gap failed here.
            double cx=.5+facing.getStepX()*.499,cy=.5+facing.getStepY()*.499,cz=.5+facing.getStepZ()*.499;
            var connection=net.minecraft.world.phys.shapes.Shapes.box(cx-.001,cy-.001,cz-.001,cx+.001,cy+.001,cz+.001);
            for(boolean open:new boolean[]{true,false})h.assertTrue(
                net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(state.setValue(EssentiaPumpBlock.OPEN,open).getShape(level,pos),connection,net.minecraft.world.phys.shapes.BooleanOp.AND),
                "Outlet does not meet receiver face centre: "+facing+", open="+open);

            var pump=(EssentiaPumpBlockEntity)level.getBlockEntity(pos);
            for(Direction side:Direction.values())h.assertTrue(pump.isConnectable(side)==(side==facing),"Exposed wrong attachment face");
            // A point on the wheel next to the receiver must toggle every orientation.
            double x=.5,y=.94,z=.9;
            double[] point=switch(facing) {
                case SOUTH -> new double[]{x,y,z}; case NORTH -> new double[]{1-x,y,1-z};
                case EAST -> new double[]{z,y,1-x}; case WEST -> new double[]{1-z,y,x};
                case UP -> new double[]{x,z,1-y}; case DOWN -> new double[]{x,1-z,y};
            };
            var wheelHit=new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(pos.getX()+point[0],pos.getY()+point[1],pos.getZ()+point[2]),facing,pos,false);
            h.assertTrue(DeviceRegistry.PUMP.get().toggleValve(state,level,pos,player,wheelHit),"Valve not next to receiver for "+facing);
            h.assertTrue(!level.getBlockState(pos).getValue(EssentiaPumpBlock.OPEN),"Valve did not close for "+facing);
            h.assertTrue(level.getBlockState(pos).getValue(EssentiaPumpBlock.FACING)==facing,"Valve rotated attachment");

        }
        var state=DeviceRegistry.PUMP.get().defaultBlockState();level.setBlockAndUpdate(pos,state);
        var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+.94,pos.getZ()+.4),Direction.UP,pos,false);
        h.assertTrue(DeviceRegistry.PUMP.get().toggleValve(state,level,pos,player,hit),"Valve click not recognised");
        h.assertTrue(!level.getBlockState(pos).getValue(EssentiaPumpBlock.OPEN),"Valve did not close");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void builtinResearchMirrorsAutoUnlockWithoutAdditionalRequirements(GameTestHelper h) {
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"LinkedResearch"));
        var knowledge=com.thaumicreborn.knowledge.KnowledgeAccess.get(player).orElseThrow();
        com.thaumicreborn.research.ResearchProgressService.reconcile(knowledge);
        h.assertTrue(!knowledge.hasCompletedResearch("create_essentia_mirror") && !knowledge.hasCompletedResearch("create_art_of_alchemy"),"Linked entries unlocked before originals");
        knowledge.completeResearch("mirroressentia");
        knowledge.completeResearch("alchemicalmanufacture");
        h.assertTrue(!knowledge.hasCompletedResearch("infusion"),"Test unexpectedly completed infusion");
        com.thaumicreborn.research.ResearchProgressService.reconcile(knowledge);
        h.assertTrue(knowledge.hasCompletedResearch("create_essentia_mirror"),"Mirror did not sync from original research");
        h.assertTrue(knowledge.hasCompletedResearch("create_art_of_alchemy"),"Alchemy did not sync from original research");
        h.succeed();
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=100)
    public static void pumpResearchAndRecipeAreRegistered(GameTestHelper h) {
        var mirror=com.thaumicreborn.research.ResearchRegistry.find("create_essentia_mirror").orElseThrow();
        var pump=com.thaumicreborn.research.ResearchRegistry.find("create_essentia_pump").orElseThrow();
        var original=com.thaumicreborn.research.ResearchRegistry.find("mirroressentia").orElseThrow();
        var page=mirror.pages().get(1); var originalPage=original.pages().get(1);
        h.assertTrue(page.type()==com.thaumicreborn.research.ResearchPageDefinition.Type.INFUSION, "Mirror page is not an infusion page");
        h.assertTrue(page.infusionDisplay().equals(originalPage.infusionDisplay()) && page.aspectCosts().equals(originalPage.aspectCosts()), "Mirror infusion display differs from original");
        h.assertTrue(mirror.concealed() && mirror.autoUnlock() && mirror.hiddenParents().contains("mirroressentia"),"Mirror research gate incorrect");
        h.assertTrue(pump.parents().contains("create_essentia_mirror"),"Pump parent incorrect");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumic_create","essentia_pump")).isPresent(),"Pump recipe missing");
        h.succeed();
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=180)
    public static void pumpStartsIdleThaumatoriumAndCraftsThroughFloorMirror(GameTestHelper h) {
        var s=setup(h,Direction.EAST,"potentia",false);var level=h.getLevel();
        level.setBlockAndUpdate(s.receiver.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(s.receiver,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(s.receiver.below(),ModBlocks.THAUMATORIUM_FOUNDATION.get().defaultBlockState());
        level.setBlockAndUpdate(s.receiver.below(2),ModBlocks.NITOR.get().defaultBlockState());
        var machineState=ModBlocks.THAUMATORIUM.get().defaultBlockState().setValue(com.thaumicreborn.world.block.AlchemyProcessorBlock.FACING,Direction.NORTH);
        level.setBlock(s.receiver,machineState.setValue(com.thaumicreborn.world.block.AlchemyProcessorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER),2);
        level.setBlock(s.receiver.above(),machineState.setValue(com.thaumicreborn.world.block.AlchemyProcessorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),2);
        level.setBlockAndUpdate(s.remoteMirror.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlock(s.remoteMirror,level.getBlockState(s.remoteMirror).setValue(MagicMirrorBlock.FACING,Direction.UP),2);
        level.setBlockAndUpdate(s.source,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var jars=new java.util.ArrayList<BlockPos>();
        int offset=0;
        for(String aspect:new String[]{"potentia","ignis","lux"}){
            var pos=s.remoteMirror.above().east(offset++);jars.add(pos);
            level.setBlockAndUpdate(pos,ModBlocks.WARDED_JAR.get().defaultBlockState());
            var tag=new CompoundTag();tag.putString("Aspect",aspect);tag.putInt("Amount",16);
            level.getBlockEntity(pos).loadWithComponents(tag,level.registryAccess());
        }
        var machine=(com.thaumicreborn.world.block.entity.AlchemyProcessorBlockEntity)level.getBlockEntity(s.receiver);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        com.thaumicreborn.knowledge.KnowledgeAccess.get(player).orElseThrow().completeResearch("nitor");
        h.runAfterDelay(2,()->{
            machine.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST));
            h.assertTrue(machine.selectRecipe(player,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumic_reborn","nitor")),"Nitor formula rejected");
            machine.setSuction(null,0);
            var pump=(EssentiaPumpBlockEntity)level.getBlockEntity(s.pump);
            h.assertTrue(pump.essentiaType(Direction.EAST)!=null,"Idle receiver cannot discover mirror supply");
            for(var pos:jars)h.assertTrue(amount(h,pos)==16,"Supply discovery drained jar");
            h.assertTrue(pump.effectMirror()==null,"Supply discovery emitted transfer particles without draining");
            level.setBlockAndUpdate(s.pump,level.getBlockState(s.pump).setValue(EssentiaPumpBlock.OPEN,false));
        });
        h.runAfterDelay(25,()->{
            for(var pos:jars)h.assertTrue(amount(h,pos)==16,"Closed pump drained jar");
            h.assertTrue(machine.reservedEssentia().isEmpty(),"Closed pump filled machine");
            level.setBlockAndUpdate(s.pump,level.getBlockState(s.pump).setValue(EssentiaPumpBlock.OPEN,true));
        });
        h.runAfterDelay(120,()->{
            h.assertTrue(machine.catalyst().isEmpty(),"Thaumatorium did not craft with mirror supply");
            for(var pos:jars)h.assertTrue(amount(h,pos)==13,"Recipe did not consume exactly 3 from each remote jar");
            h.succeed();
        });
    }

    @GameTest(template="mirror_pump_empty",timeoutTicks=460)
    public static void actualWorldBalancedShardsNeedHeatAndThenDrainAllFiveAspects(GameTestHelper h){
        var level=h.getLevel();var pumpPos=h.absolutePos(new BlockPos(5,4,10));var receiver=pumpPos.east();var local=pumpPos.west();var remote=local.offset(3,-1,-6);
        level.setBlockAndUpdate(receiver.below(2),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(receiver.below(),ModBlocks.THAUMATORIUM_FOUNDATION.get().defaultBlockState());
        var machineState=ModBlocks.THAUMATORIUM.get().defaultBlockState().setValue(com.thaumicreborn.world.block.AlchemyProcessorBlock.FACING,Direction.NORTH);
        level.setBlock(receiver,machineState.setValue(com.thaumicreborn.world.block.AlchemyProcessorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER),2);
        level.setBlock(receiver.above(),machineState.setValue(com.thaumicreborn.world.block.AlchemyProcessorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),2);
        level.setBlockAndUpdate(pumpPos,DeviceRegistry.PUMP.get().defaultBlockState().setValue(EssentiaPumpBlock.FACING,Direction.EAST));
        level.setBlockAndUpdate(local.west(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(local,ModBlocks.ESSENTIA_MIRROR.get().defaultBlockState().setValue(MagicMirrorBlock.FACING,Direction.EAST));
        level.setBlockAndUpdate(remote.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(remote,ModBlocks.ESSENTIA_MIRROR.get().defaultBlockState().setValue(MagicMirrorBlock.FACING,Direction.UP));
        ((EssenceMirrorBlockEntity)level.getBlockEntity(local)).setDestination(MirrorLink.of(level,remote));
        ((EssenceMirrorBlockEntity)level.getBlockEntity(remote)).setDestination(MirrorLink.of(level,local));
        var jars=new java.util.ArrayList<BlockPos>();int i=0;
        for(String aspect:new String[]{"ordo","ignis","aqua","terra","perditio"}){
            var pos=remote.offset(i%3-1,i/3,-2-i/3);i++;jars.add(pos);
            level.setBlockAndUpdate(pos,ModBlocks.WARDED_JAR.get().defaultBlockState());
            var tag=new CompoundTag();tag.putString("Aspect",aspect);tag.putInt("Amount",64);level.getBlockEntity(pos).loadWithComponents(tag,level.registryAccess());
        }
        var machine=(com.thaumicreborn.world.block.entity.AlchemyProcessorBlockEntity)level.getBlockEntity(receiver);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        com.thaumicreborn.knowledge.KnowledgeAccess.get(player).orElseThrow().completeResearch("crucible");
        h.runAfterDelay(2,()->{
            machine.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumic_reborn","air_shard")),5));
            h.assertTrue(machine.selectRecipe(player,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumic_reborn","balanced_air")),"Balanced shard formula rejected");
        });
        h.runAfterDelay(25,()->{
            h.assertTrue(machine.currentSuction()==null&&machine.reservedEssentia().isEmpty(),"Cold machine requested fuel");
            for(var pos:jars)h.assertTrue(amount(h,pos)==64,"Cold machine drained source");
            level.setBlockAndUpdate(receiver.below(2),ModBlocks.NITOR.get().defaultBlockState());
        });
        h.runAfterDelay(410,()->{
            h.assertTrue(machine.catalyst().isEmpty(),"Actual-world layout never completed five balanced shards after heating");
            for(var pos:jars)h.assertTrue(amount(h,pos)==54,"Expected exactly ten points from each of five jars");
            h.succeed();
        });
    }
    @GameTest(template="mirror_pump_empty",timeoutTicks=60)
    public static void pumpAnimationMatchesValveTubeTravel(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,2,2));
        var state=DeviceRegistry.PUMP.get().defaultBlockState().setValue(EssentiaPumpBlock.FACING,Direction.EAST).setValue(EssentiaPumpBlock.OPEN,false);
        level.setBlockAndUpdate(pos,state);var pump=(EssentiaPumpBlockEntity)level.getBlockEntity(pos);
        for(int tick=1;tick<=18;tick++){pump.animateValve();h.assertTrue(pump.valveRotation(1)==tick*20,"Valve must close at twenty degrees per tick");}
        h.assertTrue(pump.valveRotation(.5f)==350,"Valve interpolation differs from native tube");
        level.setBlockAndUpdate(pos,state.setValue(EssentiaPumpBlock.OPEN,true));
        for(int tick=1;tick<=18;tick++){pump.animateValve();h.assertTrue(pump.valveRotation(1)==360-tick*20,"Valve must reopen smoothly");}
        h.assertTrue(level.getBlockState(pos).getValue(EssentiaPumpBlock.FACING)==Direction.EAST,"Animation reoriented pump");h.succeed();
    }
}
