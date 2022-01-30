package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlyCommand extends Command {

    private final ServerCore serverCore;

    public FlyCommand(final ServerCore serverCore) {
        super("fly");
        this.serverCore = serverCore;
        this.setPermission("core.command.fly");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            if (args.length == 0) {
                if (player.isFlying()) {
                    player.setAllowFlight(false);
                    player.sendMessage(Language.get("fly.disabled"));
                } else {
                    player.setAllowFlight(true);
                    player.sendMessage(Language.get("fly.enabled"));
                }
            } else if (args.length == 1) {
                final Player target = this.serverCore.getServer().getPlayer(args[0]);
                if (target != null) {
                    if (target.isFlying()) {
                        player.setAllowFlight(false);
                        sender.sendMessage(Language.get("fly.disabled.other", target.getName()));
                    } else {
                        player.setAllowFlight(true);
                        sender.sendMessage(Language.get("fly.enabled.other", target.getName()));
                    }
                } else sender.sendMessage(Language.get("fly.player.not.found"));
            } else sender.sendMessage(Language.get("fly.usage"));
        }
        return false;
    }
}
