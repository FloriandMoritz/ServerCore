package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.rewards.DailyReward;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RewardSystemCommand extends Command {

    private final ServerCore serverCore;

    public RewardSystemCommand(final ServerCore serverCore) {
        super("rewardsystem");
        this.serverCore = serverCore;
        this.setPermission("core.command.rewardsystem");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8RewardSystem Einstellungen", "\n\n")
                    .addButton("§8» §fDailyRewards", this::openSelectDailyReward)
                    .build();
            window.send(player);
        }
        return true;
    }

    private void openSelectDailyReward(final Player player) {
        final CustomWindow window = new CustomWindow("§7» §8DailyReward Einstellungen");
        window.form()
                .slider("§8» §fBitte wähle den Tag aus, den du für die täglichen Belohnungen bearbeiten möchtest.", 1, 14, 1, 1);

        window.onSubmit((g, h) -> {
            final int day = (int) h.getSlider(0);

            this.serverCore.getRewardAPI().getRewards(day, dailyRewards -> {
                this.openDailyRewardReward(player, day, Objects.requireNonNullElseGet(dailyRewards, HashSet::new));
            });
        });
        window.send(player);
    }

    private void openDailyRewardReward(final Player player, final int day, final Set<DailyReward> dailyRewards) {
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8DailyReward Einstellungen", "§8» §fBitte wähle einen Reward für den folgenden Tag aus: §9" + day);

        dailyRewards.forEach(reward -> window.addButton(reward.description(), g -> {
            this.openEditReward(player, reward);
        }));
        window.addButton("§8» §fNeuen Reward eintragen", g -> {
            this.openCreateReward(player, day);
        });

        window.build().send(player);
    }

    private void openEditReward(final Player player, final DailyReward reward) {
        final CustomWindow window = new CustomWindow("§7» §8DailyReward bearbeiten");
        window.form()
                .input("§8» §fBeschreibung bearbeiten", reward.description(), reward.description())
                .input("§8» §fWahrscheinlichkeit bearbeiten", "" + reward.chance(), "" + reward.chance())
                .input("§8» §fBelohnungs-Daten bearbeiten\n§7xp;<amount>\n§7money;<amount>\n§7crate;<type>;<amount>", reward.data(), reward.data())
                .toggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen", false)
                .toggle("§8» §cDiesen Reward löschen", false);

        window.onSubmit((g, h) -> {
            try {
                final String description = h.getInput(0);
                final int chance = Integer.parseInt(h.getInput(1));
                String data = h.getInput(2);
                final boolean setItem = h.getToggle(3);
                final boolean delete = h.getToggle(4);

                if (description.isEmpty() || data.isEmpty()) {
                    player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
                    return;
                }

                if (!delete) {
                    if (setItem) {
                        final ItemStack item = player.getInventory().getItemInMainHand();
                        if (item.getType() == Material.AIR) {
                            player.sendMessage("Deine Angaben sind fehlerhaft: Bitte halte ein Item in deiner Hand.");
                            return;
                        }
                        data = "item;" + SyncAPI.ItemAPI.itemStackToBase64(item);
                    }
                    player.sendMessage("Der Eintrag wurde aktualisiert.");
                    this.serverCore.getRewardAPI().updateDailyReward(new DailyReward(description, reward.id(), reward.day(), chance, data));
                } else {
                    player.sendMessage("Der Eintrag wurde gelöscht.");
                    this.serverCore.getRewardAPI().removeDailyReward(reward.id());
                }
            } catch (final Exception e) {
                player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
            }
        });
        window.send(player);
    }

    private void openCreateReward(final Player player, final int day) {
        final CustomWindow window = new CustomWindow("§7» §8DailyReward erstellen");
        window.form()
                .input("§8» §fBeschreibung")
                .input("§8» §fWahrscheinlichkeit")
                .input("§8» §fBelohnungs-Daten\n§7xp;<amount>\n§7money;<amount>\n§7crate;<type>;<amount>")
                .toggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen.", false);

        window.onSubmit((g, h) -> {
            try {
                final String description = h.getInput(0);
                final int chance = Integer.parseInt(h.getInput(1));
                String data = h.getInput(2);
                final boolean setItem = h.getToggle(3);

                if (description.isEmpty() || data.isEmpty()) {
                    player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
                    return;
                }

                if (setItem) {
                    final ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR) {
                        player.sendMessage("Deine Angaben sind fehlerhaft: Bitte halte ein Item in deiner Hand.");
                        return;
                    }
                    data = "item;" + SyncAPI.ItemAPI.itemStackToBase64(item);
                }
                player.sendMessage("Der Eintrag wurde erstellt.");
                this.serverCore.getRewardAPI().createDailyReward(description, day, chance, data);
            } catch (final Exception e) {
                player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
            }
        });
        window.send(player);
    }
}
