package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HealCommand extends Command {

    final ServerCore serverCore;

    public HealCommand(final ServerCore serverCore) {
        super("heal");
        this.serverCore = serverCore;
        this.setPermission("core.command.heal");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length == 0 && sender instanceof Player player) {
            player.setHealth(20);
            player.setFoodLevel(20);
            player.sendMessage(Language.get("heal.healed"));
        } else if (args.length == 1) {
            final Player player = this.serverCore.getServer().getPlayer(args[0]);
            if (player != null) {
                player.setHealth(20);
                player.setFoodLevel(20);
                sender.sendMessage(Language.get("heal.healed.other", player.getName()));
            } else sender.sendMessage(Language.get("heal.player.not.online"));
        } else sender.sendMessage(Language.get("heal.usage"));
        return false;
    }
}
