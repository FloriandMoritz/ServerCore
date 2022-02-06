package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.rewards.DailyReward;
import net.eltown.servercore.components.data.rewards.RewardCalls;
import net.eltown.servercore.components.data.rewards.RewardPlayer;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public record RewardAPI(ServerCore serverCore) {

    public void createDailyReward(final String description, final int day, final int chance, final String data) {
        this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_ADD_REWARD.name(), description, String.valueOf(day), String.valueOf(chance), data);
    }

    public void removeDailyReward(final String id) {
        this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_REMOVE_REWARD.name(), id);
    }

    public void updateDailyReward(final DailyReward reward) {
        this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_UPDATE_DAILY_REWARD.name(), reward.description(), reward.id(), String.valueOf(reward.day()), String.valueOf(reward.chance()), reward.data());
    }

    public void addStreak(final String player) {
        this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_ADD_STREAK.name(), player);
    }

    public void resetStreak(final String player) {
        this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_RESET_STREAK.name(), player);
    }

    public void getPlayerData(final String player, final Consumer<RewardPlayer> consumer) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            consumer.accept(new RewardPlayer(delivery.getData()[1], Integer.parseInt(delivery.getData()[2]), Long.parseLong(delivery.getData()[3])));
        }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_PLAYER_DATA.name(), player);
    }

    public void getRewards(final int day, final Consumer<Set<DailyReward>> consumer) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (delivery.getData()[1].equals("null")) {
                consumer.accept(null);
            } else {
                final Set<DailyReward> dailyRewards = new HashSet<>();
                Arrays.asList(delivery.getData()[1].split("-:-")).forEach(p -> {
                    final String[] l = p.split(">:<");
                    dailyRewards.add(new DailyReward(l[0], l[1], Integer.parseInt(l[2]), Integer.parseInt(l[3]), l[4]));
                });
                consumer.accept(dailyRewards);
            }
        }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_REWARDS.name(), String.valueOf(day));
    }

}
