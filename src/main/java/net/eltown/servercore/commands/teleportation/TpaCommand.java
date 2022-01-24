package net.eltown.servercore.commands.teleportation;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaCommand extends Command {

    private final ServerCore serverCore;

    public TpaCommand(final ServerCore serverCore) {
        super("tpa");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.serverCore.getCoreAPI().proxyGetOnlinePlayers(players -> {
                if (players.size() == 1) {
                    player.sendMessage(Language.get("tpa.no.players"));
                    return;
                }

                final SimpleWindow.Builder selectWindow = new SimpleWindow.Builder("§7» §8Teleportationsanfrage senden", "§8» §fSuche dir den Spieler aus, zu dem du dich teleportieren lassen möchtest.");
                players.forEach(e -> {
                    if (e.equals(player.getName())) return;
                    selectWindow.addButton("§8» §fAnfrage an:\n§a" + e, g -> {
                        this.serverCore.getCoreAPI().proxyPlayerIsOnline(e, is -> {
                            if (is) {
                                this.serverCore.getTeleportationAPI().sendTpa(player.getName(), e, aBoolean -> {
                                    if (aBoolean) {
                                        player.sendMessage(Language.get("tpa.already.sent", e));
                                    } else {
                                        player.sendMessage(Language.get("tpa.sent", e));
                                    }
                                });
                            } else {
                                player.sendMessage(Language.get("tpa.target.offline", e));
                            }
                        });
                    });
                });
                selectWindow.build().send(player);
            });
        }
        return true;
    }
}
