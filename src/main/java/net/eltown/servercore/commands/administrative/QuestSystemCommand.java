package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;

public class QuestSystemCommand extends Command {

    private final ServerCore serverCore;

    private final HashMap<String, Location> pos1 = new HashMap<>();
    private final HashMap<String, Location> pos2 = new HashMap<>();

    public QuestSystemCommand(final ServerCore serverCore) {
        super("questsystem");
        this.setPermission("core.command.questsystem");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8GiftkeySystem", "\n\n")
                    .addButton("§8» §fQuest erstellen", this::openCreateQuestWindow)
                    .addButton("§8» §fQuest bearbeiten", this::openSelectEditQuestWindow)
                    .build();
            window.send(player);
        }
        return true;
    }

    public void openCreateQuestWindow(final Player player) {
        final CustomWindow window = new CustomWindow("§7» §8Quest erstellen");
        window.form()
                .input("§8» §fBitte gebe eine QuestID an, die einmalig ist.", "QuestNameID")
                .input("§8» §fBitte erstelle einen Anzeigenamen der Quest.", "Anzeigename")
                .label("§8» §fBitte gebe die Aufgabe an, die in der Quest gemacht werden soll.")
                .input("§7- bring#§ctoggleItemTrue\n§7- collect#§ctoggleItemTrue\n§7- place#§ctoggleItemTrue\n§7- explore (Gesetzte Positionen)\n§7- craft#§ctoggleItemTrue\n§7- execute#<command>\n", "Data")
                .input("§8» §fLege eine Aufgabenstellung fest.", "Beschreibung")
                .toggle("§8» §fDas Item in deiner Hand als Quest-Data setzen.", false)
                .input("§8» §fBitte gebe an, wie hoch der benötigte Wert liegen soll, bis die Quest erledigt ist. (Z. B.: Baue §c15 §fBlöcke ab.)", "15")
                .input("§8» §fBitte gebe an, wie lange ein Spieler für diese Quest Zeit hat. (In Stunden)", "3")
                .label("§8» §fBitte gebe die Belohnungen an, die in der Quest beinhaltet sind.")
                .input("Allgemeines Trennzeichen: -#-\n\n§7- xp#<amount>\n§7- money#<amount>\n§7- item#<itemSlot>\n§7- gutschein#<gutscheinData>\n§7- permission#<key>#<description>\n§7- crate#<type>#<amount>\n", "Data")
                .input("§8» §fSoll diese Quest mit einem QuestNPC verlinkt werden? Wenn nicht, gebe 'null' an.", "Link");

        window.onSubmit((g, h) -> {
            try {
                final String nameId = h.getInput(0);
                final String displayName = h.getInput(1);
                String data = h.getInput(3);
                final String description = h.getInput(4);
                final boolean dataSetItem = h.getToggle(5);
                final int required = Integer.parseInt(h.getInput(6));
                final long expire = Long.parseLong(h.getInput(7));
                final String rawRewardData = h.getInput(9);
                String link = h.getInput(10);

                final StringBuilder rewardDataString = new StringBuilder();
                for (final String s : rawRewardData.split("-#-")) {
                    final String[] f = s.split("#");
                    if (f[0].equals("item")) {
                        final ItemStack item = player.getInventory().getItem(Integer.parseInt(f[1]));
                        if (item.getType() != Material.AIR) {
                            rewardDataString.append("item#").append(SyncAPI.ItemAPI.itemStackToBase64(item)).append("-#-");
                        }
                    } else rewardDataString.append(s).append("-#-");
                }
                final String rewardData = rewardDataString.substring(0, rewardDataString.length() - 3);

                if (nameId.isEmpty() || displayName.isEmpty() || description.isEmpty() || data.isEmpty() || rawRewardData.isEmpty()) {
                    player.sendMessage("Fehler beim Erstellen: Bitte überprüfe deine Angaben.");
                    return;
                }

                if (link.isEmpty()) link = "null";

                if (dataSetItem) {
                    final ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR) {
                        player.sendMessage("Deine Angaben sind fehlerhaft: Bitte halte ein Item in deiner Hand.");
                        return;
                    }
                    data = data + "#" + SyncAPI.ItemAPI.itemStackToBase64(item);
                }

                if (data.startsWith("explore")) {
                    if (!this.pos1.containsKey(player.getName()) || !this.pos2.containsKey(player.getName())) throw new NullPointerException("No locations set");
                    final Location pos1 = this.pos1.get(player.getName());
                    final Location pos2 = this.pos2.get(player.getName());
                    data = "explore#" + pos1.getX() + ">" + pos1.getY() + ">" + pos1.getZ() + ">" + pos1.getWorld().getName() + "#" + pos2.getX() + ">" + pos2.getY() + ">" + pos2.getZ() + ">" + pos2.getWorld().getName();
                }

                final String finalData = data;
                final String finalLink = link;
                this.serverCore.getQuestAPI().getQuest(nameId, quest -> {
                    if (quest == null) {
                        final Quest.QuestData questData = new Quest.QuestData(nameId, this.serverCore.createId(12, "SQ"), description, finalData, required);
                        this.serverCore.getQuestAPI().createQuest(nameId, displayName, Collections.singletonList(questData), (expire * 60 * 60 * 1000), rewardData, finalLink);
                        player.sendMessage("Die Quest wurde soeben erstellt! [" + nameId + "]");
                    } else player.sendMessage("Fehler beim Erstellen: Diese QuestID wird bereits verwendet.");
                });
            } catch (final Exception e) {
                player.sendMessage("Fehler beim Erstellen: Bitte überprüfe deine Angaben.");
            }
        });
        window.send(player);
    }

    public void openSelectEditQuestWindow(final Player player) {
        final CustomWindow selectQuestWindow = new CustomWindow("§7» §8Quest bearbeiten");
        selectQuestWindow.form()
                .input("§8» §fBitte gebe eine QuestID an, um eine Quest zu bearbeiten.", "Quest");

        selectQuestWindow.onSubmit((g, h) -> {
            final String questNameId = h.getInput(0);

            if (questNameId.isEmpty()) {
                player.sendMessage("Fehler beim Aufrufen: Diese QuestID existiert nicht.");
                return;
            }

            this.serverCore.getQuestAPI().getQuest(questNameId, quest -> {
                if (quest == null) {
                    player.sendMessage("Fehler beim Aufrufen: Diese QuestID existiert nicht.");
                    return;
                }

                final SimpleWindow editSelectWindow = new SimpleWindow.Builder("§7» §8Quest bearbeiten", "\n\n")
                        .addButton("§8» §fQuest bearbeiten", e -> this.openEditQuestWindow(player, quest))
                        .addButton("§8» §fSubQuests aufrufen", e -> this.openSubQuests(player, quest))
                        .build();
                editSelectWindow.send(player);
            });
        });
        selectQuestWindow.send(player);
    }

    public void openEditQuestWindow(final Player player, final Quest quest) {
        final CustomWindow window = new CustomWindow("§7» §8Quest bearbeiten");
        window.form()
                .input("§8» §fAnzeigename der Quest", "Anzeigename", quest.getDisplayName())
                .label("§8» §fBitte gebe die Belohnungen an, die in der Quest beinhaltet sind.")
                .input("Allgemeines Trennzeichen: -#-\n\n§7- xp#<amount>\n§7- money#<amount>\n§7- item#<itemSlot>\n§7- gutschein#<gutscheinData>\n§7- permission#<key>#<description>\n§7- crate#<type>#<amount>\n", "xp#200", quest.getRewardData())
                .toggle("§8» §fQuest-Reward-Items aktualisieren.", false)
                .input("§8» §fSoll diese Quest mit einem QuestNPC verlinkt werden? Wenn nicht, gebe 'null' an.", "Lola", quest.getLink())
                .toggle("§8» §cDiese Quest löschen.", false);

        window.onSubmit((g, h) -> {

        });
        window.send(player);
    }

    public void openSubQuests(final Player player, final Quest quest) {
        final SimpleWindow.Builder subQuestsWindow = new SimpleWindow.Builder("§7» §8SubQuests aufrufen", "\n\n");

        subQuestsWindow.addButton("§8» §fSubQuest erstellen", e -> {
            final CustomWindow addSubQuestWindow = new CustomWindow("§7» §8SubQuest erstellen");
            addSubQuestWindow.form()
                    .label("")
                    .input("§7- bring#§ctoggleItemTrue\n§7- collect#§ctoggleItemTrue\n§7- place#§ctoggleItemTrue\n§7- explore (Gesetzte Positionen)\n§7- craft#§ctoggleItemTrue\n§7- execute#<command>\n", "Data")
                    .input("§8» §fLege eine Aufgabenstellung fest.", "Beschreibung")
                    .toggle("§8» §fDas Item in deiner Hand als Quest-Data setzen.", false)
                    .input("§8» §fBitte gebe an, wie hoch der benötigte Wert liegen soll, bis die Quest erledigt ist. (Z. B.: Baue §c15 §fBlöcke ab.)", "15")
                    .input("§8» §fBitte gebe an, wie lange ein Spieler für diese Quest Zeit hat. (In Stunden)", "3");

            addSubQuestWindow.onSubmit((g, h) -> {

            });
            addSubQuestWindow.send(player);
        });
        quest.getData().forEach(e -> {
            subQuestsWindow.addButton(e.getQuestNameId() + "/" + e.getQuestSubId(), g -> {
                final CustomWindow editSubQuestWindow = new CustomWindow("§7» §8SubQuest bearbeiten");
                editSubQuestWindow.form()
                        .label("§8» §fQuest NameID: §7" + quest.getNameId() + "\n§cWenn Daten nicht bearbeitet werden sollen, dann verändere das gegebene Feld nicht.")
                        .input("§8» §fQuest SubID:", "SUBID", e.getQuestSubId())
                        .input("§8» §fQuest Beschreibung:", "Beschreibung", e.getDescription())
                        .input("§8» §fData:\n§8» §fTyp: §7" + e.getData().split("#")[0], "Data")
                        .input("§8» §fBenötigte Anzahl:", "1", String.valueOf(e.getRequired()))
                        .toggle("§8» §cDiese SubQuest löschen.", false);

                editSubQuestWindow.onSubmit((n, m) -> {

                });
                editSubQuestWindow.send(player);
            });
        });
        subQuestsWindow.build().send(player);
    }
}
