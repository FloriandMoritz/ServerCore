package net.eltown.servercore.commands.economy;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PayCommand extends Command {

    final ServerCore serverCore;

    public PayCommand(final ServerCore serverCore) {
        super("pay", "Überweise einem Spieler Bargeld", "", List.of("balpay", "bezahlen"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length >= 2) {
                this.serverCore.getEconomyAPI().getMoney(player.getName(), (senderMoney) -> {
                    try {
                        double toPay = Double.parseDouble(args[1]);

                        if (toPay > senderMoney) {
                            player.sendMessage(Language.get("economy.pay.not.enough.money"));
                            return;
                        }

                        if (toPay < 0) {
                            sender.sendMessage(Language.get("economy.invalid.amount"));
                            return;
                        }

                        String target = args[0];
                        final Player playerTarget = Bukkit.getPlayer(target);
                        if (playerTarget != null) target = playerTarget.getName();

                        if (target.equals(sender.getName())) return;

                        final String finalTarget = target;
                        this.serverCore.getEconomyAPI().hasAccount(target, (has) -> {
                            if (!has) {
                                player.sendMessage(Language.get("economy.player.not.registered", finalTarget));
                                return;
                            }

                            this.serverCore.getEconomyAPI().reduceMoney(player.getName(), toPay);
                            this.serverCore.getEconomyAPI().addMoney(finalTarget, toPay);

                            player.sendMessage(Language.get("economy.pay.payer", finalTarget, this.serverCore.getMoneyFormat().format(toPay)));
                            if (playerTarget != null) {
                                playerTarget.sendMessage(Language.get("economy.pay.target", player.getName(), this.serverCore.getMoneyFormat().format(toPay)));
                            }
                        });
                    } catch (final Exception ex) {
                        sender.sendMessage(Language.get("economy.invalid.amount"));
                        ex.printStackTrace();
                    }
                });
            } else sender.sendMessage(Language.get("economy.pay.usage"));
        }
        return true;
    }
}
