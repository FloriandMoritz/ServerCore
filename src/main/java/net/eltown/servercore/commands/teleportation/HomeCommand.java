package net.eltown.servercore.commands.teleportation;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.Home;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class HomeCommand extends Command {

    private final ServerCore serverCore;

    public HomeCommand(final ServerCore serverCore) {
        super("home", "Verwalte deine Homes", "", List.of("h", "zuhause"));
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
        final SimpleWindow.Builder simpleWindow = new SimpleWindow.Builder("§7» §8Homes", "§8» §7Hier kannst du deine Homes serverweit verwalten. Wähle eine Option aus, um fortzufahren.");
        simpleWindow.addButton("§8» §fZum Home teleportieren", this::openHomeTeleporter);
        simpleWindow.addButton("§8» §fHome erstellen", this::openCreateHome);
        simpleWindow.addButton("§8» §fHomes verwalten", this::openHomeEditor);
        if (player.isOp()) simpleWindow.addButton("§8» §4Server verwalten", this::openAdminSettings);
        simpleWindow.build().send(player);
    }

    private void openHomeTeleporter(final Player player) {
        final Set<Home> homes = this.serverCore.getTeleportationAPI().getHomes(player.getName());

        if (homes.size() == 0) {
            player.sendMessage(Language.get("home.no.homes"));
            return;
        }

        final SimpleWindow.Builder form = new SimpleWindow.Builder("§7» §8Teleportation", "§8» §7Wähle aus der Liste ein Home aus, zu welchem du dich teleportieren möchtest.");
        homes.forEach(e -> {
            form.addButton("§8» §f" + e.getName(), g -> {
                this.serverCore.getTeleportationAPI().teleportToHome(player, e);
            });
        });
        form.build().send(player);
    }

    private void openCreateHome(final Player player) {
        final CustomWindow createHomeWindow = new CustomWindow("§7» §8Home erstellen");
        createHomeWindow.form()
                .input("§8» §fBitte gebe deinem Home einen Namen.", "Name");

        createHomeWindow.onSubmit((g, h) -> {
            final String name = h.getInput(0);

            if (name.isEmpty() || name.contains("/")) {
                player.sendMessage(Language.get("home.input.invalid"));
                return;
            }

            this.serverCore.getTeleportationAPI().createHome(name, player, alreadySet -> {
                if (!alreadySet) {
                    player.sendMessage(Language.get("home.set", name));
                } else {
                    player.sendMessage(Language.get("home.already.set"));
                }
            });
        });
        createHomeWindow.send(player);
    }

    private void openHomeEditor(final Player player) {
        final Set<Home> homes = this.serverCore.getTeleportationAPI().getHomes(player.getName());

        if (homes.size() == 0) {
            player.sendMessage(Language.get("home.no.homes"));
            return;
        }

        final SimpleWindow.Builder form = new SimpleWindow.Builder("§7» §8Teleportation", "§8» §7Wähle aus der Liste ein Home aus, welches du bearbeiten möchtest.");
        homes.forEach(e -> {
            form.addButton("§8» §f" + e.getName(), g -> {
                final CustomWindow editHomeWindow = new CustomWindow("§7» §8Home bearbeiten");
                editHomeWindow.form()
                        .label("§8» §fDiese Einstellungen beziehen sich auf das Home §9" + e.getName() + "§r§f.")
                        .input("§8» §fWenn du den Namen nicht ändern möchtest, dann ignoriere dieses Feld.", "Name", e.getName())
                        .toggle("§8» §fDieses Home auf meine aktuelle Position aktualisieren.", false)
                        .toggle("§8» §cDieses Home endgültig löschen.", false);

                editHomeWindow.onSubmit((q, w) -> {
                    final String name = w.getInput(1);
                    final boolean position = w.getToggle(2);
                    final boolean delete = w.getToggle(3);

                    if (!delete) {
                        if (!name.equals(e.getName())) {
                            if (!name.isEmpty() && !name.contains("/") && !name.contains(">") && !name.contains("<")) {
                                this.serverCore.getTeleportationAPI().updateHomeName(e.getName(), e.getPlayer(), name);
                                player.sendMessage(Language.get("home.renamed", name));
                            } else player.sendMessage(Language.get("home.input.invalid"));
                        }

                        if (position) {
                            this.serverCore.getTeleportationAPI().updateHomePosition(e.getName(), e.getPlayer(), player.getLocation());
                            player.sendMessage(Language.get("home.updated"));
                        }
                    } else {
                        this.serverCore.getTeleportationAPI().deleteHome(e.getName(), e.getPlayer());
                        player.sendMessage(Language.get("home.deleted", e.getName()));
                    }
                });
                editHomeWindow.send(player);
            });
        });
        form.build().send(player);
    }

    private void openAdminSettings(final Player player) {
        final SimpleWindow adminSettingsWindow = new SimpleWindow.Builder("§7» §8Admin Einstellungen", "§cSobald du auf einen Button klickst, wird die besagte Aktion sofort ausgeführt.\n§cServer: §7" + this.serverCore.getServerName())
                .addButton("§8» §4Alle Homes dieses Servers entfernen", e -> {
                    this.serverCore.getTeleportationAPI().deleteServerHomes();
                    player.sendMessage(Language.get("home.server.deleted"));
                })
                .build();
        adminSettingsWindow.send(player);
    }

}
