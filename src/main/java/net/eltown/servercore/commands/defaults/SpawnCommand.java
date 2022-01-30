package net.eltown.servercore.commands.defaults;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends Command {

    private final ServerCore serverCore;

    public static Location spawnLocation;

    public SpawnCommand(final ServerCore serverCore) {
        super("spawn");
        this.serverCore = serverCore;

        if (serverCore.getConfig().isConfigurationSection("spawn")) {
            spawnLocation = new Location(
                    serverCore.getServer().getWorld(serverCore.getConfig().getString("spawn.level")),
                    serverCore.getConfig().getDouble("spawn.x"),
                    serverCore.getConfig().getDouble("spawn.y"),
                    serverCore.getConfig().getDouble("spawn.z"),
                    (float) serverCore.getConfig().getDouble("spawn.yaw"),
                    (float) serverCore.getConfig().getDouble("spawn.pitch")
            );
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            player.teleport(spawnLocation);
            player.sendMessage(Language.get("spawn.teleported"));
        }
        return true;
    }
}
