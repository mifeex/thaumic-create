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
        var level = event.getLevel();
        var player = event.getEntity();
        var clicked = level.getBlockEntity(event.getPos());
        PressResearch.bind(clicked, player);
        // Create processes items on a depot two blocks below the press.
        if (clicked instanceof com.simibubi.create.content.logistics.depot.DepotBlockEntity depot
                && event.getFace() == net.minecraft.core.Direction.UP) {
            var offered = player.getItemInHand(event.getHand());
            var held = depot.getHeldItem();
            if (PressResearch.isPlateIngot(offered)
                    && (held.isEmpty() || (held.getItem() == offered.getItem() && held.getCount() < held.getMaxStackSize())))
                PressResearch.bindInput(level.getBlockEntity(event.getPos().above(2)), player);
        }
    }
}
