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
    public static final int FUEL_SUCTION = 128;
    public static final int FUEL_CAPACITY = 256;
    private int fuelQuarters;
    private int paidTicks;
    // Tenths of an aspect quarter: integer ledger preserves partial intervals exactly.
    // A final sub-tick credit may go slightly negative and is settled by the next payment.
    private int paidEnergy;
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
            { scale = .25f; }
            protected Vec3 getSouthLocation(){return new Vec3(.5,.5,1.01);}
            protected boolean isSideActive(BlockState state,Direction side){
                return state.getValue(EssentiaEngineBlock.UPPER)
                        && side.getAxis()==state.getValue(EssentiaEngineBlock.HORIZONTAL_FACING).getClockWise().getAxis();
            }
        }) {
            @Override public ValueSettingsBoard createBoard(net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
                return new ValueSettingsBoard(label,128,8,List.of(Component.literal("RPM")),new EngineSpeedFormatter());
            }
        }.between(1,128).withFormatter(v->v+" RPM").withCallback(v->{
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
        if (paidTicks > 0) {
            paidTicks--;
            paidEnergy -= costQuarters(paidRpm);
        }
        if (paidTicks == 0 || paidEnergy <= 0) {
            int requested = rpm.getValue();
            int payment = Math.min(fuelQuarters, costQuarters(requested));
            if (payment > 0) {
                fuelQuarters -= payment;
                paidEnergy += payment * 10;
                paidRpm = requested;
            }
            paidTicks = paidEnergy > 0 ? 10 : 0;
            if (paidTicks == 0) paidRpm = 0;
            setChanged();
            sendData();
        }
        if(level.getBlockEntity(worldPosition.above()) instanceof EssentiaEngineBlockEntity top){
            if(top.rpm.getValue()!=rpm.getValue())top.rpm.setValue(rpm.getValue());
            if(top.getSpeed()!=getOutputSpeed())top.updateGeneratedRotation();
        }
    }
    private float getOutputSpeed(){return paidTicks>0&&paidEnergy>0?paidRpm:0;}
    @Override public float getGeneratedSpeed(){var b=base();return upper()&&b!=null?b.getOutputSpeed():0;}
    @Override public float calculateAddedStressCapacity(){lastCapacityProvided=16;return 16;}
    private void pull(){
        if(!ThaumicRebornApi.isAvailable()||freeEssentiaUnits()==0)return;
        for(Direction side:Direction.values()){
            if(!isConnectable(side)||freeEssentiaUnits()==0)continue;
            var remote=ThaumicRebornApi.essentia().neighbour(level,worldPosition,side).orElse(null);Direction opposite=side.getOpposite();
            if(remote!=null&&remote.canOutputTo(opposite)&&remote.suctionAmount(opposite)<FUEL_SUCTION&&remote.minimumSuction()<=FUEL_SUCTION&&"potentia".equals(remote.essentiaType(opposite))){
                int requested=Math.min(freeEssentiaUnits(),remote.essentiaAmount(opposite));
                if(requested>0){int taken=remote.takeEssentia("potentia",requested,opposite);if(taken>0)addEssentia("potentia",taken,side);}
            }
        }
    }
    private int storedEnergy() { return fuelQuarters * 10 + Math.max(0, paidEnergy); }
    private int freeEssentiaUnits() { return Math.max(0, (FUEL_CAPACITY * 40 - storedEnergy()) / 40); }
    public double storedEssentia() { var b=base(); return b==null?0:b.storedEnergy()/40.0; }
    public int storedQuarters(){var b=base();return b==null?0:b.fuelQuarters;}
    @Override public boolean addToGoggleTooltip(List<Component> lines,boolean sneak){var b=base();if(b==null)return false;lines.add(Component.literal(b.rpm.getValue()+" RPM | "+costText(b.rpm.getValue())+" Potentia / 10t"));lines.add(Component.literal("Potentia: "+b.storedEssentia()+" / "+FUEL_CAPACITY));return true;}
    public boolean isConnectable(Direction side){return !upper()&&side!=Direction.UP;}
    public boolean canInputFrom(Direction side){return isConnectable(side);}
    public boolean canOutputTo(Direction side){return false;}
    public void setSuction(String aspect,int value){}
    public String suctionType(Direction side){return isConnectable(side)?"potentia":null;}
    public int suctionAmount(Direction side){return isConnectable(side)&&freeEssentiaUnits()>0?FUEL_SUCTION:0;}
    public String essentiaType(Direction side){return isConnectable(side)&&storedEnergy()>0?"potentia":null;}
    public int essentiaAmount(Direction side){return isConnectable(side)?storedEnergy()/40:0;}
    public int minimumSuction(){return 0;}
    public int takeEssentia(String aspect,int amount,Direction side){return 0;}
    public int addEssentia(String aspect,int amount,Direction side){if(!canInputFrom(side)||!"potentia".equals(aspect)||amount<=0)return 0;int accepted=Math.min(amount,freeEssentiaUnits());fuelQuarters+=accepted*4;setChanged();if(level!=null&&!level.isClientSide)sendData();return accepted;}
    @Override protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider,boolean packet){super.read(tag,provider,packet);fuelQuarters=Math.max(0,Math.min(FUEL_CAPACITY*4,tag.getInt("FuelQuarters")));paidTicks=Math.max(0,Math.min(10,tag.getInt("PaidTicks")));paidRpm=Math.max(0,Math.min(128,tag.getInt("PaidRpm")));
        paidEnergy = tag.contains("PaidEnergy") ? Math.max(-31, Math.min(320, tag.getInt("PaidEnergy"))) : paidTicks * costQuarters(paidRpm);
        fuelQuarters = Math.min(fuelQuarters, (FUEL_CAPACITY * 40 - Math.max(0, paidEnergy)) / 10);
    }
    @Override public void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider,boolean packet){super.write(tag,provider,packet);tag.putInt("FuelQuarters",fuelQuarters);tag.putInt("PaidTicks",paidTicks);tag.putInt("PaidRpm",paidRpm);tag.putInt("PaidEnergy",paidEnergy);}
}
