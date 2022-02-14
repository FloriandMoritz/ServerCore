package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.commands.administrative.SpawnProtectionCommand;
import net.eltown.servercore.commands.defaults.SpawnCommand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public record SpawnProtectionListener(ServerCore serverCore) implements Listener {

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        if (isInRadius(event.getPlayer())) {
            if (SpawnProtectionCommand.spawnProtection - getDistance(event.getPlayer().getLocation().toVector()) == 0) return;
            event.getPlayer().sendActionBar("§0Du bist noch im Spawnbereich! §8[§2" + (SpawnProtectionCommand.spawnProtection - getDistance(event.getPlayer().getLocation().toVector())) + " Blöcke§8]");
        }
    }

    @EventHandler
    public void on(final PlayerInteractEvent event) {
        if (isInRadius(event.getPlayer()) && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        if (isInRadius(event.getPlayer()) && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler
    public void on(final BlockPlaceEvent event) {
        if (isInRadius(event.getPlayer()) && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler
    public void on(final EntityExplodeEvent event) {
        if (isInRadius(event.getLocation().toVector())) event.blockList().clear();
    }

    @EventHandler
    public void on(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isInRadius((Player) event.getEntity()))
            event.setCancelled(true);
    }

    public static boolean isInRadius(final Vector vector3) {
        return SpawnProtectionCommand.spawnProtection >= getDistance(vector3);
    }

    public static boolean isInRadius(final Player player) {
        return SpawnProtectionCommand.spawnProtection >= getDistance(player.getLocation().toVector());
    }

    public static int getDistance(final Vector position) {
        final Location spawn = SpawnCommand.spawnLocation;

        final double dx = Math.max(spawn.getX(), position.getX()) - Math.min(spawn.getX(), position.getX());
        final double dz = Math.max(spawn.getZ(), position.getZ()) - Math.min(spawn.getZ(), position.getZ());

        return (int) Math.sqrt(dx * dx + dz * dz);
    }
}
