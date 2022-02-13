package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.components.roleplay.government.TownhallRoleplay;
import net.eltown.servercore.components.roleplay.shops.ShopRoleplay;
import net.eltown.servercore.utils.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record RoleplayListener(ServerCore serverCore) implements Listener {

    public static final List<String> openQueue = new ArrayList<>();
    private static List<PopupInfo> popupInfos = new ArrayList<>();

    public RoleplayListener(final ServerCore serverCore) {
        this.serverCore = serverCore;
        final World world = this.serverCore.getServer().getWorld("world");

        popupInfos.add(new PopupInfo(new Location(world, 89, 73, 74), new Location(world, 92, 77, 75),
                "§8» §fKlicke ein §2freies §fSchild an, um ein Gespräch zu starten.", List.of(BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST)
        ));
    }

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

                            case TOWNHALL_RECEPTION -> this.serverCore.getTownhallRoleplay().openReceptionByNpc(player);
                            case TOWNHALL_BUILDING -> {
                                if (this.serverCore.getTownhallRoleplay().isInAppointment(player.getName(), 10)) {
                                    this.serverCore.getTownhallRoleplay().openHerrKeppel(player, TownhallRoleplay.cachedAgencies.get(10));
                                }
                            }
                            case TOWNHALL_ADVICE_BUREAU_1 -> {
                                if (this.serverCore.getTownhallRoleplay().isInAdviceBureauAppointment(player.getName(), 1)) {
                                    this.serverCore.getTownhallRoleplay().openAdviceBureau(player, TownhallRoleplay.cachedAdviceBureau.get(1));
                                }
                            }
                            case TOWNHALL_ADVICE_BUREAU_2 -> {
                                if (this.serverCore.getTownhallRoleplay().isInAdviceBureauAppointment(player.getName(), 2)) {
                                    this.serverCore.getTownhallRoleplay().openAdviceBureau(player, TownhallRoleplay.cachedAdviceBureau.get(2));
                                }
                            }
                            case TOWNHALL_ADVICE_BUREAU_3 -> {
                                if (this.serverCore.getTownhallRoleplay().isInAdviceBureauAppointment(player.getName(), 3)) {
                                    this.serverCore.getTownhallRoleplay().openAdviceBureau(player, TownhallRoleplay.cachedAdviceBureau.get(3));
                                }
                            }
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

                                case TOWNHALL_RECEPTION -> this.serverCore.getTownhallRoleplay().openReceptionByNpc(player);
                                case TOWNHALL_BUILDING -> {
                                    if (this.serverCore.getTownhallRoleplay().isInAppointment(player.getName(), 10)) {
                                        this.serverCore.getTownhallRoleplay().openHerrKeppel(player, TownhallRoleplay.cachedAgencies.get(10));
                                    }
                                }
                                case TOWNHALL_ADVICE_BUREAU_1 -> {
                                    if (this.serverCore.getTownhallRoleplay().isInAdviceBureauAppointment(player.getName(), 1)) {
                                        this.serverCore.getTownhallRoleplay().openAdviceBureau(player, TownhallRoleplay.cachedAdviceBureau.get(1));
                                    }
                                }
                                case TOWNHALL_ADVICE_BUREAU_2 -> {
                                    if (this.serverCore.getTownhallRoleplay().isInAdviceBureauAppointment(player.getName(), 2)) {
                                        this.serverCore.getTownhallRoleplay().openAdviceBureau(player, TownhallRoleplay.cachedAdviceBureau.get(2));
                                    }
                                }
                                case TOWNHALL_ADVICE_BUREAU_3 -> {
                                    if (this.serverCore.getTownhallRoleplay().isInAdviceBureauAppointment(player.getName(), 3)) {
                                        this.serverCore.getTownhallRoleplay().openAdviceBureau(player, TownhallRoleplay.cachedAdviceBureau.get(3));
                                    }
                                }
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

    static final Cooldown iCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(2));

    @EventHandler
    public void on(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        if (block == null) return;
        if (this.serverCore.getServerName().equals("server-1")) {
            if (block.getX() == 18 && block.getY() == 70 && block.getZ() == 12 && block.getWorld().getName().equals("world") && block.getType() == Material.ENDER_CHEST) {
                this.serverCore.getJohnRoleplay().openCrate(player);
                event.setCancelled(true);
            } else if (this.serverCore.getChestShopAPI().isWallSign(block.getType())) {
                if (!iCooldown.hasCooldown(player.getName())) {
                    final Location location = block.getLocation();
                    this.serverCore.getTownhallRoleplay().getAdviceBureauSignLocations().forEach(((e, adviceBureau) -> {
                        if (location.equals(e) && !TownhallRoleplay.dateExpire.containsCooldown(player.getName())) {
                            if (!this.serverCore.getTownhallRoleplay().isAlreadyInAdviceBureauAppointment(player.getName())) {
                                if (adviceBureau.getCurrentPlayer().equals("null")) {
                                    player.teleport(adviceBureau.getTo());
                                    this.serverCore.getTownhallRoleplay().takeAdviceBureauAppointment(player.getName(), adviceBureau);
                                    this.serverCore.getTownhallRoleplay().openAdviceBureau(player, adviceBureau);
                                } else {
                                    Sound.NOTE_BASS.playSound(player);
                                    player.sendMessage("§8» §f" + adviceBureau.getName() + " §8| §7Bitte haben Sie einen Moment Geduld. Ich bin gerade beschäftigt.");
                                }
                            }
                        }
                    }));
                    if (TownhallRoleplay.dateExpire.containsCooldown(player.getName())) {
                        this.serverCore.getTownhallRoleplay().getSignLocations().forEach((c, g) -> {
                            if (location.equals(c)) {
                                if (g.getJob().equals(TownhallRoleplay.cachedAppointments.get(player.getName()).getJob())) {
                                    if (g.getCurrentPlayer().equals("null")) {
                                        Sound.RANDOM_DOOR_OPEN.playSound(player);
                                        player.teleport(g.getIn());
                                        this.serverCore.getTownhallRoleplay().removeAppointment(player.getName());
                                        this.serverCore.getTownhallRoleplay().takeAppointment(player.getName(), g);
                                        switch (g.getJob()) {
                                            case "Bauamt" -> this.serverCore.getTownhallRoleplay().openHerrKeppel(player, g);
                                        }
                                    } else {
                                        Sound.NOTE_BASS.playSound(player);
                                        player.sendMessage("§8» §f" + g.getName() + " §8| §7Bitte haben Sie einen Moment Geduld. Ich bin gerade in einem Termin.");
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        popupInfos.forEach(e -> {
            if (this.serverCore.isInArea(player.getLocation(), e.pos1(), e.pos2()) && e.blockFaces().contains(player.getFacing())) {
                player.sendActionBar(Component.text(e.message()));
            }
        });
    }

    public record PopupInfo(Location pos1, Location pos2, String message, List<BlockFace> blockFaces) {

    }

}
