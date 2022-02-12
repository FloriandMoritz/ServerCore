package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.components.roleplay.shops.ShopRoleplay;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public record RoleplayListener(ServerCore serverCore) implements Listener {

    public static final List<String> openQueue = new ArrayList<>();

    @EventHandler
    public void on(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final Entity entity = event.getRightClicked();
        if (entity.getType() == EntityType.VILLAGER) {
            final Villager villager = (Villager) entity;
            if (villager.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING)) {
                final String key = villager.getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING);
                if (!openQueue.contains(player.getName())) {
                    try {
                        final RoleplayID id = RoleplayID.valueOf(key);
                        switch (id) {
                            case FEATURE_LOLA -> this.serverCore.getLolaRoleplay().openLolaByNpc(player);
                            case FEATURE_JOHN -> this.serverCore.getJohnRoleplay().openJohnByNpc(player);
                            case JOB_BANKER -> this.serverCore.getBankRoleplay().openBankManagerByNpc(player);
                            case JOB_COOK -> this.serverCore.getCookRoleplay().openCookByNpc(player);
                            default -> this.serverCore.getShopRoleplay().interact(player, ShopRoleplay.availableShops.get(id));
                        }
                    } catch (final Exception ignored) {
                    }
                }
                event.setCancelled(true);
            }
        } else if (entity.getType() == EntityType.ARMOR_STAND) {
            final ArmorStand armorStand = (ArmorStand) entity;
            if (armorStand.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "fnpc.key"), PersistentDataType.STRING)) {
                final String key = armorStand.getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "fnpc.key"), PersistentDataType.STRING);
                if (!openQueue.contains(player.getName())) {
                    try {
                        final RoleplayID id = RoleplayID.valueOf(key);
                        switch (id) {
                            case UTIL_ATM -> this.serverCore.getBankRoleplay().openBankLogin(player);
                        }
                    } catch (final Exception ignored) {
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(final EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();
        if (entity.getType() == EntityType.VILLAGER) {
            final Villager villager = (Villager) entity;
            if (villager.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING)) {
                final String key = villager.getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING);
                if (event.getDamager() instanceof final Player player) {
                    if (!openQueue.contains(player.getName())) {
                        try {
                            final RoleplayID id = RoleplayID.valueOf(key);
                            switch (id) {
                                case FEATURE_LOLA -> this.serverCore.getLolaRoleplay().openLolaByNpc(player);
                                case FEATURE_JOHN -> this.serverCore.getJohnRoleplay().openJohnByNpc(player);
                                case JOB_BANKER -> this.serverCore.getBankRoleplay().openBankManagerByNpc(player);
                                case JOB_COOK -> this.serverCore.getCookRoleplay().openCookByNpc(player);
                                default -> this.serverCore.getShopRoleplay().interact(player, ShopRoleplay.availableShops.get(id));
                            }
                        } catch (final Exception ignored) {
                        }
                    }
                }
            }
        } else if (entity.getType() == EntityType.ARMOR_STAND) {
            final ArmorStand armorStand = (ArmorStand) entity;
            if (armorStand.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "fnpc.key"), PersistentDataType.STRING)) {
                final String key = armorStand.getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "fnpc.key"), PersistentDataType.STRING);
                if (event.getDamager() instanceof final Player player) {
                    if (!openQueue.contains(player.getName())) {
                        try {
                            final RoleplayID id = RoleplayID.valueOf(key);
                            switch (id) {
                                case UTIL_ATM -> this.serverCore.getBankRoleplay().openBankLogin(player);
                            }
                        } catch (final Exception ignored) {
                        }
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        if (block == null) return;
        if (this.serverCore.getServerName().equals("server-1")) {
            if (block.getX() == 18 && block.getY() == 70 && block.getZ() == 12 && block.getWorld().getName().equals("world") && block.getType() == Material.ENDER_CHEST) {
                this.serverCore.getJohnRoleplay().openCrate(player);
                event.setCancelled(true);
            }
        }
    }

}
