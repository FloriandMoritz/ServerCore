package net.eltown.servercore.components.event;

import net.eltown.servercore.components.data.rewards.DailyReward;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PlayerClaimDailyRewardEvent extends PlayerEvent {

    private final Set<DailyReward> dailyRewards;
    private final DailyReward dailyReward;

    private static final HandlerList handlers = new HandlerList();

    public PlayerClaimDailyRewardEvent(@NotNull Player player, final Set<DailyReward> dailyRewards, final DailyReward dailyReward) {
        super(player);
        this.player = player;
        this.dailyRewards = dailyRewards;
        this.dailyReward = dailyReward;
    }

    public Set<DailyReward> getDailyRewards() {
        return dailyRewards;
    }

    public DailyReward getDailyReward() {
        return dailyReward;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
