package com.thaumiccreate;

import com.thaumicreborn.api.ThaumicRebornApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;

/** 125 mB is one indivisible Thaumic Reborn essentia unit. */
public final class EssentiaFluid {
    public static final int UNIT = 125;
    public static final int BLOCK_CAPACITY = 64 * UNIT;
    private EssentiaFluid() {}
    public static String aspect(FluidStack stack) {
        if (stack.isEmpty() || stack.getFluid() != DeviceRegistry.ESSENTIA.get()) return null;
        String value = stack.hasTag() ? stack.getTag().getString("Aspect") : "";
        return value.isBlank() ? null : value;
    }
    public static boolean valid(FluidStack stack) {
        String aspect = aspect(stack);
        return aspect != null && ThaumicRebornApi.isAvailable() && ThaumicRebornApi.aspects().find(aspect).isPresent();
    }
    public static FluidStack stack(String aspect, int millibuckets) {
        if (aspect == null || aspect.isBlank() || millibuckets <= 0) return FluidStack.EMPTY;
        FluidStack stack = new FluidStack(DeviceRegistry.ESSENTIA.get(), millibuckets);
        CompoundTag tag = new CompoundTag(); tag.putString("Aspect", aspect);
        stack.setTag(tag);
        return stack;
    }
    public static int color(FluidStack stack) {
        String aspect = aspect(stack);
        return aspect != null && ThaumicRebornApi.isAvailable()
            ? ThaumicRebornApi.aspects().find(aspect).map(a -> 0xFF000000 | a.color()).orElse(0xFFAA66CC)
            : 0xFFAA66CC;
    }
}
