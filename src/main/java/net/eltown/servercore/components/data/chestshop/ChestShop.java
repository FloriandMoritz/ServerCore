package net.eltown.servercore.components.data.chestshop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Getter
@Setter
public class ChestShop {

    private final Location signLocation;
    private final Location chestLocation;
    private final long id;
    private final String owner;
    private final ShopType shopType;
    private double shopPrice;
    private int shopCount;
    private ItemStack item;
    private String bankAccount;

    public enum ShopType {

        SELL,
        BUY,
        ADMIN

    }

}
