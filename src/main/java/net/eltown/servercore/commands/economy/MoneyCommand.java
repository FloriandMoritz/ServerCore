package net.eltown.servercore.commands.economy;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MoneyCommand extends Command {

    final ServerCore serverCore;

    public MoneyCommand(final ServerCore serverCore) {
        super("money", "Sehe deinen Bargeldstand ein oder den eines anderen Spielers", "", List.of("bargeld", "bal", "getmoney"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (args.length >= 1) {
            String target = args[0];
            Player playerTarget = Bukkit.getPlayer(target);
            if (playerTarget != null) target = playerTarget.getName();

            String finalTarget = target;
            this.serverCore.getEconomyAPI().hasAccount(target, (has) -> {
                if (!has) {
                    sender.sendMessage(Language.get("economy.player.not.registered", finalTarget));
                    return;
                }

                this.serverCore.getEconomyAPI().getMoney(finalTarget, (money) -> {
                    sender.sendMessage(Language.get("economy.money.other", finalTarget, this.serverCore.getMoneyFormat().format(money)));
                });
            });
        } else {
            if (sender instanceof Player) {
                this.serverCore.getEconomyAPI().getMoney(sender.getName(), (money) -> {
                    sender.sendMessage(Language.get("economy.money", this.serverCore.getMoneyFormat().format(money)));
                });
            }
        }
        return true;
    }
}
