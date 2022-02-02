package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public record QuestAPI(ServerCore serverCore) {

    public static final HashMap<String, QuestPlayer> cachedQuestPlayer = new HashMap<>();
    public static final HashMap<String, Quest> cachedQuests = new HashMap<>();

    public void getQuest(final String questNameId, final Consumer<Quest> quest) {
        if (!cachedQuests.containsKey(questNameId)) {
            this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                final String[] d = delivery.getData();
                switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_QUEST_DATA -> {
                        final List<String> data = Arrays.asList(d[3].split("-#-"));
                        final List<Quest.QuestData> questData = new ArrayList<>();
                        data.forEach(g -> {
                            final String[] e = g.split("-:-");
                            questData.add(new Quest.QuestData(d[1], e[1], e[2], e[3], Integer.parseInt(e[4])));
                        });
                        cachedQuests.put(d[1], new Quest(d[1], d[2], questData, Long.parseLong(d[4]), d[5], d[6]));
                        quest.accept(new Quest(d[1], d[2], questData, Long.parseLong(d[4]), d[5], d[6]));
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
                    final List<String> data = Arrays.asList(d[3].split("-#-"));
                    final List<Quest.QuestData> questData = new ArrayList<>();
                    data.forEach(g -> {
                        final String[] e = g.split("-:-");
                        questData.add(new Quest.QuestData(d[1], e[1], e[2], e[3], Integer.parseInt(e[4])));
                    });
                    if (!cachedQuests.containsKey(d[1])) cachedQuests.put(d[1], new Quest(d[1], d[2], questData, Long.parseLong(d[4]), d[5], d[6]));
                    final AtomicReference<Quest> questReference = new AtomicReference<>(null);
                    cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                        this.getQuest(e.getQuestNameId(), f -> {
                            if (f.getLink().equals(link)) {
                                questReference.set(f);
                            }
                        });
                    });
                    if (questReference.get() == null) {
                        questReference.set(new Quest(d[1], d[2], questData, Long.parseLong(d[4]), d[5], d[6]));
                    }
                    quest.accept(questReference.get());
                }
                case CALLBACK_NULL -> quest.accept(null);
            }
        }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_RANDOM_QUEST_DATA_BY_LINK.name(), link);
    }

    public void updateSubQuest(final String questNameId, final String questSubId, final String description, final String data, final int required) {
        final List<Quest.QuestData> list = new ArrayList<>(cachedQuests.get(questNameId).getData());
        list.removeIf(s -> s.getQuestNameId().equals(questNameId) && s.getQuestSubId().equals(questSubId));
        list.add(new Quest.QuestData(questNameId, questSubId, description, data, required));
        cachedQuests.get(questNameId).setData(list);

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_SUB_QUEST.name(), questNameId, questSubId, description, data, String.valueOf(required));
    }

    public void updateQuest(final Quest quest, final String displayName, final long expire, final String rewardData, final String link) {
        cachedQuests.remove(quest.getNameId());
        cachedQuests.put(quest.getNameId(), new Quest(quest.getNameId(), displayName, quest.getData(), expire, rewardData, link));

        final StringBuilder dataBuilder = new StringBuilder();
        quest.getData().forEach(e -> {
            dataBuilder.append(e.getQuestNameId()).append("-:-").append(e.getQuestSubId()).append("-:-").append(e.getDescription()).append("-:-").append(e.getData()).append("-:-").append(e.getRequired()).append("-#-");
        });
        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_QUEST.name(), quest.getNameId(), displayName, dataBuilder.substring(0, dataBuilder.length() - 3), String.valueOf(expire), rewardData, link);
    }

    public void createSubQuest(final String questNameId, final String questSubId, final String description, final String data, final int required) {
        cachedQuests.get(questNameId).getData().add(new Quest.QuestData(questNameId, questSubId, description, data, required));

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_CREATE_SUB_QUEST.name(), questNameId, questSubId, description, data, String.valueOf(required));
    }

    public void createQuest(final String questNameId, final String displayName, final List<Quest.QuestData> data, final long expire, final String rewardData, final String link) {
        cachedQuests.remove(questNameId);
        cachedQuests.put(questNameId, new Quest(questNameId, displayName, data, expire, rewardData, link));

        final StringBuilder dataBuilder = new StringBuilder();
        data.forEach(e -> {
            dataBuilder.append(e.getQuestNameId()).append("-:-").append(e.getQuestSubId()).append("-:-").append(e.getDescription()).append("-:-").append(e.getData()).append("-:-").append(e.getRequired()).append("-#-");
        });
        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_CREATE_QUEST.name(), questNameId, displayName, dataBuilder.substring(0, dataBuilder.length() - 3), String.valueOf(expire), rewardData, link);
    }

    public boolean playerIsInQuest(final String player, final String questNameId) {
        this.checkIfQuestIsExpired(player);
        final List<QuestPlayer.QuestData> questPlayerData = cachedQuestPlayer.get(player).getQuestPlayerData();
        final AtomicBoolean b = new AtomicBoolean(false);
        questPlayerData.forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) b.set(true);
        });
        return b.get();
    }

    public Set<QuestPlayer.FullQuestPlayer> getActivePlayerQuests(final String player) {
        final Set<QuestPlayer.FullQuestPlayer> quests = new HashSet<>();

        this.checkIfQuestIsExpired(player);
        for (QuestPlayer.QuestData questPlayerData : cachedQuestPlayer.get(player).getQuestPlayerData()) {
            this.getQuest(questPlayerData.getQuestNameId(), quest -> {
                quests.add(new QuestPlayer.FullQuestPlayer(quest, questPlayerData));
            });
        }

        return quests;
    }

    public void setQuestOnPlayer(final String player, final String questNameId) {
        this.updateQuestPlayerData(player);

        this.getQuest(questNameId, quest -> {
            final List<QuestPlayer.QuestData> playerData = new ArrayList<>(cachedQuestPlayer.get(player).getQuestPlayerData());
            quest.getData().forEach(e -> {
                playerData.add(new QuestPlayer.QuestData(e.getQuestNameId(), e.getQuestSubId(), e.getData(), 0, e.getRequired(), System.currentTimeMillis() + quest.getExpire()));
            });
            cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

            this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_SET_PLAYER_QUEST.name(), player, questNameId);
        });
    }

    public void removeQuestFromPlayer(final String player, final String questNameId) {
        final List<QuestPlayer.QuestData> playerData = new ArrayList<>(cachedQuestPlayer.get(player).getQuestPlayerData());
        playerData.removeIf(s -> s.getQuestNameId().equals(questNameId));
        cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_REMOVE_PLAYER_QUEST.name(), player, questNameId);
    }

    public void addQuestProgress(final Player player, final String questNameId, final String questSubId, final int progress) {
        cachedQuestPlayer.get(player.getName()).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId) && e.getQuestSubId().equals(questSubId)) {
                if (!(e.getCurrent() >= e.getRequired())) {
                    e.setCurrent(e.getCurrent() + progress);
                    this.checkForQuestEnding(player, questNameId);
                }
            }
        });
    }

    public Set<QuestPlayer.QuestData> getQuestPlayerDataByNameId(final String player, final String questNameId) {
        final Set<QuestPlayer.QuestData> questData = new HashSet<>();

        cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) questData.add(e);
        });

        return questData;
    }

    public void checkForQuestEnding(final Player player, final String questNameId) {
        this.getQuest(questNameId, quest -> {
            final Set<QuestPlayer.QuestData> questData = this.getQuestPlayerDataByNameId(player.getName(), questNameId);
            final AtomicInteger i = new AtomicInteger(0);

            questData.forEach(e -> {
                if (e.getCurrent() >= e.getRequired()) i.addAndGet(1);
            });

            if (i.get() >= questData.size()) {
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
                            this.serverCore.getEconomyAPI().addMoney(player.getName(), Double.parseDouble(reward[1]));
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
                this.updateQuestPlayerData(player.getName());
            }
        });
    }

    public void checkIfQuestIsExpired(final String player) {
        final List<String> list = new ArrayList<>();
        if (!cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) {
            cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                if (e.getExpire() < System.currentTimeMillis()) {
                    if (!list.contains(e.getQuestNameId())) list.add(e.getQuestNameId());
                }
            });
        }

        if (!list.isEmpty()) {
            list.forEach(e -> this.removeQuestFromPlayer(player, e));
        }
    }

    public void updateQuestPlayerData(final String player) {
        final QuestPlayer questPlayer = cachedQuestPlayer.get(player);

        final StringBuilder builder = new StringBuilder();
        if (!questPlayer.getQuestPlayerData().isEmpty()) {
            questPlayer.getQuestPlayerData().forEach(e -> {
                builder.append(e.getQuestNameId()).append("-:-").append(e.getQuestSubId()).append("-:-").append(e.getData()).append("-:-").append(e.getCurrent()).append("-:-").append(e.getRequired()).append("-:-").append(e.getExpire()).append("-#-");
            });

            this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_PLAYER_DATA.name(), player, builder.substring(0, builder.length() - 3));
        }
    }

    public void removeQuest(final String questNameId) {
        cachedQuests.remove(questNameId);

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_REMOVE_QUEST.name(), questNameId);
    }

    public void removeSubQuest(final String questNameId, final String questSubId) {
        final List<Quest.QuestData> questData = new ArrayList<>(cachedQuests.get(questNameId).getData());
        questData.removeIf(s -> s.getQuestNameId().equals(questNameId) && s.getQuestSubId().equals(questSubId));
        cachedQuests.get(questNameId).setData(questData);

        this.serverCore.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_REMOVE_SUB_QUEST.name(), questNameId, questSubId);
    }

    public void invalidateQuestCache() {
        cachedQuests.clear();
    }

    public void invalidatePlayerCache() {
        cachedQuestPlayer.clear();
    }

}
