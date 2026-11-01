package com.thaumiccreate;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.thaumicreborn.api.ThaumicRebornApi;
import com.thaumicreborn.api.essentia.EssentiaTransport;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class EssentiaEngineBlockEntity extends GeneratingKineticBlockEntity implements EssentiaTransport {
    private int fuelQuarters;
    private int paidTicks;
    private int paidRpm;
    public ScrollValueBehaviour rpm;
    public EssentiaEngineBlockEntity(BlockPos p,BlockState s) { super(DeviceRegistry.ENGINE_ENTITY.get(),p,s); }
    public boolean upper() { return getBlockState().getValue(EssentiaEngineBlock.UPPER); }
    private EssentiaEngineBlockEntity base() {
        if(!upper()) return this;
        return level!=null&&level.getBlockEntity(worldPosition.below()) instanceof EssentiaEngineBlockEntity b&&!b.upper()?b:null;
    }
    public static int costQuarters(int speed) { return speed<8?1:speed<=16?2:speed<=32?4:speed<=64?8:speed<=96?16:32; }
    public static String costText(int speed) { return switch(costQuarters(speed)) { case 1->"0.25";case 2->"0.5";default->Integer.toString(costQuarters(speed)/4); }; }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rpm=new ScrollValueBehaviour(Component.translatable("engine.thaumic_create.speed"),this,new ValueBoxTransform.Sided(){
            protected Vec3 getSouthLocation(){return new Vec3(.5,.5,1.01);}
            protected boolean isSideActive(BlockState state,Direction side){return side==state.getValue(EssentiaEngineBlock.HORIZONTAL_FACING);}
        }) {
            @Override public ValueSettingsBoard createBoard(net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
                return new ValueSettingsBoard(label,128,8,List.of(Component.literal("RPM")),new ValueSettingsFormatter(v->Component.literal(v.value()+" RPM | "+costText(Math.max(1,v.value()))+" Potentia / 10t")));
            }
        }.between(1,128).withFormatter(v->v+" RPM | "+costText(v)+" Potentia / 10t").withCallback(v->{
            if(level==null||level.isClientSide)return;
            var b=base();if(b!=null&&b!=this&&b.rpm.getValue()!=v)b.rpm.setValue(v);
            setChanged();sendData();
        });
        rpm.setValue(8);behaviours.add(rpm);
    }
    @Override public void tick(){
        super.tick();
        if(level==null||level.isClientSide||upper())return;
        pull();
        if(paidTicks>0)paidTicks--;
        if(paidTicks==0){int requested=rpm.getValue();int cost=costQuarters(requested);if(fuelQuarters>=cost){fuelQuarters-=cost;paidTicks=10;paidRpm=requested;}else paidRpm=0;setChanged();sendData();}
        if(level.getBlockEntity(worldPosition.above()) instanceof EssentiaEngineBlockEntity top){
            if(top.rpm.getValue()!=rpm.getValue())top.rpm.setValue(rpm.getValue());
            if(top.getSpeed()!=getOutputSpeed())top.updateGeneratedRotation();
        }
    }
    private float getOutputSpeed(){return paidTicks>0?paidRpm:0;}
    @Override public float getGeneratedSpeed(){var b=base();return upper()&&b!=null?b.getOutputSpeed():0;}
    @Override public float calculateAddedStressCapacity(){lastCapacityProvided=16;return 16;}
    private void pull(){
        if(!ThaumicRebornApi.isAvailable()||fuelQuarters>252)return;
        for(Direction side:Direction.values()){
            if(!isConnectable(side)||fuelQuarters>252)continue;
            var remote=ThaumicRebornApi.essentia().neighbour(level,worldPosition,side).orElse(null);Direction opposite=side.getOpposite();
            if(remote!=null&&remote.canOutputTo(opposite)&&remote.suctionAmount(opposite)<32&&remote.minimumSuction()<=32&&"potentia".equals(remote.essentiaType(opposite))){
                int taken=remote.takeEssentia("potentia",1,opposite);if(taken>0)addEssentia("potentia",taken,side);
            }
        }
    }
    public int storedQuarters(){var b=base();return b==null?0:b.fuelQuarters;}
    @Override public boolean addToGoggleTooltip(List<Component> lines,boolean sneak){var b=base();if(b==null)return false;lines.add(Component.literal(b.rpm.getValue()+" RPM | "+costText(b.rpm.getValue())+" Potentia / 10t"));lines.add(Component.literal("Potentia: "+(b.fuelQuarters/4.0)+" / 64"));return true;}
    public boolean isConnectable(Direction side){return !upper()&&side!=Direction.UP;}
    public boolean canInputFrom(Direction side){return isConnectable(side);}
    public boolean canOutputTo(Direction side){return false;}
    public void setSuction(String aspect,int value){}
    public String suctionType(Direction side){return isConnectable(side)?"potentia":null;}
    public int suctionAmount(Direction side){return isConnectable(side)&&fuelQuarters<=252?32:0;}
    public String essentiaType(Direction side){return isConnectable(side)&&fuelQuarters>0?"potentia":null;}
    public int essentiaAmount(Direction side){return isConnectable(side)?fuelQuarters/4:0;}
    public int minimumSuction(){return 0;}
    public int takeEssentia(String aspect,int amount,Direction side){return 0;}
    public int addEssentia(String aspect,int amount,Direction side){if(!canInputFrom(side)||!"potentia".equals(aspect)||amount<=0)return 0;int accepted=Math.min(amount,(256-fuelQuarters)/4);fuelQuarters+=accepted*4;setChanged();if(level!=null&&!level.isClientSide)sendData();return accepted;}
    @Override protected void read(CompoundTag tag,boolean packet){super.read(tag,packet);fuelQuarters=Math.max(0,Math.min(256,tag.getInt("FuelQuarters")));paidTicks=Math.max(0,Math.min(10,tag.getInt("PaidTicks")));paidRpm=Math.max(0,Math.min(128,tag.getInt("PaidRpm")));}
    @Override public void write(CompoundTag tag,boolean packet){super.write(tag,packet);tag.putInt("FuelQuarters",fuelQuarters);tag.putInt("PaidTicks",paidTicks);tag.putInt("PaidRpm",paidRpm);}
}
