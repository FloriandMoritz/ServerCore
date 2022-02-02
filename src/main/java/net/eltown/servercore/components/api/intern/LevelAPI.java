package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.level.LevelReward;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;

public record LevelAPI(ServerCore serverCore) {

    public static final HashMap<String, Level> cachedData = new HashMap<>();

    public void getLevelReward(final int level, final Consumer<LevelReward> levelRewardConsumer) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_LEVEL_REWARD -> levelRewardConsumer.accept(new LevelReward(Integer.parseInt(delivery.getData()[1]), delivery.getData()[2], delivery.getData()[3]));
                case CALLBACK_NULL -> levelRewardConsumer.accept(null);
            }
        }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_LEVEL_REWARD.name(), String.valueOf(level));
    }

    public void updateReward(final int level, final String description, final String data) {
        this.serverCore.getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_REWARD.name(), String.valueOf(level), description, data);
    }

    public void deleteReward(final int level) {
        this.serverCore.getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_REMOVE_REWARD.name(), String.valueOf(level));
    }

    public void addExperience(final Player player, final double experience) {
        final Level level = cachedData.get(player.getName());
        level.setExperience(level.getExperience() + experience);

        Objects.requireNonNull(player.getScoreboard().getTeam("level")).setPrefix("   §f" + this.getLevel(player.getName()).getLevel() + " §8[" + this.getLevelDisplay(player) + "§8]  ");

        player.sendActionBar(Component.text("§a+ §2" + this.serverCore.getMoneyFormat().format(experience) + "XP"));

        this.checkForLevelUp(player);
    }

    public void checkForLevelUp(final Player player) {
        final Level level = cachedData.get(player.getName());
        final double experience = this.getMaxExperienceByLevel(level.getLevel());

        if (level.getExperience() >= experience) this.levelUp(player);
    }

    public void levelUp(final Player player) {
        final Level level = cachedData.get(player.getName());
        level.setLevel(level.getLevel() + 1);

        player.sendMessage(" ");
        player.sendMessage(Language.get("level.levelup", level.getLevel()));
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (LevelCalls.valueOf(delivery.getKey().toUpperCase()) == LevelCalls.CALLBACK_LEVEL_REWARD) {
                final LevelReward levelReward = new LevelReward(Integer.parseInt(delivery.getData()[1]), delivery.getData()[2], delivery.getData()[3]);
                final String[] rewardData = levelReward.getData().split("#");

                player.sendMessage(Language.get("level.reward", levelReward.getDescription()));

                if (rewardData[0].startsWith("gutschein")) {
                    this.serverCore.getTinyRabbit().sendAndReceive(delivery1 -> {
                        if (GiftkeyCalls.valueOf(delivery1.getKey().toUpperCase()) == GiftkeyCalls.CALLBACK_NULL) {
                            player.sendMessage(Language.get("level.reward.giftkey", delivery1.getData()[1]));
                        }
                    }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_CREATE_KEY.name(), String.valueOf(1), rewardData[1], player.getName());
                } else if (rewardData[0].startsWith("item")) {
                    final ItemStack item = SyncAPI.ItemAPI.itemStackFromBase64(rewardData[1]);
                    player.getInventory().addItem(item);
                } else if (rewardData[0].startsWith("permission")) {
                    this.serverCore.getGroupAPI().addPlayerPermission(player.getName(), rewardData[1]);
                } else if (rewardData[0].startsWith("crate")) {
                    this.serverCore.getCrateAPI().addCrate(player.getName(), rewardData[1], Integer.parseInt(rewardData[2]));
                }
            }
        }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_LEVEL_REWARD.name(), String.valueOf(level.getLevel()));
        player.sendMessage(" ");

        //player.setScoreTag("§gLevel §l" + level.getLevel());

        Objects.requireNonNull(player.getScoreboard().getTeam("level")).setPrefix("   §f" + this.getLevel(player.getName()).getLevel() + " §8[" + this.getLevelDisplay(player) + "§8]  ");
    }

    public Level getLevel(final String player) {
        if (!cachedData.containsKey(player)) {
            this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                if (LevelCalls.valueOf(delivery.getKey().toUpperCase()) == LevelCalls.CALLBACK_LEVEL) {
                    cachedData.put(player, new Level(
                            delivery.getData()[1],
                            Integer.parseInt(delivery.getData()[2]),
                            Double.parseDouble(delivery.getData()[3])
                    ));
                }
            }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_GET_LEVEL.name(), player);
        }

        return cachedData.get(player);
    }

    public double getMaxExperienceByLevel(final int level) {
        return (level * 500 * (level * 0.66 + (level * 0.25 + 1)));
    }

    public String getLevelDisplay(final Player player) {
        final int level = cachedData.get(player.getName()).getLevel();
        final double xp = cachedData.get(player.getName()).getExperience() - this.getMaxExperienceByLevel(level - 1); // 5.0 = Derzeitige XP getten
        final double required = this.getMaxExperienceByLevel(level) - this.getMaxExperienceByLevel(level - 1);

        final double percent = (xp / required) * 100;

        final long green = Math.round(percent / 5);

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= 20; i++) {
            builder.append(i <= green ? "§2|" : "§7|");
        }

        return builder.toString();
    }

}
