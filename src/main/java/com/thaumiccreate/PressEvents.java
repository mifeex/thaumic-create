package com.thaumiccreate;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class PressEvents {
    private PressEvents() {}
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player)
            PressResearch.bind(event.getLevel().getBlockEntity(event.getPos()), player);
    }
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        PressResearch.bind(event.getLevel().getBlockEntity(event.getPos()), event.getEntity());
    }
}
