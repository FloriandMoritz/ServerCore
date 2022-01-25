package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GiftkeySystemCommand extends Command {

    private final ServerCore serverCore;

    public GiftkeySystemCommand(final ServerCore serverCore) {
        super("giftkeysystem");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8GiftkeySystem", "\n\n")
                    .addButton("§8» §fGiftkey erstellen", this::openCreateGiftKey)
                    .addButton("§8» §fInformationen einsehen", this::openGiftKeyInformation)
                    .build();
            window.send(player);
        }
        return true;
    }

    private void openCreateGiftKey(final Player player) {
        final CustomWindow window = new CustomWindow("§7» §8Giftkey erstellen");
        window.form()
                .slider("§8» §fBitte wähle aus, wie viel Inhalt ein Key haben soll.", 1, 10, 1, 1);

        window.onSubmit((g, h) -> {
            final CustomWindow createWindow = new CustomWindow("§7» §8Giftkey erstellen");
            createWindow.form()
                    .label("§8» §fReward-Formate:\n§7item;<slot>\n§7money;amount\nlevelxp;amount\ncrate;type;amount")
                    .input("§8» §fLege einen Key für diesen Gutschein fest.", "Eltown2022")
                    .slider("§8» §fBitte gebe an, wie viele Spieler diesen Gutschein einlösen können.", 1, 250, 1, 1)
                    .input("§8» §fGebe optional an, für welche Spieler dieser Gutschein vorbemerkt werden soll.", "EltownUser123;LolUser9283;ABCspieler")
                    .toggle("§8» §fSoll dieser Gutschein eine zeitliche Begrenzung haben?", false)
                    .slider("§8» §fBitte wähle eine zeitliche Begrenzung aus (Stunden).", 1, 720, 1, 3);
            for (int x = 0; x < (int) h.getSlider(0); x++) {
                createWindow.form().input("", "Reward " + (x + 1));
            }

            createWindow.onSubmit((k, l) -> {
                try {
                    final String key = l.getInput(1);
                    final int uses = (int) l.getSlider(2);
                    final boolean hasDuration = l.getToggle(4);
                    long duration = -1;
                    if (hasDuration) {
                        if (hasDuration) duration = ((int) l.getSlider(5) * 3600000L) + System.currentTimeMillis();
                    }
                    final StringBuilder rewards = new StringBuilder();
                    for (int x = 0; x < (int) h.getSlider(0); x++) {
                        if (l.getInput(6 + x).startsWith("item")) {
                            final String[] d = l.getInput(6 + x).split(";");
                            final ItemStack itemStack = player.getInventory().getItem(Integer.parseInt(d[1]));
                            if (itemStack != null || itemStack.getType() != Material.AIR) {
                                rewards.append("item;").append(SyncAPI.ItemAPI.itemStackToBase64(itemStack)).append(">:<");
                            }
                        } else rewards.append(l.getInput(6 + x)).append(">:<");
                    }
                    final String rewardString = rewards.substring(0, rewards.length() - 3);

                    final String rawMarks = l.getInput(3);
                    final StringBuilder marks = new StringBuilder();
                    for (final String s : rawMarks.split(";")) {
                        marks.append(s).append(">:<");
                    }
                    String marksString = marks.substring(0, marks.length() - 3);

                    if (marksString.isEmpty()) marksString = "none>:<none";

                    this.serverCore.getGiftKeyAPI().createKey(key.isEmpty() ? this.serverCore.createId(7) : key, uses, rewardString, marksString, duration, callBack -> {
                        if (callBack == null) {
                            player.sendMessage(Language.get("giftkey.created.invalid.key"));
                        } else {
                            player.sendMessage(Language.get("giftkey.created", callBack));
                        }
                    });
                } catch (final Exception e) {
                    player.sendMessage(Language.get("giftkey.invalid.input"));
                }
            });
            createWindow.send(player);
        });
        window.send(player);
    }

    private void openGiftKeyInformation(final Player player) {
        final CustomWindow window = new CustomWindow("§7» §8Giftkey Informationen");
        window.form()
                .input("§8» §fBitte gebe einen Key an.");

        window.onSubmit((g, h) -> {
            final String key = h.getInput(0);

            if (key.isEmpty()) {
                player.sendMessage(Language.get("giftkey.invalid.input"));
                return;
            }

            this.serverCore.getGiftKeyAPI().getKey(key, giftKey -> {
                if (giftKey == null) {
                    player.sendMessage(Language.get("giftkey.invalid.key"));
                } else {
                    final CustomWindow giftKeyWindow = new CustomWindow("§7» §8Giftkey Informationen");
                    giftKeyWindow.form()
                            .label("§8» §fFestgelegter Code: §7" + giftKey.getKey())
                            .label("§8» §fMaximale Benutzungen: §7" + giftKey.getMaxUses())
                            .label("§8» §fReward-Data: §7" + giftKey.getRewards())
                            .label("§8» §fMarks-Data: §7" + giftKey.getMarks())
                            .label("§8» §fUse-Data: §7" + giftKey.getUses())
                            .label(giftKey.getDuration() == -1 ? "§8» §fZeitlicher Ablauf: §7Keiner" : "§8» §fZeitlicher Ablauf in: §7" + this.serverCore.getRemainingTimeFuture(giftKey.getDuration()))
                            .toggle("§8» §cSoll dieser Gutscheincode gelöscht werden?", false);

                    giftKeyWindow.onSubmit((b, n) -> {
                        if (n.getToggle(6)) {
                            this.serverCore.getGiftKeyAPI().deleteKey(giftKey.getKey());
                            player.sendMessage(Language.get("giftkey.key.deleted"));
                        }
                    });
                    giftKeyWindow.send(player);
                }
            });
        });
        window.send(player);
    }
}
