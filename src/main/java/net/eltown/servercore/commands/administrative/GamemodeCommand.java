package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GamemodeCommand extends Command {

    private final ServerCore serverCore;

    public GamemodeCommand(final ServerCore serverCore) {
        super("gamemode", "", "", List.of("gm", "mode", "spielmodus"));
        this.serverCore = serverCore;
        this.setPermission("core.command.gamemode");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length == 1 && sender instanceof Player player) {
            switch (args[0]) {
                case "survival", "0" -> {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(Language.get("gamemode.set", 0));
                }
                case "creative", "1" -> {
                    player.setGameMode(GameMode.CREATIVE);
                    player.sendMessage(Language.get("gamemode.set", 1));
                }
                case "adventure", "2" -> {
                    player.setGameMode(GameMode.ADVENTURE);
                    player.sendMessage(Language.get("gamemode.set", 2));
                }
                case "spectator", "3" -> {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(Language.get("gamemode.set", 3));
                }
                default -> player.sendMessage(Language.get("gamemode.mode.invalid"));
            }
        } else if (args.length == 2) {
            final Player player = this.serverCore.getServer().getPlayer(args[1]);
            if (player != null) {
                switch (args[0]) {
                    case "survival", "0" -> {
                        player.setGameMode(GameMode.SURVIVAL);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 0));
                    }
                    case "creative", "1" -> {
                        player.setGameMode(GameMode.CREATIVE);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 1));
                    }
                    case "adventure", "2" -> {
                        player.setGameMode(GameMode.ADVENTURE);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 2));
                    }
                    case "spectator", "3" -> {
                        player.setGameMode(GameMode.SPECTATOR);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 3));
                    }
                    default -> sender.sendMessage(Language.get("gamemode.mode.invalid"));
                }
            } else sender.sendMessage(Language.get("gamemode.player.not.found"));
        } else sender.sendMessage(Language.get("gamemode.usage"));
        return true;
    }
}
