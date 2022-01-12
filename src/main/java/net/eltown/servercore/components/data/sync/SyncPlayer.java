package net.eltown.servercore.components.data.sync;

import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

public record SyncPlayer(
        @NotNull ItemStack[] inventory,
        @NotNull ItemStack[] armorInventory,
        @NotNull ItemStack[] enderchest,
        int foodLevel,
        float saturation,
        float exhaustion,
        int selectedSlot,
        @NotNull PotionEffect[] potionEffects,
        int totalExperience,
        int level,
        float experience,
        @NotNull GameMode gameMode,
        boolean flying
) { }