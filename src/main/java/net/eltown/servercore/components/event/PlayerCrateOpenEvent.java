package net.eltown.servercore.components.event;

import net.eltown.servercore.components.data.crates.Raffle;
import net.eltown.servercore.components.data.crates.data.CrateReward;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PlayerCrateOpenEvent extends PlayerEvent {

    private final String crate;
    private final Set<CrateReward> crateRewards;
    private final Raffle raffle;

    private static final HandlerList handlers = new HandlerList();

    public PlayerCrateOpenEvent(@NotNull Player player, final String crate, final Set<CrateReward> crateRewards, final Raffle raffle) {
        super(player);
        this.player = player;
        this.crate = crate;
        this.crateRewards = crateRewards;
        this.raffle = raffle;
    }

    public String getCrate() {
        return crate;
    }

    public Set<CrateReward> getCrateRewards() {
        return crateRewards;
    }

    public Raffle getRaffle() {
        return raffle;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
