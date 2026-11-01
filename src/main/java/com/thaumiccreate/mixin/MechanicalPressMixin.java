package com.thaumiccreate.mixin;

import java.util.Optional;
import com.thaumiccreate.PressResearch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity", remap = false)
public abstract class MechanicalPressMixin {
    @Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true, remap = false)
    private void thaumicCreate$research(ItemStack input, CallbackInfoReturnable<Optional<net.minecraft.world.item.crafting.RecipeHolder<?>>> cir) {
        var found = cir.getReturnValue();
        if (found.isPresent() && !PressResearch.allowed((BlockEntity) (Object) this, found.get().id())) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
