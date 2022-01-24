package net.eltown.servercore.commands.teleportation;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TpacceptCommand extends Command {

    private final ServerCore serverCore;

    public TpacceptCommand(final ServerCore serverCore) {
        super("tpaccept");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            final Set<String> tpas = this.serverCore.getTeleportationAPI().getTpas(player.getName());
            if (tpas.size() == 0) {
                player.sendMessage(Language.get("tpa.no.requests"));
                return true;
            }

            final SimpleWindow.Builder tpasWindow = new SimpleWindow.Builder("§7» §8Teleportationsanfragen", "§8» §fWähle eine Anfrage aus, welche du annehmen oder ablehnen möchtest.");
            tpas.forEach(e -> {
                tpasWindow.addButton("§8» §fAnfrage von\n§a" + e, g -> {
                    final ModalWindow acceptWindow = new ModalWindow.Builder("§7» §8Teleportationsanfragen", "§fMöchtest du die Teleportationsanfrage von §a" + e + " §fannehmen oder ablehnen?",
                            "§8» §aAnnehmen", "§8» §cAblehnen")
                            .onYes(v -> {
                                this.serverCore.getTeleportationAPI().acceptTpa(player.getName(), e);
                                this.serverCore.getTeleportationAPI().teleportToTpa(player, e);
                                player.sendMessage(Language.get("tpa.accepted", e));
                            })
                            .onNo(v -> {
                                this.serverCore.getTeleportationAPI().denyTpa(player.getName(), e);
                                player.sendMessage(Language.get("tpa.denied", e));
                            })
                            .build();
                    acceptWindow.send(player);
                });
            });
            tpasWindow.build().send(player);
        }
        return true;
    }
}
