package net.eltown.servercore.components.event;

import net.eltown.servercore.components.data.level.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerExperienceAddEvent extends PlayerEvent {

    private final double experience;
    private final Level level;

    private static final HandlerList handlers = new HandlerList();

    public PlayerExperienceAddEvent(@NotNull Player player, final double experience, final Level level) {
        super(player);
        this.player = player;
        this.experience = experience;
        this.level = level;
    }

    public double getExperience() {
        return experience;
    }

    public Level getLevel() {
        return level;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
