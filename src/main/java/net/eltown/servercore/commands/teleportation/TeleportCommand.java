package net.eltown.servercore.commands.teleportation;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class TeleportCommand extends Command {

    private final ServerCore serverCore;

    public TeleportCommand(final ServerCore serverCore) {
        super("teleport", "Teleportiere dich schnell in die FarmWelt", "", List.of("tp", "farmen"));
        this.setPermission("core.command.teleport");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player player) {
            if (args.length == 1) {
                final Player target = this.serverCore.getServer().getPlayer(args[0]);
                if (target != null) {
                    player.teleport(target);
                    player.sendMessage(Language.get("teleport.teleported.target", target.getName()));

                    PluginCommand.broadcastCommandMessage(sender, "Teleported to " + target.getName(), false);
                } else {
                    this.serverCore.getCoreAPI().proxyPlayerIsOnline(args[0], isOnline -> {
                        if (isOnline) {
                            this.serverCore.getTeleportationAPI().teleportToPlayer(player, args[0]);
                            player.sendMessage(Language.get("teleport.teleported.target", args[0]));

                            PluginCommand.broadcastCommandMessage(sender, "Teleported to " + args[0], false);
                        } else {
                            player.sendMessage(Language.get("teleport.player.not.online", args[0]));
                        }
                    });
                }
            } else if (args.length == 2) {
                final Player from = this.serverCore.getServer().getPlayer(args[0]);
                final Player to = this.serverCore.getServer().getPlayer(args[1]);
                if (from != null && to != null) {
                    from.teleport(to);
                    player.sendMessage(Language.get("teleport.teleported.others", from.getName(), to.getName()));

                    PluginCommand.broadcastCommandMessage(sender, "Teleported " + from.getName() + " to " + to.getName(), false);
                } else {
                    this.serverCore.getCoreAPI().proxyPlayerIsOnline(args[0], isOnline -> {
                        if (isOnline) {
                            this.serverCore.getTeleportationAPI().teleportToPlayer(from, args[0]);
                            player.sendMessage(Language.get("teleport.teleported.others", args[0], args[1]));

                            PluginCommand.broadcastCommandMessage(sender, "Teleported " + args[0] + " to " + args[1], false);
                        }
                    });
                }
            } else if (args.length == 3) {
                try {
                    final int x = Integer.parseInt(args[0]);
                    final int y = Integer.parseInt(args[1]);
                    final int z = Integer.parseInt(args[2]);
                    player.teleport(new Location(player.getWorld(), x, y, z));
                    player.sendMessage(Language.get("teleport.teleported.xyz", x, y, z));

                    PluginCommand.broadcastCommandMessage(sender, "Teleported to " + x + ", " + y + ", " + z, false);
                } catch (final Exception e) {
                    player.sendMessage(Language.get("teleport.invalid.coordiantes"));
                }
            } else if (args.length == 4) {
                try {
                    final Player target = this.serverCore.getServer().getPlayer(args[0]);
                    if (target == null) {
                        player.sendMessage(Language.get("teleport.player.not.online", args[0]));
                        return true;
                    }

                    final int x = Integer.parseInt(args[1]);
                    final int y = Integer.parseInt(args[2]);
                    final int z = Integer.parseInt(args[3]);
                    target.teleport(new Location(target.getWorld(), x, y, z));
                    player.sendMessage(Language.get("teleport.teleported.xyz.other", target.getName(), x, y, z));

                    PluginCommand.broadcastCommandMessage(sender, "Teleported " + target.getName() + " to " + x + ", " + y + ", " + z, false);
                } catch (final Exception e) {
                    player.sendMessage(Language.get("teleport.invalid.coordiantes"));
                }
            } else player.sendMessage(Language.get("teleport.usage", this.getName()));
        }
        return true;
    }
}
