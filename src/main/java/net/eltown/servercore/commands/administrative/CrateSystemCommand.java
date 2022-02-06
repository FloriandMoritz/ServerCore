package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.crates.data.CrateReward;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CrateSystemCommand extends Command {

    private final ServerCore serverCore;

    public CrateSystemCommand(final ServerCore serverCore) {
        super("cratesystem");
        this.serverCore = serverCore;
        this.setPermission("core.command.crate");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8CrateSystem", "\n\n")
                    .addButton("§8» §fGlücksboxen vergeben", this::openGiveCrate)
                    .addButton("§8» §fGlücksboxen bearbeiten", this::openEditSelectCrateType)
                    .build();
            window.send(player);
        }
        return false;
    }

    public void openGiveCrate(final Player player) {
        final List<String> crates = List.of("common", "uncommon", "epic", "legendary");
        final CustomWindow window = new CustomWindow("§7» §8Glücksboxen vergeben");
        window.form()
                .input("§8» §fGebe einen Spieler an, der die Glücksboxen erhalten soll.", player.getName(), player.getName())
                .stepSlider("§8» §fWelche Art der Glücksboxen soll vergeben werden?", crates.toArray(new String[0]))
                .slider("§8» §fWie viele Glücksboxen der o. g. Art sollen vergeben werden?", 1, 50, 1, 1);

        window.onSubmit((g, h) -> {
            final String target = h.getInput(0);
            final String crate = crates.get(h.getStepSlide(1));
            final int amount = (int) h.getSlider(2);

            if (target.isEmpty()) {
                player.sendMessage("§8» §fCore §8| §7Deine Angaben sind fehlerhaft. Bitte überprüfe diese.");
                return;
            }

            this.serverCore.getCrateAPI().addCrate(target, crate, amount);
            player.sendMessage("§8» §fCore §8| §7Dem Spieler wurden die Crates hinzugefügt.");
        });
        window.send(player);
    }

    public void openEditSelectCrateType(final Player player) {
        final List<String> crates = List.of("common", "uncommon", "epic", "legendary");
        final CustomWindow window = new CustomWindow("§7» §8Glücksboxen bearbeiten");
        window.form()
                .stepSlider("§8» §fWelche Art der Glücksboxen soll bearbeitet werden?", crates.toArray(new String[0]));

        window.onSubmit((g, h) -> {
            final String crate = crates.get(h.getStepSlide(0));
            this.openCrateRewards(player, crate);
        });
        window.send(player);
    }

    public void openCrateRewards(final Player player, final String crate) {
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Crate Gewinne", "§8» §fCrate: §9" + crate.toUpperCase() + "\n\n");
        window.addButton("§8» §fNeuen Gewinn erstellen", e -> {
            this.openInsertCrateReward(player, crate);
        });

        this.serverCore.getCrateAPI().getCrateRewards(crate, crateRewards -> {
            crateRewards.forEach(crateReward -> {
                window.addButton("§8» " + crateReward.getDisplayName() + "\n§8[§f" + crateReward.getId() + "§8]", e -> {
                    this.openUpdateCrateReward(player, crateReward);
                });
            });
        });
        window.build().send(player);
    }

    public void openInsertCrateReward(final Player player, final String crate) {
        final CustomWindow window = new CustomWindow("§7» §8Neuen Gewinn erstellen");
        window.form()
                .label("§8» §fCrate: §9" + crate.toUpperCase())
                .input("§8» §fAnzeigename des Gewinns", "16x Eichenholz")
                .slider("§8» §fChance, dass der Gewinn gezogen wird", 1, 100, 1, 50)
                .input("§8» §fGewinn-Daten\n§7item;<slot>\n§7money;amount\nxp;amount\ncrate;type;amount", "money;300");

        window.onSubmit((g, h) -> {
            final String displayName = h.getInput(1);
            final int chance = (int) h.getSlider(2);
            String data = h.getInput(3);

            if (data.startsWith("item")) {
                final ItemStack item = player.getInventory().getItem((Integer.parseInt(data.split(";")[1])));
                data = "item;" + SyncAPI.ItemAPI.itemStackToBase64(item);
            }

            this.serverCore.getCrateAPI().insertCrateReward(this.serverCore.createId(10, "F-CR"), crate, displayName, chance, data);
            player.sendMessage("§8» §fCore §8| §7Neuer Gewinn wurde erstellt.");
        });
        window.send(player);
    }

    public void openUpdateCrateReward(final Player player, final CrateReward crateReward) {
        final CustomWindow window = new CustomWindow("");
        window.form()
                .label("§8» §fID: §9" + crateReward.getId())
                .label("§8» §fCrate: §9" + crateReward.getCrate().toUpperCase())
                .input("§8» §fAnzeigename des Gewinns", "16x Eichenholz", crateReward.getDisplayName())
                .slider("§8» §fChance, dass der Gewinn gezogen wird", 1, 100, 1, crateReward.getChance())
                .input("§8» §fGewinn-Daten\n§7item;<slot>\n§7money;amount\nxp;amount\ncrate;type;amount", "money;300", crateReward.getData())
                .toggle("§8» §fItem-Gewinn aktualisieren", false)
                .toggle("§8» §cReward löschen", false);

        window.onSubmit((g, h) -> {
            final String displayName = h.getInput(2);
            final int chance = (int) h.getSlider(3);
            String data = h.getInput(4);
            final boolean setItem = h.getToggle(5);
            final boolean delete = h.getToggle(6);

            if (delete) {
                this.serverCore.getCrateAPI().deleteCrateReward(crateReward.getId());
                player.sendMessage("§8» §fCore §8| §7Der Gewinn wurde gelöscht.");
                return;
            }

            if (setItem && data.startsWith("item")) {
                final ItemStack item = player.getInventory().getItem((Integer.parseInt(data.split(";")[1])));
                data = "item;" + SyncAPI.ItemAPI.itemStackToBase64(item);
            }

            this.serverCore.getCrateAPI().updateCrateReward(crateReward.getId(), crateReward.getCrate(), displayName, chance, data);
            player.sendMessage("§8» §fCore §8| §7Gewinn wurde aktualisiert.");
        });
        window.send(player);
    }
}
