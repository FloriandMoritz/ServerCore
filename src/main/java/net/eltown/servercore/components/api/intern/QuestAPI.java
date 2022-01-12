package net.eltown.servercore.components.api.intern;

import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.data.quests.FullQuestPlayer;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public record QuestAPI(ServerCore serverCore) {

    public static final HashMap<String, QuestPlayer> cachedQuestPlayer = new HashMap<>();
    private static final HashMap<String, Quest> cachedQuests = new HashMap<>();

    public void getQuest(final String questNameId, final Consumer<Quest> quest) {
        if (!cachedQuests.containsKey(questNameId)) {
            this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                final String[] d = delivery.getData();
                switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_QUEST_DATA -> {
                        cachedQuests.put(d[1], new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));
                        quest.accept(new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));
                    }
                    case CALLBACK_NULL -> quest.accept(null);
                }
            }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_QUEST_DATA.name(), questNameId);
        } else quest.accept(cachedQuests.get(questNameId));
    }

    public void getRandomQuestByLink(final String link, final String player, final Consumer<Quest> quest) {
        this.checkIfQuestIsExpired(player);
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_RANDOM_QUEST_DATA_BY_LINK -> {
                    if (!cachedQuests.containsKey(d[1])) cachedQuests.put(d[1], new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));
                    final AtomicReference<Quest> questReference = new AtomicReference<>(null);
                    cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                        this.getQuest(e.getQuestNameId(), f -> {
                            if (f.getLink().equals(link)) {
                                questReference.set(f);
                            }
                        });
                    });
                    if (questReference.get() == null) {
                        questReference.set(new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));
                    }
                    quest.accept(questReference.get());
                }
                case CALLBACK_NULL -> quest.accept(null);
            }
        }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_RANDOM_QUEST_DATA_BY_LINK.name(), link);
    }

    public void updateQuest(final String questNameId, final String displayName, final String description, final String data, final int required, final long expire, final String rewardData, final String link) {
        cachedQuests.remove(questNameId);
        cachedQuests.put(questNameId, new Quest(questNameId, displayName, description, data, required, expire, rewardData, link));

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_QUEST.name(), questNameId, displayName, description, data, String.valueOf(required), String.valueOf(expire), rewardData, link);
    }

    public void createQuest(final String questNameId, final String displayName, final String description, final String data, final int required, final long expire, final String rewardData, final String link) {
        cachedQuests.remove(questNameId);
        cachedQuests.put(questNameId, new Quest(questNameId, displayName, description, data, required, expire, rewardData, link));

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_CREATE_QUEST.name(), questNameId, displayName, description, data, String.valueOf(required), String.valueOf(expire), rewardData, link);
    }

    public boolean playerIsInQuest(final String player, final String questNameId) {
        this.checkIfQuestIsExpired(player);
        final QuestPlayer.QuestPlayerData questPlayerData = this.getQuestPlayerDataFromQuestId(player, questNameId);
        if (questPlayerData == null) return false;
        return questPlayerData.getQuestNameId().equals(questNameId);
    }

    public Set<FullQuestPlayer> getActivePlayerQuests(final String player) {
        final Set<FullQuestPlayer> quests = new HashSet<>();

        this.checkIfQuestIsExpired(player);
        for (QuestPlayer.QuestPlayerData questPlayerData : cachedQuestPlayer.get(player).getQuestPlayerData()) {
            this.getQuest(questPlayerData.getQuestNameId(), quest -> {
                quests.add(new FullQuestPlayer(quest, questPlayerData));
            });
        }

        return quests;
    }

    public void setQuestOnPlayer(final String player, final String questNameId) {
        cachedQuestPlayer.get(player).getQuestPlayerData().forEach(questPlayerData -> {
            this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_PLAYER_DATA.name(), player, questPlayerData.getQuestNameId(), String.valueOf(questPlayerData.getCurrent()));
        });

        this.getQuest(questNameId, quest -> {
            final List<QuestPlayer.QuestPlayerData> playerData = cachedQuestPlayer.get(player).getQuestPlayerData();
            playerData.add(new QuestPlayer.QuestPlayerData(quest.getNameId(), (System.currentTimeMillis() + quest.getExpire()), quest.getRequired(), 0));
            cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

            this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_SET_PLAYER_QUEST.name(), player, questNameId);
        });
    }

    public void removeQuestFromPlayer(final String player, final String questNameId) {
        final QuestPlayer.QuestPlayerData questPlayerData = this.getQuestPlayerDataFromQuestId(player, questNameId);
        final List<QuestPlayer.QuestPlayerData> playerData = cachedQuestPlayer.get(player).getQuestPlayerData();
        playerData.remove(questPlayerData);
        cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_REMOVE_PLAYER_QUEST.name(), player, questNameId);
    }

    public void addQuestProgress(final Player player, final String questNameId, final int progress) {
        cachedQuestPlayer.get(player.getName()).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) {
                if (!(e.getCurrent() >= e.getRequired())) {
                    e.setCurrent(e.getCurrent() + progress);
                    this.checkForQuestEnding(player, questNameId, e);
                }
            }
        });
    }

    public void checkForQuestEnding(final Player player, final String questNameId, final QuestPlayer.QuestPlayerData questPlayerData) {
        this.getQuest(questNameId, quest -> {
            if (quest.getRequired() <= questPlayerData.getCurrent()) {
                player.sendMessage(" ");
                player.sendMessage(Language.get("quest.completed", quest.getDisplayName()));

                final List<String> rewards = new ArrayList<>(Arrays.asList(quest.getRewardData().split("-#-")));
                rewards.forEach(e -> {
                    final String[] reward = e.split("#");

                    switch (reward[0]) {
                        case "xp" -> {
                            this.serverCore.getLevelAPI().addExperience(player, Double.parseDouble(reward[1]));
                            player.sendMessage(Language.get("quest.reward.xp", reward[1]));
                        }
                        case "money" -> {
                            Economy.getAPI().addMoney(player, Double.parseDouble(reward[1]));
                            player.sendMessage(Language.get("quest.reward.money", reward[1]));
                        }
                        case "item" -> {
                            final ItemStack item = SyncAPI.ItemAPI.itemStackFromBase64(reward[1]);
                            player.getInventory().addItem(item);
                            player.sendMessage(Language.get("quest.reward.item", item.getI18NDisplayName(), item.getAmount()));
                        }
                        case "gutschein" -> this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                            if (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase()) == GiftkeyCalls.CALLBACK_NULL) {
                                player.sendMessage(Language.get("quest.reward.giftkey", delivery.getData()[1]));
                            }
                        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_CREATE_KEY.name(), String.valueOf(1), reward[1], player.getName());
                        case "permission" -> {
                            this.serverCore.getGroupAPI().addPlayerPermission(player.getName(), reward[1]);
                            player.sendMessage(Language.get("quest.reward.permission", reward[2]));
                        }
                        case "crate" -> this.serverCore.getCrateAPI().addCrate(player.getName(), reward[1], Integer.parseInt(reward[2]));

                        //player.sendMessage(Language.get("quest.reward.crate", this.serverCore.getFeatureRoleplay().convertToDisplay(reward[1]), Integer.parseInt(reward[2])));
                    }
                });
                player.sendMessage(" ");

                //this.serverCore.getServer().getPluginManager().callEvent(new QuestCompleteEvent(player, quest, questPlayerData));
                this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_PLAYER_DATA.name(), player.getName(), questPlayerData.getQuestNameId(), String.valueOf(questPlayerData.getCurrent()));
            }
        });
    }

    public QuestPlayer.QuestPlayerData getQuestPlayerDataFromQuestId(final String player, final String questNameId) {
        final AtomicReference<QuestPlayer.QuestPlayerData> questPlayerData = new AtomicReference<>();

        if (cachedQuestPlayer.get(player) == null || cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) return null;

        cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) questPlayerData.set(e);
        });

        return questPlayerData.get();
    }

    public void checkIfQuestIsExpired(final String player) {
        final List<String> list = new ArrayList<>();
        if (!cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) {
            cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                if (e.getExpire() < System.currentTimeMillis()) list.add(e.getQuestNameId());
            });
        }

        if (!list.isEmpty()) {
            list.forEach(e -> this.removeQuestFromPlayer(player, e));
        }
    }

}
