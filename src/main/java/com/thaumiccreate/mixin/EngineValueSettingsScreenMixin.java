package com.thaumiccreate.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import com.thaumiccreate.EngineSpeedFormatter;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Engine-only footer; all other Create value boards retain their normal rendering. */
@Mixin(value = ValueSettingsScreen.class, remap = false)
public abstract class EngineValueSettingsScreenMixin extends AbstractSimiScreen {
    @Shadow private ValueSettingsBoard board;
    @Shadow public abstract ValueSettings getClosestCoordinate(int mouseX, int mouseY);

    @Inject(method = "renderWindow", at = @At("TAIL"), remap = false)
    private void thaumicCreate$renderConsumption(GuiGraphics graphics, int mouseX, int mouseY,
                                                float partialTicks, CallbackInfo ci) {
        if (!(board.formatter() instanceof EngineSpeedFormatter engineFormatter)) return;
        // Use the same pending value as Create's cursor, never the last server-confirmed RPM.
        var line = engineFormatter.consumption(getClosestCoordinate(mouseX, mouseY));
        int center = guiLeft + windowWidth / 2;
        int y = guiTop + windowHeight + 22;
        graphics.drawCenteredString(font, line, center, y, 0xFFF2D49A);
    }
}
