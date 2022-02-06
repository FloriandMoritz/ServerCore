package net.eltown.servercore.commands.economy;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReduceMoneyCommand extends Command {

    final ServerCore serverCore;

    public ReduceMoneyCommand(final ServerCore serverCore) {
        super("reducemoney", "Verringere das Bargeld eines Spielers", "", List.of("takemoney"));
        this.serverCore = serverCore;
        this.setPermission("core.command.reducemoney");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length >= 2) {

            String target = args[0];
            final Player playerTarget = Bukkit.getPlayer(target);
            if (playerTarget != null) target = playerTarget.getName();

            final String finalTarget = target;
            this.serverCore.getEconomyAPI().hasAccount(target, (has) -> {
                try {
                    if (!has) {
                        sender.sendMessage(Language.get("economy.player.not.registered", finalTarget));
                        return;
                    }

                    final double amt = Double.parseDouble(args[1]);

                    if (amt < 0) {
                        sender.sendMessage(Language.get("economy.invalid.amount"));
                        return;
                    }

                    this.serverCore.getEconomyAPI().reduceMoney(finalTarget, amt);
                    sender.sendMessage(Language.get("economy.money.reduced", finalTarget, this.serverCore.getMoneyFormat().format(amt)));
                } catch (final NumberFormatException ex) {
                    sender.sendMessage(Language.get("economy.invalid.amount"));
                }
            });
        }
        return true;
    }
}
