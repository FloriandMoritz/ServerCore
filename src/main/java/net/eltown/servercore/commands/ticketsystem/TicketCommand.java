package net.eltown.servercore.commands.ticketsystem;

import net.eltown.servercore.ServerCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TicketCommand extends Command {

    private final ServerCore serverCore;

    public TicketCommand(final ServerCore serverCore) {
        super("ticket", "Stelle Fragen, fordere Hilfe an oder gebe uns Feedback", "", List.of("support", "hilfe", "feedback"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            player.sendMessage("§8» §fCore §8| §7Dieses Feature ist bald erst verfügbar. Für weitere Informationen, besuche unseren Discord-Server unter §9http://bit.ly/discord-et§7.");
        }
        return true;
    }
}
