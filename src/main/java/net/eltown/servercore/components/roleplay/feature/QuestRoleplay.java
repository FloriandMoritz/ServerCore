package net.eltown.servercore.components.roleplay.feature;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.RoleplayListener;
import net.eltown.servercore.utils.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public record QuestRoleplay(ServerCore serverCore) {

    static final Cooldown playerTalks = new Cooldown(TimeUnit.MINUTES.toMillis(15));
    static final Cooldown talkCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(20));

    private static final List<ChainMessage> ruedigerTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Ha...Hahahallo, §a%p§7! Wie geht's dir mein Guter?", 3),
            new ChainMessage("Danke, dass du mir alten Mann behilflich bist!", 2),
            new ChainMessage("Ich bezahle gut! Jaja, sehr gut! Das glaub mir mal!", 2),
            new ChainMessage("Schön, dich zu sehen!", 2)
    ));

    public void openRuedigerByNpc(final Player player) {
        this.smallTalk(ruedigerTalks, RoleplayID.FEATURE_RUEDIGER.name(), player, message -> {
            if (message == null) {
                this.openRuediger(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fRüdiger §8| §7" + message.message().replace("%p", player.getName()));
                            Sound.MOB_VILLAGER_HAGGLE.playSound(player);
                        })
                        .append(message.seconds(), () -> {
                            this.openRuediger(player);
                            Sound.MOB_VILLAGER_HAGGLE.playSound(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    private void openRuediger(final Player player) {
        this.serverCore.getQuestAPI().getRandomQuestByLink("Rüdiger", player.getName(), quest -> {
            this.openQuestNPC(player, quest, "§8» §fRüdiger", " §8| §7Erledige meine Aufgaben, um eine oder mehrere tolle Belohnungen zu erhalten! Ich bezahle anständig und habe immer was zu tun!");
        });
    }

    public void openQuestNPC(final Player player, final Quest quest, final String npcPrefix, final String npcText) {
        boolean b = true;
        if (!this.serverCore.getQuestAPI().playerIsInQuest(player.getName(), quest.getNameId())) {
            this.serverCore.getQuestAPI().setQuestOnPlayer(player.getName(), quest.getNameId());
            b = false;
        }
        final Set<QuestPlayer.QuestData> questData = this.serverCore.getQuestAPI().getQuestPlayerDataByNameId(player.getName(), quest.getNameId());

        final StringBuilder builder = new StringBuilder();
        final HashMap<String, QuestPlayer.QuestData> map = new HashMap<>();
        questData.forEach(data -> {
            map.put(data.getQuestSubId(), data);
        });

        builder.append("§8» §fQuest: §7").append(quest.getDisplayName()).append("§r\n")
                .append("§8» §fAblauf in: §7").append(this.serverCore.getRemainingTimeFuture(map.values().stream().findAny().get().getExpire())).append("§r\n")
                .append("§8» §fAufgaben:").append("§r\n");

        quest.getData().forEach(e -> {
            final QuestPlayer.QuestData o = map.get(e.getQuestSubId());
            builder.append("   §7» §7").append(e.getDescription()).append(" §r§8[§f").append(o.getCurrent()).append("§7/§f").append(o.getRequired()).append("§8]").append("\n");
        });
        builder.append("\n");

        final Map<String, Quest.QuestData> bringQuests = new HashMap<>();
        map.forEach((subId, data) -> {
            if (data.getData().startsWith("bring")) {
                if (data.getCurrent() < data.getRequired()) bringQuests.put(subId, quest.getData().stream().filter(q -> q.getQuestSubId().equals(subId)).findFirst().get());
            }
        });

        final SimpleWindow.Builder window = new SimpleWindow.Builder(npcPrefix, npcPrefix + npcText + "\n\n" + builder);
        if (bringQuests.size() != 0 && b) {
            final SimpleWindow.Builder selectBringWindow = new SimpleWindow.Builder(npcPrefix, "§8» §7Wähle aus, welche Items du für deine laufende Quest abgeben möchtest.\n\n");
            window.addButton(npcPrefix + " Items bringen", "", e -> {
                bringQuests.forEach((subId, data) -> {
                    final ItemStack itemStack = SyncAPI.ItemAPI.itemStackFromBase64(data.getData().split("#")[1]);
                    final int count = this.countInventoryItems(player, itemStack);
                    if (count > 0) {
                        final int needed = map.get(subId).getRequired() - map.get(subId).getCurrent();
                        final int giveCount = Math.min(count, needed);
                        selectBringWindow.addButton("§8» §9" + data.getRequired() + "x §f" + itemStack.getI18NDisplayName() + "\n§r§8[§f" + map.get(subId).getCurrent() + "§7/§f" + map.get(subId).getRequired() + "§8]", f -> {
                            final ModalWindow bringItemsWindow = new ModalWindow.Builder(npcPrefix, npcPrefix + " §8| §7Oh! Damit du deine Aufgabe erledigst, musst du mir noch §9" + needed + "x " + itemStack.getI18NDisplayName()
                                    + " §7geben. Möchtest du mir §9" + giveCount + " Stück §7schon geben, um in der Aufgabe weiterzumachen?",
                                    "§8» §aItems abgeben", "§8» §cAbbrechen")
                                    .onYes(v -> {
                                        itemStack.setAmount(giveCount);
                                        player.getInventory().removeItem(itemStack);
                                        player.sendMessage(Language.get("quest.progress.gave.item", itemStack.getI18NDisplayName(), giveCount));
                                        Sound.NOTE_PLING.playSound(player);
                                        this.serverCore.getQuestAPI().addQuestProgress(player, quest.getNameId(), subId, giveCount);
                                    })
                                    .onNo(v -> this.openQuestNPC(player, quest, npcPrefix, npcText))
                                    .build();
                            bringItemsWindow.send(player);
                        });
                    }
                });
                selectBringWindow.build().send(player);
            });
        }
        window.build().send(player);
    }

    private int countInventoryItems(final Player player, final ItemStack provided) {
        final AtomicInteger i = new AtomicInteger();
        for (final ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null) continue;
            if (itemStack.getType() == provided.getType()) i.addAndGet(itemStack.getAmount());
        }
        return i.get();
    }

    private void smallTalk(final List<ChainMessage> messages, final String npc, final Player player, final Consumer<ChainMessage> message) {
        if (talkCooldown.hasCooldown(npc + "//" + player.getName())) {
            message.accept(null);
            return;
        }
        if (!playerTalks.hasCooldown(npc + "//" + player.getName())) {
            message.accept(messages.get(0));
        } else {
            int index = ThreadLocalRandom.current().nextInt(1, messages.size());
            message.accept(messages.get(index));
        }
        RoleplayListener.openQueue.add(player.getName());
    }

}
