package net.eltown.servercore.commands.feature;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.QuestAPI;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class QuestCommand extends Command {

    private final ServerCore serverCore;

    public QuestCommand(final ServerCore serverCore) {
        super("quest");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.serverCore.getQuestAPI().checkIfQuestIsExpired(player.getName());
            final List<QuestPlayer.QuestData> questData = QuestAPI.cachedQuestPlayer.get(player.getName()).getQuestPlayerData();
            final HashMap<String, List<QuestPlayer.QuestData>> dataHashMap = new HashMap<>();

            if (!questData.isEmpty()) {
                final StringBuilder builder = new StringBuilder();

                questData.forEach(data -> {
                    if (!dataHashMap.containsKey(data.getQuestNameId())) {
                        dataHashMap.put(data.getQuestNameId(), new ArrayList<>(Collections.singletonList(data)));
                    } else {
                        dataHashMap.get(data.getQuestNameId()).add(data);
                    }
                });
                dataHashMap.forEach((g, h) -> {
                    final HashMap<String, QuestPlayer.QuestData> map = new HashMap<>();
                    h.forEach(data -> {
                        map.put(data.getQuestSubId(), data);
                    });
                    this.serverCore.getQuestAPI().getQuest(g, quest -> {
                        builder
                                .append("§8» §fQuest: §7").append(quest.getDisplayName()).append("§r\n")
                                .append("§8» §fAblauf in: §7").append(this.serverCore.getRemainingTimeFuture(h.get(0).getExpire())).append("§r\n")
                                .append("§8» §fAufgaben:").append("§r\n");
                        quest.getData().forEach(e -> {
                            final QuestPlayer.QuestData o = map.get(e.getQuestSubId());
                            builder.append("   §7» §7").append(e.getDescription()).append(" §r§8[§f").append(o.getCurrent()).append("§7/§f").append(o.getRequired()).append("§8]").append("\n");
                        });
                    });
                    builder.append("\n§8----------------------------\n\n");
                });

                final SimpleWindow window = new SimpleWindow.Builder("§7» §8Deine Quests", builder.substring(0, builder.length() - 33))
                        .build();
                window.send(player);
            } else {
                player.sendMessage("§8» §fCore §8| §7Du hast aktuell keine Quests!");
            }
        }
        return true;
    }
}
