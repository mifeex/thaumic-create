package com.thaumiccreate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
public final class EngineItemData {
 public static CompoundTag read(ItemStack stack){return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();}
 public static void write(ItemStack stack,CompoundTag tag){stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag.copy()));}
 public static java.util.List<String> fuels(CompoundTag tag){
  var list=new java.util.ArrayList<String>();list.add("potentia");
  if(tag.getBoolean("EngineArbor"))list.add("arbor");
  if(tag.getBoolean("EngineMagic"))list.add("praecantatio");return list;
 }
}
