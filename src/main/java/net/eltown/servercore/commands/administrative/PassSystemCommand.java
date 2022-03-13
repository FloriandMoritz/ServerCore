package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.passes.Season;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PassSystemCommand extends Command {

    private final ServerCore serverCore;

    public PassSystemCommand(final ServerCore serverCore) {
        super("passsystem");
        this.serverCore = serverCore;
        this.setPermission("core.command.passsystem");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            this.openMain(player);
        }
        return true;
    }

    public void openMain(final Player player) {
        this.serverCore.getPassAPI().getCurrentSeason((call, season) -> {
            if (season != null) {
                final SimpleWindow window = new SimpleWindow.Builder("§7» §8Ära Einstellungen", "§8» §fÄra: §7" + season.getName() + "§r\n"
                        + "§8» §fBeschreibung: §7" + season.getDescription() + "§r\n\n")
                        .addButton("§8» §fQuests bearbeiten", e -> this.openEditQuests(player, season))
                        .addButton("§8» §fBelohnungen bearbeiten", e -> this.openSeasonRewards(player, season))
                        .addButton("§8» §fErweiterte Einstellungen", e -> this.openAdvancedSettings(player, season))
                        .build();
                window.send(player);
            } else {
                final CustomWindow window = new CustomWindow("§7» §8Ära erstellen");
                window.form()
                        .label("§8» §cAktuell existiert keine Ära. Hier kannst du eine erstellen und anschließend einrichten.")
                        .input("§8» §fGebe einen treffenden Namen an.")
                        .input("§8» §fGebe eine Beschreibung an.")
                        .slider("§8» §fGebe die Dauer dieser Ära (in Tage) an.", 1, 120, 1, 50);

                window.onSubmit((g, h) -> {
                    final String name = h.getInput(1);
                    final String description = h.getInput(2);
                    final long expire = this.serverCore.getDuration("d", (int) h.getSlider(3));

                    if (name.isEmpty() || description.isEmpty()) {
                        player.sendMessage("§8» §fCore §8| §7Bitte gebe gültige Daten an.");
                        return;
                    }

                    this.serverCore.getPassAPI().createSeason(name, description, expire);
                    player.sendMessage("§8» §fCore §8| §7Die Ära '" + name + "§r§7' wurde erstellt.");
                });
                window.send(player);
            }
        });
    }

    public void openEditQuests(final Player player, final Season season) {
        final List<String> quests = new ArrayList<>(List.of("- Keine -"));
        quests.addAll(season.getQuests());
        final CustomWindow window = new CustomWindow("§7» §8Quests bearbeiten");
        window.form()
                .dropdown("§8» §fWähle eine verlinkte Quest aus, die entfernt werden soll.", quests.toArray(new String[0]))
                .input("§8» §fGebe eine QuestID an, um diese zu verlinken. Lasse dieses Feld frei, wenn keine hinzugefügt werden soll.", "sq1");

        window.onSubmit((g, h) -> {
            final String toRemove = quests.get(h.getDropdown(0));
            final String toAdd = h.getInput(1);

            if (!toRemove.isEmpty() && !toRemove.equals("- Keine -")) {
                this.serverCore.getPassAPI().removeQuestFromSeason(toRemove);
                player.sendMessage("§8» §fCore §8| §7Die Quest '" + toRemove + "' wurde entfernt.");
            }
            if (!toAdd.isEmpty()) {
                if (!quests.contains(toAdd)) {
                    this.serverCore.getPassAPI().addQuestToSeason(toAdd);
                    player.sendMessage("§8» §fCore §8| §7Die Quest '" + toAdd + "' wurde hinzugefügt.");
                } else player.sendMessage("§8» §fCore §8| §7Diese Quest ist bereits hinzugefügt.");
            }
        });
        window.send(player);
    }

    public void openSeasonRewards(final Player player, final Season season) {
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Belohnungen bearbeiten", "");
        window.addButton("§8» §fBelohnung erstellen", e -> {
            this.openCreateReward(player, season);
        });
        season.getRewards().values().forEach(seasonReward -> {
            window.addButton("§8» §fPunkte: §7" + seasonReward.getPoints() + "§r\n" + seasonReward.getDescription(), e -> {
                this.openSeasonReward(player, seasonReward);
            });
        });
        window.addButton("§8» §cZurück", this::openMain);
        window.build().send(player);
    }

    public void openCreateReward(final Player player, final Season season) {
        final List<String> types = new ArrayList<>(List.of("S", "P"));
        final CustomWindow window = new CustomWindow("§7» §8Belohnung erstellen");
        window.form()
                .input("§8» §fBenötigte Punkte", "35")
                .dropdown("§8» §fTyp der Belohnung", types.toArray(new String[0]))
                .input("§8» §fBildlink (ohne http://)", "eltown.net:3000/img/...")
                .input("§8» §fBeschreibung", "300$ Bargeld")
                .input("§8» §fBelohnungs-Daten", "xp#300")
                .toggle("§8» §fDas Item in der Hand als Belohnungs-Daten setzen.", false)
                .label("§7xp#<amount>\n§7item#toggle\n§7money#<amount>\n§7crate#type#<amount>");

        window.onSubmit((g, h) -> {
            try {
                final int points = Integer.parseInt(h.getInput(0));
                final String type = types.get(h.getDropdown(1));
                final String image = h.getInput(2);
                final String description = h.getInput(3);
                String data = h.getInput(4);
                final boolean setItem = h.getToggle(5);

                if (setItem) {
                    final ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR) {
                        player.sendMessage("§8» §fCore §8| §7Das Item in deiner Hand ist ungültig.");
                        return;
                    }
                    data = "item#" + SyncAPI.ItemAPI.itemStackToBase64(item);
                }
                if (data.isEmpty()) data = "null";

                this.serverCore.getPassAPI().addRewardToSeason(points, type, image, description, data);
                player.sendMessage("§8» §fCore §8| §7Die Belohnung wurde erstellt.");
            } catch (final Exception e) {
                player.sendMessage("§8» §fCore §8| §7Die angegebenen Daten sind fehlerhaft. Bitte überprüfe diese.");
            }
        });
        window.send(player);
    }

    public void openSeasonReward(final Player player, final Season.SeasonReward seasonReward) {
        final List<String> types = new ArrayList<>(List.of("S", "P"));
        final CustomWindow window = new CustomWindow("§7» §8Belohnung bearbeiten");
        window.form()
                .input("§8» §fBenötigte Punkte", "35", String.valueOf(seasonReward.getPoints()))
                .dropdown("§8» §fTyp der Belohnung (§9" + seasonReward.getType() + "§f)", types.toArray(new String[0]))
                .input("§8» §fBildlink", "http://eltown.net:3000/img/...", seasonReward.getImage())
                .input("§8» §fBeschreibung", "300$ Bargeld", seasonReward.getDescription())
                .input("§8» §fBelohnungs-Daten", "xp#300", seasonReward.getData())
                .toggle("§8» §fDas Item in der Hand als Belohnungs-Daten setzen.", false)
                .toggle("§8» §cDiesen Reward aktualisieren.", false)
                .toggle("§8» §cDiesen Reward löschen.", false)
                .label("§7xp#<amount>\n§7item#toggle\n§7money#<amount>\n§7crate#type#<amount>");

        window.onSubmit((g, h) -> {
            try {
                final int points = Integer.parseInt(h.getInput(0));
                final String type = types.get(h.getDropdown(1));
                final String image = h.getInput(2);
                final String description = h.getInput(3);
                String data = h.getInput(4);
                final boolean setItem = h.getToggle(5);
                final boolean update = h.getToggle(6);
                final boolean delete = h.getToggle(7);

                if (delete) {
                    player.sendMessage("§8» §fCore §8| §7Die Belohnung wurde gelöscht.");
                    this.serverCore.getPassAPI().removeRewardFromSeason(seasonReward.getId());
                    return;
                }

                if (update) {
                    if (setItem) {
                        final ItemStack item = player.getInventory().getItemInMainHand();
                        if (item.getType() == Material.AIR) {
                            player.sendMessage("§8» §fCore §8| §7Das Item in deiner Hand ist ungültig.");
                            return;
                        }
                        data = "item#" + SyncAPI.ItemAPI.itemStackToBase64(item);
                    }
                    if (data.isEmpty()) data = "null";

                    this.serverCore.getPassAPI().updateReward(seasonReward.getId(), points, type, image, description, data);
                    player.sendMessage("§8» §fCore §8| §7Die Belohnung wurde aktualisiert.");
                }
            } catch (final Exception e) {
                player.sendMessage("§8» §fCore §8| §7Die angegebenen Daten sind fehlerhaft. Bitte überprüfe diese.");
            }
        });
        window.send(player);
    }

    public void openAdvancedSettings(final Player player, final Season season) {
        final CustomWindow window = new CustomWindow("§7» §8Erweiterte Einstellungen");
        window.form()
                .label("§8» §fÄra läuft ab in: §9" + this.serverCore.getRemainingTimeFuture(season.getExpire()))
                .input("§8» §fName der Ära", "", season.getName())
                .input("§8» §fBeschreibung der Ära", "", season.getDescription())
                .slider("§8» §fGebe eine neue Dauer dieser Ära (in Tage) an (ab diesem Zeitpunkt).", 0, 120, 1, 0)
                .toggle("§8» §cAktuelle Ära deaktivieren und löschen.", false);

        window.onSubmit((g, h) -> {
            final String name = h.getInput(1);
            final String description = h.getInput(2);
            final boolean delete = h.getToggle(4);

            if (delete) {
                this.serverCore.getPassAPI().deleteSeason();
                player.sendMessage("§8» §fCore §8| §7Die Ära wurde deaktiviert und gelöscht.");
                return;
            }

            if (!name.equals(season.getName())) {
                this.serverCore.getPassAPI().updateName(name);
                player.sendMessage("§8» §fCore §8| §7Der Name wurde aktualisiert.");
            }

            if (!description.equals(season.getDescription())) {
                this.serverCore.getPassAPI().updateDescription(description);
                player.sendMessage("§8» §fCore §8| §7Die Beschreibung wurde aktualisiert.");
            }

            if (h.getSlider(3) != 0) {
                final long expire = this.serverCore.getDuration("d", (int) h.getSlider(3));
                this.serverCore.getPassAPI().updateExpire(expire);
                player.sendMessage("§8» §fCore §8| §7Das Ablaufdatum wurde aktualisiert.");
            }
        });
        window.send(player);
    }

}
