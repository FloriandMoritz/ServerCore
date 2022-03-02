package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.listeners.RoleplayListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CoreDebugCommand extends Command {

    private final ServerCore serverCore;

    public CoreDebugCommand(final ServerCore serverCore) {
        super("coredebug");
        this.serverCore = serverCore;
        this.setPermission("core.command.coredebug");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("removeinteract")) {
                    final String target = args[1];
                    RoleplayListener.openQueue.remove(target);
                    player.sendMessage(target + " wurde aus der 'openQueue' entfernt.");
                } else this.sendHelp(player);
            } else this.sendHelp(player);
        }
        return false;
    }

    private void sendHelp(final Player player) {
        player.sendMessage("--- CoreDebug ---");
        player.sendMessage("/coredebug removeinteract <target>");
    }
}
