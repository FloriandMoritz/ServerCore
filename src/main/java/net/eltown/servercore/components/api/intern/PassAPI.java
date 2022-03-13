package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.passes.PassCalls;
import net.eltown.servercore.components.data.passes.Season;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public record PassAPI(ServerCore serverCore) {

    public void getCurrentSeason(final BiConsumer<PassCalls, Season> seasonConsumer) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            switch (PassCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL -> seasonConsumer.accept(PassCalls.CALLBACK_NULL, null);
                case CALLBACK_GET_CURRENT_SEASON, CALLBACK_SEASON_NOT_ACTIVE -> {
                    final String name = d[1];
                    final String description = d[2];
                    final List<String> quests = d[3].equals("null") ? new ArrayList<>() : Arrays.asList(d[3].split("#"));
                    final HashMap<String, Season.SeasonReward> rewards = new HashMap<>();
                    if (!d[4].equals("null")) {
                        final List<String> rawRewards = List.of(d[4].split(";-;"));
                        rawRewards.forEach(rawReward -> {
                            final String[] e = rawReward.split(";");
                            rewards.put(e[0], new Season.SeasonReward(e[0], Integer.parseInt(e[1]), e[2], e[3], e[4], e[5]));
                        });
                    }
                    final long expire = Long.parseLong(d[5]);
                    final boolean isActive = Boolean.parseBoolean(d[6]);

                    seasonConsumer.accept(PassCalls.valueOf(delivery.getKey().toUpperCase()), new Season(name, description, quests, rewards, expire, isActive));
                }
            }
        }, Queue.PASS_CALLBACK, PassCalls.REQUEST_GET_CURRENT_SEASON.name(), "null");
    }

    public void createSeason(final String name, final String description, final long expire) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_CREATE_SEASON.name(), name, description, String.valueOf(expire));
    }

    public void addQuestToSeason(final String questId) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_ADD_QUEST.name(), questId);
    }

    public void removeQuestFromSeason(final String questId) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_REMOVE_QUEST.name(), questId);
    }

    public void addRewardToSeason(final int points, final String type, final String image, final String description, final String data) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_ADD_REWARD.name(), this.serverCore.createId(7), String.valueOf(points), type, image, description, data);
    }

    public void removeRewardFromSeason(final String id) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_REMOVE_REWARD.name(), id);
    }

    public void updateName(final String newName) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_UPDATE_SEASON_NAME.name(), newName);
    }

    public void updateDescription(final String newDescription) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_UPDATE_SEASON_DESCRIPTION.name(), newDescription);
    }

    public void updateExpire(final long newExpire) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_UPDATE_SEASON_EXPIRE.name(), String.valueOf(newExpire));
    }

    public void toggleActive(final boolean active) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_TOGGLE_SEASON_ACTIVE.name(), String.valueOf(active));
    }

    public void updateReward(final String id, final int points, final String type, final String image, final String description, final String data) {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_UPDATE_REWARD.name(), id, String.valueOf(points), type, image, description, data);
    }

    public void deleteSeason() {
        this.serverCore.getTinyRabbit().send(Queue.PASS_RECEIVE, PassCalls.REQUEST_DELETE_SEASON.name(), "null");
    }

}
