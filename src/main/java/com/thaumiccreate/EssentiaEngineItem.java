package com.thaumiccreate;
public final class EssentiaEngineItem extends net.minecraft.world.item.BlockItem {
 public EssentiaEngineItem(net.minecraft.world.level.block.Block block,net.minecraft.world.item.Item.Properties properties){super(block,properties);}
 @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){
  super.appendHoverText(stack,context,lines,flag);
  if(net.minecraft.client.gui.screens.Screen.hasShiftDown()){
   lines.add(net.minecraft.network.chat.Component.translatable("engine.thaumic_create.supported_fuels"));
   for(var aspect:EngineItemData.fuels(EngineItemData.read(stack)))lines.add(net.minecraft.network.chat.Component.translatable("tc.aspect."+aspect));
  }else lines.add(net.minecraft.network.chat.Component.translatable("engine.thaumic_create.hold_shift"));
 }
}
