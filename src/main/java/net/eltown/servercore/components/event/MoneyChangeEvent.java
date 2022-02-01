package net.eltown.servercore.components.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class MoneyChangeEvent extends PlayerEvent {

    private final double money;

    public double getMoney() {
        return this.money;
    }

    private static final HandlerList handlers = new HandlerList();

    public MoneyChangeEvent(Player player, double money) {
        super(player, true);
        this.player = player;
        this.money = money;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}