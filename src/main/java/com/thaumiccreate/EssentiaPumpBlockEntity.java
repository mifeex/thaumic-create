package com.thaumiccreate;

import com.thaumicreborn.api.ThaumicRebornApi;
import com.thaumicreborn.api.essentia.EssentiaTransport;
import com.thaumicreborn.essentia.EssenceAirHandler;
import com.thaumicreborn.world.block.entity.EssenceMirrorBlockEntity;
import com.thaumicreborn.world.block.entity.EssenceJarContainer;
import com.thaumicreborn.world.block.MagicMirrorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Demand and delivery use the public transport contract; only mirror access is TR-specific.
 * An untyped receiver sees available remote aspects in rotation and chooses what to pull.
 * Discovery never drains a source. Only a successful receiver pull drains a mirror.
 */
public final class EssentiaPumpBlockEntity extends BlockEntity implements EssentiaTransport {
    public static final int RANGE=8;
    private float valveRotation,previousValveRotation;
    public void animateValve(){
        previousValveRotation=valveRotation;
        valveRotation=getBlockState().getValue(EssentiaPumpBlock.OPEN)?Math.max(0,valveRotation-20):Math.min(360,valveRotation+20);
    }
    public float valveRotation(float partialTick){return previousValveRotation+(valveRotation-previousValveRotation)*partialTick;}
    public static final int TRANSFER_EFFECT_TICKS=15;
    private BlockPos effectMirror;
    private int effectColor=0xFFFFFF;
    private long effectUntil;
    public BlockPos effectMirror(){return effectMirror;}
    public int effectColor(){return effectColor;}
    public long effectUntil(){return effectUntil;}
    private void transferredFrom(EssenceMirrorBlockEntity mirror,String aspect){
        effectMirror=mirror.getBlockPos().immutable();
        effectColor=ThaumicRebornApi.aspects().find(aspect).map(a->a.color()).orElse(0xFFFFFF);
        effectUntil=level.getGameTime()+TRANSFER_EFFECT_TICKS;
        level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    private net.minecraft.nbt.CompoundTag transferUpdate(){
        var tag=new net.minecraft.nbt.CompoundTag();
        if(effectMirror!=null)tag.putLong("TransferMirror",effectMirror.asLong());
        tag.putInt("TransferColor",effectColor);tag.putLong("TransferUntil",effectUntil);return tag;
    }
    private void readTransfer(net.minecraft.nbt.CompoundTag tag){
        effectMirror=tag.contains("TransferMirror")?BlockPos.of(tag.getLong("TransferMirror")):null;
        effectColor=tag.getInt("TransferColor");effectUntil=tag.getLong("TransferUntil");
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){return transferUpdate();}
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){super.loadAdditional(tag,registries);readTransfer(tag);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    private long nextTransfer;
    private List<BlockPos> mirrors=List.of();
    private long nextScan;
    private boolean transferring;
    private long availableTick=Long.MIN_VALUE;
    private List<String> availableAspects=List.of();
    public EssentiaPumpBlockEntity(BlockPos pos, BlockState state) { super(DeviceRegistry.PUMP_ENTITY.get(),pos,state); }
    public Direction outlet() { return getBlockState().getValue(EssentiaPumpBlock.FACING); }
    private boolean open() { return level != null && getBlockState().getValue(EssentiaPumpBlock.OPEN); }
    private EssentiaTransport receiver() {
        if (!open() || !ThaumicRebornApi.isAvailable() || !level.hasChunkAt(worldPosition.relative(outlet()))) return null;
        var entity=level.getBlockEntity(worldPosition.relative(outlet()));
        if (entity instanceof EssenceJarContainer || entity instanceof EssentiaPumpBlockEntity) return null;
        var remote=ThaumicRebornApi.essentia().neighbour(level,worldPosition,outlet()).orElse(null);
        return remote != null && remote.isConnectable(outlet().getOpposite()) && (remote.canInputFrom(outlet().getOpposite()) || remote.suctionAmount(outlet().getOpposite())>0) ? remote : null;
    }
    private List<EssenceMirrorBlockEntity> mirrors() {
        if (!(level instanceof ServerLevel server)) return List.of();
        if (level.getGameTime()>=nextScan) {
            var candidates=new ArrayList<>(EssenceAirHandler.blockEntityPositions(server,worldPosition,RANGE));
            Direction intake=outlet().getOpposite();
            candidates.removeIf(p -> !(level.getBlockEntity(p) instanceof EssenceMirrorBlockEntity));
            candidates.sort(Comparator.<BlockPos>comparingInt(p -> {
                int dx=p.getX()-worldPosition.getX(),dy=p.getY()-worldPosition.getY(),dz=p.getZ()-worldPosition.getZ();
                int forward=dx*intake.getStepX()+dy*intake.getStepY()+dz*intake.getStepZ();
                return forward>0 && dx*dx+dy*dy+dz*dz==forward*forward ? 0 : 1;
            }).thenComparingDouble(p -> p.distSqr(worldPosition)));
            mirrors=List.copyOf(candidates); nextScan=level.getGameTime()+20;
        }
        return mirrors.stream().filter(level::hasChunkAt).map(level::getBlockEntity)
            .filter(EssenceMirrorBlockEntity.class::isInstance).map(EssenceMirrorBlockEntity.class::cast).toList();
    }
    private List<String> available() {
        if (!(level instanceof ServerLevel server)) return List.of();
        if(availableTick==level.getGameTime()) return availableAspects;
        Set<String> found=new TreeSet<>();
        for (var mirror:mirrors()) {
            if (!mirror.validReciprocalLink()) continue;
            var link=mirror.link(); var remoteLevel=link.level(server.getServer());
            if(remoteLevel==null || !remoteLevel.hasChunkAt(link.position()) || !(remoteLevel.getBlockEntity(link.position()) instanceof EssenceMirrorBlockEntity remote)) continue;
            Direction facing=remote.getBlockState().getValue(MagicMirrorBlock.FACING);
            for(var p:EssenceAirHandler.blockEntityPositions(remoteLevel,link.position(),EssenceMirrorBlockEntity.REMOTE_SOURCE_RANGE)) {
                int forward=(p.getX()-link.position().getX())*facing.getStepX()+(p.getY()-link.position().getY())*facing.getStepY()+(p.getZ()-link.position().getZ())*facing.getStepZ();
                if (forward<0 || forward>=EssenceMirrorBlockEntity.REMOTE_SOURCE_RANGE || p.equals(link.position())) continue;
                var source=ThaumicRebornApi.essentia().transport(remoteLevel,p).orElse(null);
                if(source==null || source instanceof EssentiaPumpBlockEntity) continue;
                for(Direction side:Direction.values()) {
                    String aspect=source.essentiaType(side);
                    if(source.canOutputTo(side) && source.essentiaAmount(side)>0 && aspect!=null) found.add(aspect);
                }
            }
        }
        availableTick=level.getGameTime();
        availableAspects=List.copyOf(found);
        return availableAspects;
    }
    private boolean acquire(String aspect) {
        if (!(level instanceof ServerLevel) || transferring || level.getGameTime()<nextTransfer) return false;
        transferring=true;
        try {
            for(var mirror:mirrors()) if(mirror.takeFromAir(aspect)) { nextTransfer=level.getGameTime()+5; availableTick=Long.MIN_VALUE; transferredFrom(mirror,aspect); return true; }
            return false;
        } finally { transferring=false; }
    }
    @Override public boolean isConnectable(Direction side) { return side==outlet(); }
    @Override public boolean canInputFrom(Direction side) { return false; }
    @Override public boolean canOutputTo(Direction side) { return isConnectable(side) && receiver()!=null && !transferring; }
    @Override public void setSuction(String aspect,int amount) { }
    @Override public String suctionType(Direction side) {
        // Some pull-only addon machines inspect the source suction type.
        if(!isConnectable(side)) return null;
        var target=receiver();
        return target==null?null:target.suctionType(side.getOpposite());
    }
    @Override public int suctionAmount(Direction side) { return 0; }
    @Override public boolean renderExtendedTube() { return false; }
    @Override public int minimumSuction() { return 0; }
    @Override public String essentiaType(Direction side) {
        if(!canOutputTo(side) || level.isClientSide) return null;
        var target=receiver();
        // Receivers such as the Thaumatorium discover supply BEFORE selecting suction.
        // Advertising is read-only; takeEssentia still requires active demand.

        var aspects=available();
        // Do not pin discovery to the receiver's previous demand: a multiaspect
        // machine must be able to discover its NEXT required aspect as well.
        return aspects.isEmpty()?null:aspects.get((int)Math.floorMod(level.getGameTime()/5,aspects.size()));
    }
    @Override public int essentiaAmount(Direction side) { return essentiaType(side)==null?0:1; }
    @Override public int takeEssentia(String aspect,int amount,Direction side) {
        if(amount<=0 || aspect==null || !canOutputTo(side) || level.isClientSide) return 0;
        var target=receiver(); String wanted=target.suctionType(side.getOpposite());
        if(target.suctionAmount(side.getOpposite())<=0 || wanted!=null && !wanted.equals(aspect)) return 0;
        return acquire(aspect)?1:0;
    }
    @Override public int addEssentia(String aspect,int amount,Direction side) { return 0; }
}
