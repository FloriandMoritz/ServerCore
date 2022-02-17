package net.eltown.servercore.components.event;

import net.eltown.servercore.components.data.quests.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerQuestCompleteEvent extends PlayerEvent {

    private final Quest quest;

    private static final HandlerList handlers = new HandlerList();

    public PlayerQuestCompleteEvent(@NotNull Player player, final Quest quest) {
        super(player);
        this.player = player;
        this.quest = quest;
    }

    public Quest getQuest() {
        return quest;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
