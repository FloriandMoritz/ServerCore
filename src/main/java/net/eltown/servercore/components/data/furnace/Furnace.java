package net.eltown.servercore.components.data.furnace;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@AllArgsConstructor
@Getter
@Setter
public class Furnace {

    private String owner;
    private final long id;
    private Location location;
    private FurnaceLevel furnaceLevel;
    private int smeltingBoost;
    private int doubleChance;
    private int xpBoost;

    @AllArgsConstructor
    @Getter
    public static class FurnaceLevel {

        private final int level;
        private double price;
        private int neededLevel;
        private int smeltingBoost;
        private int doubleChance;
        private int xpBoost;

    }

}
