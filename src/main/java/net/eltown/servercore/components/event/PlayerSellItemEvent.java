package net.eltown.servercore.components.event;

import net.eltown.servercore.components.roleplay.shops.ShopRoleplay;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerSellItemEvent extends PlayerEvent {

    private final ShopRoleplay.Shop shop;
    private final ItemStack soldItem;
    private final double price;

    private static final HandlerList handlers = new HandlerList();

    public PlayerSellItemEvent(@NotNull Player player, final ShopRoleplay.Shop shop, final ItemStack soldItem, final double price) {
        super(player);
        this.player = player;
        this.shop = shop;
        this.soldItem = soldItem;
        this.price = price;
    }

    public ShopRoleplay.Shop getShop() {
        return shop;
    }

    public ItemStack getSoldItem() {
        return soldItem;
    }

    public double getPrice() {
        return price;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
