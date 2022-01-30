package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.commands.defaults.SpawnCommand;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand extends Command {

    private final ServerCore serverCore;

    public SetSpawnCommand(final ServerCore serverCore) {
        super("setspawn");
        this.serverCore = serverCore;
        this.setPermission("core.command.setspawn");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            final Location location = player.getLocation();

            this.serverCore.getConfig().set("spawn.x", location.getX());
            this.serverCore.getConfig().set("spawn.y", location.getY());
            this.serverCore.getConfig().set("spawn.z", location.getZ());
            this.serverCore.getConfig().set("spawn.yaw", location.getYaw());
            this.serverCore.getConfig().set("spawn.pitch", location.getPitch());
            this.serverCore.getConfig().set("spawn.level", location.getWorld().getName());
            this.serverCore.saveConfig();

            SpawnCommand.spawnLocation = player.getLocation();

            player.sendMessage("Der Spawnpunkt dieses Servers wurde umgesetzt.");
        }
        return false;
    }
}
