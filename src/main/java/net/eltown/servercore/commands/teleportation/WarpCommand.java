package net.eltown.servercore.commands.teleportation;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.Warp;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class WarpCommand extends Command {

    private final ServerCore serverCore;

    public WarpCommand(final ServerCore serverCore) {
        super("warp", "Teleportiere dich rund um den Server", "", List.of("w", "servers"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.openMain(player);
        }
        return true;
    }

    private void openMain(final Player player) {
        final Set<Warp> warps = this.serverCore.getTeleportationAPI().getWarps();

        if (warps.size() == 0) {
            player.sendMessage(Language.get("warp.no.warps"));
            if (player.isOp()) this.openAddWarp(player);
            return;
        }

        final SimpleWindow.Builder warpWindow = new SimpleWindow.Builder("§7» §8Warps", "§8» §fSuche dir einen Warp-Punkt aus, um sich dorthin zu teleportieren.\n");
        warps.forEach(e -> warpWindow.addButton("§8» §f" + e.getDisplayName(), "http://" + e.getImageUrl(), g -> this.serverCore.getTeleportationAPI().teleportToWarp(player, e)));

        if (player.isOp()) {
            warpWindow.addButton("§8» §4Einstellungen", this::openSettings);
        }

        warpWindow.build().send(player);
    }

    private void openSettings(final Player player) {
        final SimpleWindow settingsWindow = new SimpleWindow.Builder("§7» §8Einstellungen", "")
                .addButton("§8» §fWarp erstellen", this::openAddWarp)
                .addButton("§8» §fWarps verwalten", this::openUpdateWarp)
                .addButton("§8» §cZurück", this::openMain)
                .build();
        settingsWindow.send(player);
    }

    private void openAddWarp(final Player player) {
        final CustomWindow addWarpWindow = new CustomWindow("§7» §8Warp erstellen");
        addWarpWindow.form()
                .input("§8» §fGebe dem Warp einen Namen.", "Name")
                .input("§8» §fGebe einen Anzeigenamen für dieses Warp an.", "Anzeigename")
                .input("§8» §fLege ein Bild für diesen Warp fest, welches in der UI angezeigt werden soll.", "eltown.net/bild.png");

        addWarpWindow.onSubmit((g, h) -> {
            final String name = h.getInput(0);
            final String displayName = h.getInput(0);
            final String imageUrl = h.getInput(0);

            if (name != null && displayName != null && imageUrl != null && (name.isEmpty() || displayName.isEmpty() || imageUrl.isEmpty())) {
                player.sendMessage(Language.get("warp.input.invalid"));
                return;
            }

            this.serverCore.getTeleportationAPI().createWarp(name, displayName, imageUrl, player.getLocation(), alreadySet -> {
                if (alreadySet) {
                    player.sendMessage(Language.get("warp.already.set"));
                } else {
                    player.sendMessage(Language.get("warp.set", name));
                }
            });
        });
        addWarpWindow.send(player);
    }

    private void openUpdateWarp(final Player player) {
        final Set<Warp> warps = this.serverCore.getTeleportationAPI().getWarps();
        final SimpleWindow.Builder selectWarpWindow = new SimpleWindow.Builder("§7» §8Warps verwalten", "§8» §fWähle aus der Liste ein Warp aus, welches du bearbeiten möchtest.");
        warps.forEach(warp -> selectWarpWindow.addButton("§8» §f" + warp.getName(), e -> {
            final CustomWindow editWarpWindow = new CustomWindow("§7» §8Warp bearbeiten");
            editWarpWindow.form()
                    .label("§8» §fWarp zur Bearbeitung: §9" + warp.getName() + "\n§r§cElemente, die nicht bearbeitet werden müssen, können frei gelassen werden.")
                    .input("§8» §fAnzeigename des Warps:", "Anzeigename", warp.getDisplayName())
                    .input("§8» §fBild-Link des Warps:", "eltown.net/bild.png")
                    .toggle("§8» §fWarp auf meine aktuelle Position aktualisieren?", false)
                    .toggle("§8» §cWarp endgültig entfernen?", false);

            editWarpWindow.onSubmit((g, h) -> {
                final String displayName = h.getInput(1);
                final String imageUrl = h.getInput(2);
                final boolean updatePosition = h.getToggle(3);
                final boolean delete = h.getToggle(4);

                if (!delete) {
                    assert displayName != null;
                    if (!displayName.isEmpty()) {
                        if (!displayName.equals(warp.getDisplayName())) {
                            this.serverCore.getTeleportationAPI().updateWarpDisplayName(warp.getName(), displayName);
                            player.sendMessage(Language.get("warp.update.name", displayName));
                        }
                    }

                    assert imageUrl != null;
                    if (!imageUrl.isEmpty()) {
                        if (!imageUrl.equals(warp.getImageUrl())) {
                            this.serverCore.getTeleportationAPI().updateWarpImageUrl(warp.getName(), imageUrl);
                            player.sendMessage(Language.get("warp.update.image"));
                        }
                    }

                    if (updatePosition) {
                        this.serverCore.getTeleportationAPI().updateWarpPosition(warp.getName(), player.getLocation());
                        player.sendMessage("warp.update.location");
                    }
                } else {
                    this.serverCore.getTeleportationAPI().deleteWarp(warp.getName());
                    player.sendMessage(Language.get("warp.deleted"));
                }
            });
            editWarpWindow.send(player);
        }));
        selectWarpWindow.addButton("§8» §cZurück", this::openSettings);
        selectWarpWindow.build().send(player);
    }

}
