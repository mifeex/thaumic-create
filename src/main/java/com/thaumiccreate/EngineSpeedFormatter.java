package com.thaumiccreate;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Identifies the engine board and keeps its slider label short. */
public final class EngineSpeedFormatter extends ValueSettingsFormatter {
    private java.util.function.Supplier<String> fuel = () -> "potentia";
    public EngineSpeedFormatter(java.util.function.Supplier<String> fuel) { this(); this.fuel=fuel; }
    public EngineSpeedFormatter() {
        super(settings -> Component.literal(speed(settings) + " RPM"));
    }

    public static int speed(ValueSettings settings) {
        return Math.max(1, Math.min(128, settings.value()));
    }

    public MutableComponent consumption(ValueSettings preview) {
        return Component.translatable("engine.thaumic_create.fuel_rate",
                EssentiaEngineBlockEntity.costText(speed(preview)), Component.translatable("tc.aspect."+fuel.get()));
    }
}
