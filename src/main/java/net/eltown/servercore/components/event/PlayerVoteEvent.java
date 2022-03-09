package net.eltown.servercore.components.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerVoteEvent extends PlayerEvent {

    private final String giftKey;

    private static final HandlerList handlers = new HandlerList();

    public PlayerVoteEvent(@NotNull Player player, final String giftKey) {
        super(player);
        this.player = player;
        this.giftKey = giftKey;
    }

    public String getGiftKey() {
        return giftKey;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
