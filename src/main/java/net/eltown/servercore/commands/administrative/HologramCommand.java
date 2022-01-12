package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.HologramAPI;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HologramCommand extends Command {

    private final ServerCore serverCore;

    public HologramCommand(final ServerCore serverCore) {
        super("hologram");
        this.serverCore = serverCore;
        this.setDescription("Hologramm Command");
        this.setPermission("core.command.hologram");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player player) {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8Hologramme", "")
                    .addButton("§8» §fHologramm erstellen", this::createHologram)
                    .addButton("§8» §fHologramm bearbeiten", this::openHologramSettings)
                    .build();
            window.send(player);
        }
        return false;
    }

    private void createHologram(final Player player) {
        final CustomWindow selectLinesWindow = new CustomWindow("§7» §8Hologramm erstellen");
        selectLinesWindow.form()
                .slider("§8» §fBitte wähle aus, wie viele Zeilen das Hologramm haben soll", 1, 10, 1, 1);

        selectLinesWindow.onSubmit((g, h) -> {
            final int lines = (int) h.getSlider(0);

            final CustomWindow createWindow = new CustomWindow("§7» §8Hologramm erstellen");
            createWindow.form()
                    .input("§8» §fBitte gebe dem Hologramm einen einmaligen Namen.", "ID");
            for (int x = 0; x < lines; x++) {
                createWindow.form().input("", "Zeile " + (x + 1));
            }

            createWindow.onSubmit((j, k) -> {
                final String id = k.getInput(0);

                if ((id != null && id.isEmpty()) || this.serverCore.getHologramAPI().hologramExists(id)) {
                    player.sendMessage(Language.get("holograms.invalid.input"));
                    return;
                }

                final List<String> textLines = new ArrayList<>();
                for (int x = 0; x < lines; x++) {
                    textLines.add(k.getInput(1 + x));
                }

                this.serverCore.getHologramAPI().createHologram(id, player.getLocation(), textLines);
                player.sendMessage(Language.get("holograms.created", id));
            });
            createWindow.send(player);
        });
        selectLinesWindow.send(player);
    }

    private void openHologramSettings(final Player player) {
        if (this.serverCore.getHologramAPI().holograms.keySet().size() == 0) {
            player.sendMessage(Language.get("holograms.no.holograms"));
            return;
        }

        final CustomWindow chooseHologramWindow = new CustomWindow("§8» §fHologramm bearbeiten");
        chooseHologramWindow.form()
                .input("§8» §fBitte wähle ein Hologramm aus, welches du bearbeiten möchtest.", "hologramID");

        chooseHologramWindow.onSubmit((g, h) -> {
            final String id = h.getInput(0);

            if ((id != null && id.isEmpty()) || !this.serverCore.getHologramAPI().hologramExists(id)) {
                player.sendMessage(Language.get("holograms.invalid.input"));
                return;
            }

            final HologramAPI.Hologram hologram = this.serverCore.getHologramAPI().holograms.get(id);
            final SimpleWindow settingsWindow = new SimpleWindow.Builder("§7» §8Hologramm bearbeiten", "§8» §fHologramm: §9" + id)
                    .addButton("§8» §fZeile hinzufügen", e -> {
                        final CustomWindow addLineWindow = new CustomWindow("§7» §8Zeile hinzufügen");
                        addLineWindow.form()
                                .input("§8» §fBitte gebe der neuen Zeile einen Text.", "Neue Zeile");

                        addLineWindow.onSubmit((j, k) -> {
                            final String line = k.getInput(0);
                            if (line != null && !line.isEmpty()) {
                                this.serverCore.getHologramAPI().addLine(id, line);
                                player.sendMessage(Language.get("holograms.line.added"));
                            }
                        });
                        addLineWindow.send(player);
                    })
                    .addButton("§8» §fZeile entfernen", e -> {
                        final CustomWindow removeLineWindow = new CustomWindow("§7» §8Zeile entfernen");
                        removeLineWindow.form()
                                .slider("§8» §fZeile, die entfernt werden soll", 1, hologram.getLines().size(), 1, 1);

                        removeLineWindow.onSubmit((j, k) -> {
                            final int line = (int) k.getSlider(0);
                            this.serverCore.getHologramAPI().removeLine(id, line);
                            player.sendMessage(Language.get("holograms.line.removed"));
                        });
                        removeLineWindow.send(player);
                    })
                    .addButton("§8» §fZeile ersetzen", e -> {
                        final CustomWindow selectLineWindow = new CustomWindow("§7» §8Zeile erstezen");
                        selectLineWindow.form()
                                .slider("§8» §fZeile, die erstezt werden soll", 1, hologram.getLines().size(), 1, 1);

                        selectLineWindow.onSubmit((j, k) -> {
                            final int line = (int) k.getSlider(0);

                            final CustomWindow replaceLineWindow = new CustomWindow("§7» §8Zeile erstezen");
                            replaceLineWindow.form()
                                    .input("§8» §fZeile bearbeiten", "Zeile", hologram.getLines().get(line - 1).getText());

                            replaceLineWindow.onSubmit((l, o) -> {
                                final String text = o.getInput(0);
                                this.serverCore.getHologramAPI().setLine(id, line, text);
                                player.sendMessage(Language.get("holograms.line.edited"));
                            });
                            replaceLineWindow.send(player);
                        });
                        selectLineWindow.send(player);
                    })
                    .addButton("§8» §fErweitert", e -> {
                        final CustomWindow extendedWindow = new CustomWindow("§7» §8Erweitert");
                        extendedWindow.form()
                                .toggle("§8» §fSoll die Position des Hologramms auf deine aktualisiert werden?", false)
                                .toggle("§8» §fSoll das Hologramm endgültig gelöscht werden?", false);

                        extendedWindow.onSubmit((j, k) -> {
                            final boolean position = k.getToggle(0);
                            final boolean delete = k.getToggle(1);

                            if (!delete) {
                                if (position) {
                                    this.serverCore.getHologramAPI().moveHologram(id, player.getLocation());
                                    player.sendMessage(Language.get("holograms.moved"));
                                }
                            } else {
                                this.serverCore.getHologramAPI().deleteHologram(id);
                                player.sendMessage(Language.get("holograms.deleted"));
                            }
                        });
                        extendedWindow.send(player);
                    })
                    .build();
            settingsWindow.send(player);
        });
        chooseHologramWindow.send(player);
    }
}
