package net.eltown.servercore.commands.economy;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.economy.SortPlayer;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TopMoneyCommand extends Command {

    final ServerCore serverCore;

    public TopMoneyCommand(final ServerCore serverCore) {
        super("topmoney", "Eine Auflistung der Spieler mit dem meisten Bargeld", "", List.of("baltop"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        this.serverCore.getEconomyAPI().getAll(all -> {
            final List<SortPlayer> sortPlayers = new ArrayList<>();
            all.forEach((string, money) -> sortPlayers.add(new SortPlayer(string, money)));
            sortPlayers.sort(Comparator.comparing(SortPlayer::money).reversed());

            int maxPages = sortPlayers.size() / 5;
            if (maxPages * 5 == sortPlayers.size()) maxPages--;
            int page = 0;

            try {
                if (args.length >= 1) {
                    final int tPage = Integer.parseInt(args[0]) - 1;
                    page = Math.min(tPage, maxPages);
                }
            } catch (final Exception ex) {
                sender.sendMessage(Language.get("economy.topmoney.invalid"));
                return;
            }

            sender.sendMessage(Language.getNP("economy.topmoney.header"));
            final int startFromIndex = page * 5;
            for (int i = 0; i < 5; i++) {
                final int at = startFromIndex + i;
                if (sortPlayers.size() - 1 >= at) {
                    SortPlayer sortPlayer = sortPlayers.get(at);
                    sender.sendMessage(Language.getNP("economy.topmoney.player", at + 1, sortPlayer.name(), this.serverCore.getMoneyFormat().format(sortPlayer.money())));
                }
            }
            sender.sendMessage("\n" + Language.getNP("economy.topmoney.siteinfo", page + 1, maxPages + 1));
            sender.sendMessage(Language.getNP("economy.topmoney.footer"));
        });
        return true;
    }
}
