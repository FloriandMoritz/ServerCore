package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.Home;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.data.teleportation.Warp;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public record TeleportationAPI(ServerCore serverCore) {

    public void teleportToHome(final Player player, final Home home) {
        if (home.getServer().equals(this.serverCore.getServerName())) {
            player.teleport(new Location(this.serverCore.getServer().getWorld(home.getWorld()), home.getX(), home.getY(), home.getZ(), (float) home.getYaw(), (float) home.getPitch()));
            player.sendMessage(Language.get("home.teleported", home.getName()));
        } else {
            this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT_HOME.name(), "home", home.getName(), home.getPlayer(), home.getServer(), home.getWorld(),
                    String.valueOf(home.getX()), String.valueOf(home.getY()), String.valueOf(home.getZ()), String.valueOf(home.getYaw()), String.valueOf(home.getPitch()));
        }
    }

    public void teleportToWarp(final Player player, final Warp warp) {
        if (warp.getServer().equals(this.serverCore.getServerName())) {
            player.teleport(new Location(this.serverCore.getServer().getWorld(warp.getWorld()), warp.getX(), warp.getY(), warp.getZ(), (float) warp.getYaw(), (float) warp.getPitch()));
            player.sendMessage(Language.get("warp.teleported", warp.getName()));
        } else {
            this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT_WARP.name(), "warp", warp.getName(), player.getName(), warp.getServer(), warp.getWorld(),
                    String.valueOf(warp.getX()), String.valueOf(warp.getY()), String.valueOf(warp.getZ()), String.valueOf(warp.getYaw()), String.valueOf(warp.getPitch()));
        }
    }

    public void teleportToTpa(final Player player, final String target) {
        final Player targetPlayer = this.serverCore.getServer().getPlayer(target);
        if (targetPlayer != null) {
            targetPlayer.teleport(player);
        } else {
            this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "tpa", target, player.getName());
        }
    }

    public void teleportToPlayer(final Player player, final String target) {
        final Player targetPlayer = this.serverCore.getServer().getPlayer(target);
        if (targetPlayer != null) {
            player.teleport(targetPlayer);
        } else {
            this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "teleport", player.getName(), target);
        }
    }

    public void teleportToPlayer(final Player player, final String target, final String message) {
        final Player targetPlayer = this.serverCore.getServer().getPlayer(target);
        if (targetPlayer != null) {
            player.teleport(targetPlayer);
            player.sendMessage(message);
        } else {
            this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "teleport", player.getName(), target);
            player.sendMessage(message);
        }
    }

    public void handleCachedData(final Player player) {
        this.serverCore.getTinyRabbit().sendAndReceive((data) -> {
            if (TeleportationCalls.valueOf(data.getKey().toUpperCase()) == TeleportationCalls.CALLBACK_CACHED_DATA) {
                final String[] d = data.getData();
                switch (d[1]) {
                    case "home" -> {
                        player.teleport(new Location(this.serverCore.getServer().getWorld(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), (float) Double.parseDouble(d[7]), (float) Double.parseDouble(d[8])));
                        player.sendMessage(Language.get("home.teleported", d[2]));
                    }
                    case "warp" -> {
                        player.teleport(new Location(this.serverCore.getServer().getWorld(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), (float) Double.parseDouble(d[7]), (float) Double.parseDouble(d[8])));
                        player.sendMessage(Language.get("warp.teleported", d[2]));
                    }
                    case "teleport" -> {
                        final Player target = this.serverCore.getServer().getPlayer(d[2]);
                        if (target != null) {
                            player.teleport(target);
                        } else player.sendMessage("§cEs trat ein Fehler bei der Teleportation auf: §7teleport#" + d[2]);
                    }
                    case "tpa" -> {
                        final Player target = this.serverCore.getServer().getPlayer(d[2]);
                        if (target != null) {
                            player.teleport(target);
                            player.sendMessage(Language.get("tpa.teleported", target.getName()));
                        } else player.sendMessage("§cEs trat ein Fehler bei der Teleportation auf: §7tpa#" + d[2]);
                    }
                    default -> player.sendMessage("§cEs trat ein Fehler bei der Teleportation auf: §7" + d[1] + "#" + d[2]);
                }
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_CACHED_DATA.name(), player.getName());
    }

    public void createHome(final String name, final Player player, final Consumer<Boolean> alreadySet) {
        final Location location = player.getLocation();
        this.serverCore.getTinyRabbit().sendAndReceive((data) -> {
                    switch (TeleportationCalls.valueOf(data.getKey().toUpperCase())) {
                        case CALLBACK_NULL -> alreadySet.accept(false);
                        case CALLBACK_HOME_ALREADY_SET -> alreadySet.accept(true);
                    }
                }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ADD_HOME.name(), name, player.getName(), this.serverCore.getServerName(), location.getWorld().getName(),
                String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()), String.valueOf(location.getPitch()));
    }

    public void deleteHome(final String home, final String player) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DELETE_HOME.name(), home, player);
    }

    public void deleteServerHomes() {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DELETE_ALL_SERVER_HOMES.name(), this.serverCore.getServerName());
    }

    public void updateHomeName(final String home, final String player, final String newName) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_RENAME_HOME.name(), home, player, newName);
    }

    public void updateHomePosition(final String home, final String player, final Location location) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_UPDATE_POSITION.name(), home, player, this.serverCore.getServerName(), location.getWorld().getName(),
                String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()), String.valueOf(location.getPitch()));
    }

    public Set<Home> getHomes(final String player) {
        final Set<Home> homes = new HashSet<>();
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (TeleportationCalls.valueOf(delivery.getKey().toUpperCase()) == TeleportationCalls.CALLBACK_ALL_HOMES) {
                final List<String> list = Arrays.asList(delivery.getData());
                list.forEach(e -> {
                    if (!e.equals(delivery.getKey().toLowerCase())) {
                        final String[] d = e.split(">>");
                        homes.add(new Home(d[0], d[1], d[2], d[3], Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8])));
                    }
                });
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ALL_HOMES.name(), player);
        return homes;
    }

    public void createWarp(final String name, final String displayName, final String imageUrl, final Location location, final Consumer<Boolean> alreadySet) {
        this.serverCore.getTinyRabbit().sendAndReceive((delivery -> {
                    switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_NULL -> alreadySet.accept(false);
                        case CALLBACK_WARP_ALREADY_SET -> alreadySet.accept(true);
                    }
                }), Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ADD_WARP.name(), name, displayName, imageUrl, this.serverCore.getServerName(), location.getWorld().getName(),
                String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()), String.valueOf(location.getPitch()));
    }

    public void deleteWarp(final String name) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DELETE_WARP.name(), name);
    }

    public void updateWarpDisplayName(final String name, final String displayName) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_RENAME_WARP.name(), name, displayName);
    }

    public void updateWarpImageUrl(final String name, final String imageUrl) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_UPDATE_WARP_IMAGE.name(), name, imageUrl);
    }

    public void updateWarpPosition(final String name, final Location location) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_UPDATE_WARP_POSITION.name(), name, this.serverCore.getServerName(),
                location.getWorld().getName(), String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()),
                String.valueOf(location.getPitch()));
    }

    public Set<Warp> getWarps() {
        final Set<Warp> warps = new HashSet<>();
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (TeleportationCalls.valueOf(delivery.getKey().toUpperCase()) == TeleportationCalls.CALLBACK_ALL_WARPS) {
                final List<String> list = Arrays.asList(delivery.getData());
                list.forEach(e -> {
                    if (!e.equals(delivery.getKey().toLowerCase())) {
                        final String[] d = e.split(">>");
                        warps.add(new Warp(d[0], d[1], d[2], d[3], d[4], Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8]), Double.parseDouble(d[9])));
                    }
                });
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ALL_WARPS.name(), "null");
        return warps;
    }

    public void sendTpa(final String player, final String target, final Consumer<Boolean> alreadySent) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery2 -> {
            switch (TeleportationCalls.valueOf(delivery2.getKey().toUpperCase())) {
                case CALLBACK_TPA_ALREADY_SENT -> alreadySent.accept(true);
                case CALLBACK_NULL -> alreadySent.accept(false);
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_SEND_TPA.name(), player, target);
    }

    public Set<String> getTpas(final String player) {
        final Set<String> tpas = new HashSet<>();
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (TeleportationCalls.valueOf(delivery.getKey().toUpperCase()) == TeleportationCalls.CALLBACK_TPAS) {
                final List<String> list = Arrays.asList(delivery.getData());
                list.forEach(e -> {
                    if (!e.equals(delivery.getKey().toLowerCase())) {
                        tpas.add(e);
                    }
                });
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_TPAS.name(), player);
        return tpas;
    }

    public void acceptTpa(final String player, final String target) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_ACCEPT_TPA.name(), player, target);
    }

    public void denyTpa(final String player, final String target) {
        this.serverCore.getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DENY_TPA.name(), player, target);
    }

}
