package net.eltown.servercore.components.tasks;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.crates.Raffle;
import net.eltown.servercore.components.data.crates.data.CrateReward;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.feature.JohnRoleplay;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.concurrent.ThreadLocalRandom;

public class RaffleTask implements Runnable {

    private final ServerCore serverCore;
    private final Raffle raffle;
    private final Player player;
    private final int spins;
    private final ArmorStand hologram;
    private int done = 0;

    public RaffleTask(final ServerCore serverCore, final Raffle raffle, final Player player) {
        this.serverCore = serverCore;
        this.raffle = raffle;
        this.player = player;
        this.spins = ThreadLocalRandom.current().nextInt(100) + 50;
        this.hologram = JohnRoleplay.hologram;
    }

    @Override
    public void run() {
        final int add = Math.max(this.done - (this.spins - 20), 1);
        final String display = this.raffle.getNextRaffleDisplay();

        if ((this.spins - 1) <= this.done) {
            final CrateReward reward = this.raffle.getFinalReward();

            this.hologram.setCustomName(reward.getDisplayName());
            this.player.getWorld().playSound(this.player.getLocation(), "random.click", 1, 1);

            this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
                this.hologram.setCustomName("§2§l> §r" + reward.getDisplayName() + " §r§2§l<");
                this.player.getWorld().playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 3);

                this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
                    final Location fireworkLocation = this.hologram.getLocation().clone();
                    final Firework firework = (Firework) this.hologram.getLocation().getWorld().spawnEntity(fireworkLocation.add(0, 1.5, 0), EntityType.FIREWORK);
                    final FireworkMeta fireworkMeta = firework.getFireworkMeta();

                    fireworkMeta.setPower(2);
                    fireworkMeta.addEffect(FireworkEffect.builder().withColor(Color.PURPLE, Color.BLUE).flicker(true).trail(true).build());

                    firework.setFireworkMeta(fireworkMeta);
                    firework.detonate();
                }, 20);

                if (reward.getChance() <= 20) {
                    this.serverCore.getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_BROADCAST_PROXY_MESSAGE.name(), Language.get("crate.reward.broadcast", this.player.getName(), reward.getDisplayName(), this.serverCore.getCrateAPI().convertToDisplay(reward.getCrate())));
                    this.player.getWorld().playSound(this.player.getLocation(), "random.levelup", 1, 3);
                }

                final String[] rewardData = reward.getData().split(";");
                switch (rewardData[0]) {
                    case "item" -> {
                        final ItemStack item = SyncAPI.ItemAPI.itemStackFromBase64(rewardData[1]);
                        this.player.getInventory().addItem(item);
                        this.player.sendMessage(Language.get("crate.reward.item", item.getI18NDisplayName(), item.getAmount()));
                    }
                    case "money" -> {
                        this.serverCore.getEconomyAPI().addMoney(this.player.getName(), Double.parseDouble(rewardData[1]));
                        this.player.sendMessage(Language.get("crate.reward.money", Double.parseDouble(rewardData[1])));
                    }
                    case "xp" -> {
                        this.serverCore.getLevelAPI().addExperience(this.player, Double.parseDouble(rewardData[1]));
                        this.player.sendMessage(Language.get("crate.reward.xp", Double.parseDouble(rewardData[1])));
                    }
                    case "crate" -> {
                        this.serverCore.getCrateAPI().addCrate(this.player.getName(), rewardData[1], Integer.parseInt(rewardData[2]));
                        this.player.sendMessage(Language.get("crate.reward.crate", this.serverCore.getCrateAPI().convertToDisplay(rewardData[1]), rewardData[2]));
                    }
                }

                this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
                    this.hologram.setCustomName("§5§lGlückstruhe");
                    JohnRoleplay.crateInUse = false;
                }, 80);
            }, (add * 25L) / 20);
            return;
        }

        this.hologram.setCustomName(display);
        this.player.getWorld().playSound(this.player.getLocation(), "random.click", 1, 1);

        this.done++;
        this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, this, (add * 25L) / 20);
    }
}
