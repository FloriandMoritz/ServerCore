package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LevelSystemCommand extends Command {

    private final ServerCore serverCore;

    public LevelSystemCommand(final ServerCore serverCore) {
        super("levelsystem");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.openUpdateReward(player);
        }
        return true;
    }

    private void openUpdateReward(final Player player) {
        final CustomWindow selectRewardWindow = new CustomWindow("§7» §8Level Reward Einstellungen");
        selectRewardWindow.form()
                .input("§8» §fBitte gebe ein Level an.", "12");

        selectRewardWindow.onSubmit((g, h) -> {
            try {
                final int i = Integer.parseInt(h.getInput(0));
                this.serverCore.getLevelAPI().getLevelReward(i, levelReward -> {
                    if (levelReward == null) {
                        final CustomWindow window = new CustomWindow("§7» §8Level Reward Einstellungen");
                        window.form()
                                .label("§8» §fLevel: §9" + i)
                                .input("§8» §fBeschreibung", "Beschreibung")
                                .input("§8» §fBelohnungs-Daten\n§7AllgemeinesTrennzeichen: #\n§7Gutschein-Belohnungs-Trennzeichen: >:<", "Data")
                                .toggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen", false);

                        window.onSubmit((n, m) -> {
                            final String description = m.getInput(1);
                            String data = m.getInput(2);
                            final boolean setItem = m.getToggle(3);

                            if (description.isEmpty()) {
                                player.sendMessage(Language.get("level.settings.invalid.input"));
                                return;
                            }

                            if (setItem) {
                                final ItemStack item = player.getInventory().getItemInMainHand();
                                if (item.getType() == Material.AIR) {
                                    player.sendMessage(Language.get("level.settings.invalid.input"));
                                    return;
                                }
                                data = "item#" + SyncAPI.ItemAPI.itemStackToBase64(item);
                            }
                            if (data.isEmpty()) data = "null";

                            player.sendMessage(Language.get("level.settings.updated"));
                            this.serverCore.getLevelAPI().updateReward(i, description, data);
                        });
                        window.send(player);
                    } else {
                        final CustomWindow window = new CustomWindow("§7» §8Level Reward Einstellungen");
                        window.form()
                                .label("§8» §fLevel: §9" + i)
                                .input("§8» §fBeschreibung", "Beschreibung", levelReward.getDescription())
                                .input("§8» §fBelohnungs-Daten\n§7AllgemeinesTrennzeichen: #\n§7Gutschein-Belohnungs-Trennzeichen: >:<", "Data", levelReward.getData())
                                .toggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen", false)
                                .toggle("§8» §cDiesen Reward löschen", false);

                        window.onSubmit((n, m) -> {
                            final String description = m.getInput(1);
                            String data = m.getInput(2);
                            final boolean setItem = m.getToggle(3);
                            final boolean delete = m.getToggle(4);

                            if (description.isEmpty()) {
                                player.sendMessage(Language.get("level.settings.invalid.input"));
                                return;
                            }

                            if (!delete) {
                                if (setItem) {
                                    final ItemStack item = player.getInventory().getItemInMainHand();
                                    if (item.getType() == Material.AIR) {
                                        player.sendMessage(Language.get("level.settings.invalid.input"));
                                        return;
                                    }
                                    data = "item#" + SyncAPI.ItemAPI.itemStackToBase64(item);
                                }
                                if (data.isEmpty()) data = "null";

                                player.sendMessage(Language.get("level.settings.updated"));
                                this.serverCore.getLevelAPI().updateReward(i, description, data);
                            } else {
                                player.sendMessage(Language.get("level.settings.updated"));
                                this.serverCore.getLevelAPI().deleteReward(i);
                            }
                        });
                        window.send(player);
                    }
                });
            } catch (final Exception e) {
                player.sendMessage(Language.get("level.settings.invalid.input"));
            }
        });
        selectRewardWindow.send(player);
    }
}
