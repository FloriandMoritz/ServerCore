package net.eltown.servercore.commands.economy;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AddMoneyCommand extends Command {

    final ServerCore serverCore;

    public AddMoneyCommand(final ServerCore serverCore) {
        super("addmoney", "Füge einem Spieler Bargeld hinzu", "", List.of("givemoney"));
        this.serverCore = serverCore;
        this.setPermission("core.command.addmoney");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length >= 2) {
            try {
                String target = args[0];

                final Player player = this.serverCore.getServer().getPlayer(target);
                if (player != null) target = player.getName();

                final String finalTarget = target;
                this.serverCore.getEconomyAPI().hasAccount(target, has -> {
                    if (has) {
                        final double amount = Double.parseDouble(args[1]);

                        if (amount <= 0) {
                            player.sendMessage(Language.get("economy.invalid.amount"));
                            return;
                        }

                        this.serverCore.getEconomyAPI().addMoney(finalTarget, amount);
                        sender.sendMessage(Language.get("economy.money.added", finalTarget, this.serverCore.getMoneyFormat().format(amount)));
                    } else sender.sendMessage(Language.get("economy.player.not.registered", finalTarget));
                });
            } catch (final Exception e) {
                sender.sendMessage("economy.invalid.amount");
            }
        }
        return true;
    }
}
