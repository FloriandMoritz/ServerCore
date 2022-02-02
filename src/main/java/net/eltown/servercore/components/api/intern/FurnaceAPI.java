package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.config.Config;
import net.eltown.servercore.components.data.furnace.Furnace;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class FurnaceAPI {

    private final ServerCore serverCore;
    private final Config configuration;

    public HashMap<Integer, Furnace.FurnaceLevel> cachedFurnaceLevel = new HashMap<>();
    public HashMap<Location, Furnace> cachedFurnaces = new HashMap<>();

    public FurnaceAPI(final ServerCore instance) {
        this.serverCore = instance;
        this.configuration = new Config(instance.getDataFolder() + "/components/furnace.yml", Config.YAML);

        this.cachedFurnaceLevel.put(0, new Furnace.FurnaceLevel(0, 0, 0, 0, 0, 0));
        this.cachedFurnaceLevel.put(1, new Furnace.FurnaceLevel(1, 1000, 3, 9, 7, 15));
        this.cachedFurnaceLevel.put(2, new Furnace.FurnaceLevel(2, 2000, 6, 18, 14, 30));
        this.cachedFurnaceLevel.put(3, new Furnace.FurnaceLevel(3, 2000, 9, 27, 21, 45));
        this.cachedFurnaceLevel.put(4, new Furnace.FurnaceLevel(4, 4000, 12, 36, 28, 60));
        this.cachedFurnaceLevel.put(5, new Furnace.FurnaceLevel(5, 4000, 15, 45, 35, 75));
        this.cachedFurnaceLevel.put(6, new Furnace.FurnaceLevel(6, 6000, 18, 54, 42, 90));
        this.cachedFurnaceLevel.put(7, new Furnace.FurnaceLevel(7, 6000, 21, 63, 49, 105));

        for (final String owner : this.configuration.getSection("furnace").getKeys(false)) {
            for (final String id : this.configuration.getSection("furnace." + owner).getKeys(false)) {
                final Location location = new Location(
                        this.serverCore.getServer().getWorld(this.configuration.getString("furnace." + owner + "." + id + ".world")),
                        this.configuration.getDouble("furnace." + owner + "." + id + ".x"),
                        this.configuration.getDouble("furnace." + owner + "." + id + ".y"),
                        this.configuration.getDouble("furnace." + owner + "." + id + ".z")
                );

                this.cachedFurnaces.put(location,
                        new Furnace(
                                owner,
                                Long.parseLong(id),
                                location,
                                this.cachedFurnaceLevel.get(this.configuration.getInt("furnace." + owner + "." + id + ".data.level")),
                                this.configuration.getInt("furnace." + owner + "." + id + ".data.smelting"),
                                this.configuration.getInt("furnace." + owner + "." + id + ".data.double"),
                                this.configuration.getInt("furnace." + owner + "." + id + ".data.xp")
                        )
                );
            }
        }
    }

    public void placeFurnace(final String player, final Location location, final int level, final int smeltingBoost, final int doublePercent, final int xpBoost) {
        final long id = 1095216660480L + ThreadLocalRandom.current().nextLong(0L, 2147483647L);
        final String path = "furnace." + player + "." + id + ".";
        this.configuration.set(path + "world", location.getWorld().getName());
        this.configuration.set(path + "x", location.getX());
        this.configuration.set(path + "y", location.getY());
        this.configuration.set(path + "z", location.getZ());
        this.configuration.set(path + "data.level", level);
        this.configuration.set(path + "data.smelting", smeltingBoost);
        this.configuration.set(path + "data.double", doublePercent);
        this.configuration.set(path + "data.xp", xpBoost);
        this.configuration.save();
        this.configuration.reload();

        this.cachedFurnaces.put(location, new Furnace(player, id, location, this.cachedFurnaceLevel.get(level), smeltingBoost, doublePercent, xpBoost));
    }

    public void upgradeFurnace(final Furnace furnace, final Furnace.FurnaceLevel furnaceLevel) {
        if (furnace.getId() == -1) {
            this.placeFurnace(furnace.getOwner(), furnace.getLocation(), furnaceLevel.getLevel(), 0, 0, 0);
        } else {
            this.configuration.set("furnace." + furnace.getOwner() + "." + furnace.getId() + ".data.level", furnaceLevel.getLevel());
            this.configuration.save();
            this.configuration.reload();

            this.cachedFurnaces.get(furnace.getLocation()).setFurnaceLevel(furnaceLevel);
        }
    }

    public void breakFurnace(final Furnace furnace) {
        final Map<String, Object> map = this.configuration.getSection("furnace." + furnace.getOwner()).getAllMap();
        map.remove(String.valueOf(furnace.getId()));
        this.configuration.set("furnace." + furnace.getOwner(), map);
        this.configuration.save();
        this.configuration.reload();

        this.cachedFurnaces.remove(furnace.getLocation());
    }
}
