package com.thaumiccreate;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Machine owner and last verified research travel with the block entity's saved data. */
public final class PressResearch {
    private static final String OWNER = "ThaumicCreateOwner";
    private PressResearch() {}

    public static boolean isPress(BlockEntity be) {
        return be != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE
            .getKey(be.getType()).toString().equals("create:mechanical_press");
    }
    public static void bind(BlockEntity be, Player player) {
        if (!isPress(be) || be.getLevel().isClientSide) return;
        CompoundTag data = be.getPersistentData();
        if (!data.hasUUID(OWNER)) {
            data.putUUID(OWNER, player.getUUID());
            be.setChanged();
        }
        if (data.getUUID(OWNER).equals(player.getUUID())) {
            for (String research : new String[]{"create_plate_thaumium", "create_plate_void"})
                data.putBoolean(research, completed(player, research));
            be.setChanged();
        }
    }
    public static boolean allowed(BlockEntity be, ResourceLocation recipe) {
        if (!recipe.getNamespace().equals("thaumic_create")) return true;
        String research = switch (recipe.getPath()) {
            case "pressing/plate_thaumium" -> "create_plate_thaumium";
            case "pressing/plate_void" -> "create_plate_void";
            default -> null;
        };
        if (research == null) return true;
        if (be.getLevel() == null || be.getLevel().isClientSide) return false;
        CompoundTag data = be.getPersistentData();
        if (!data.hasUUID(OWNER)) return false;
        ServerPlayer player = be.getLevel().getServer().getPlayerList().getPlayer(data.getUUID(OWNER));
        // Keep automation working while its owner is offline, using the last verified knowledge.
        if (player != null) {
            boolean complete = completed(player, research);
            if (data.getBoolean(research) != complete) {
                data.putBoolean(research, complete);
                be.setChanged();
            }
            return complete;
        }
        return data.getBoolean(research);
    }
    private static boolean completed(Player player, String research) {
        try {
            Class<?> access = Class.forName("com.thaumicreborn.knowledge.KnowledgeAccess");
            Optional<?> knowledge = (Optional<?>) access.getMethod("get", Player.class).invoke(null, player);
            return knowledge.isPresent() && (boolean) knowledge.get().getClass()
                .getMethod("hasCompletedResearch", String.class).invoke(knowledge.get(), research);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot read Thaumic Reborn research", error);
        }
    }
}
