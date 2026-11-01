package com.thaumiccreate;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

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
