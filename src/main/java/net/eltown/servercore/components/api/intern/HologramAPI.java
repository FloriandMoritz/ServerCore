package net.eltown.servercore.components.api.intern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.config.Config;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class HologramAPI {

    private final ServerCore serverCore;
    public final HashMap<String, Hologram> holograms = new HashMap<>();
    private final Config configuration;

    @SneakyThrows
    public HologramAPI(final ServerCore serverCore) {
        this.serverCore = serverCore;
        this.configuration = new Config(serverCore.getDataFolder() + "/components/holograms.yml", Config.YAML);
        this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
            this.serverCore.getServer().getWorlds().forEach(world -> {
                world.getEntities().forEach(entity -> {
                    if (entity.getType() == EntityType.ARMOR_STAND) {
                        if (entity.getPersistentDataContainer().has(new NamespacedKey(ServerCore.getServerCore(), "container.hologram"), PersistentDataType.INTEGER)) {
                            entity.remove();
                        }
                    }
                });
            });
        }, 150);
        this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
            this.configuration.getSection("holograms").getAll().getKeys(false).forEach(e -> {
                final Location location = new Location(serverCore.getServer().getWorld(this.configuration.getString("holograms." + e + ".world")), this.configuration.getDouble("holograms." + e + ".x"), this.configuration.getDouble("holograms." + e + ".y"), this.configuration.getDouble("holograms." + e + ".z"));
                final List<String> lines = this.configuration.getStringList("holograms." + e + ".lines");
                this.holograms.put(e, new Hologram(e, location));
                this.holograms.get(e).create(lines);
            });
        }, 170);
    }

    public boolean hologramExists(final String id) {
        return this.holograms.containsKey(id);
    }

    public void createHologram(final String id, final Location location, final List<String> lines) {
        this.configuration.set("holograms." + id + ".world", location.getWorld().getName());
        this.configuration.set("holograms." + id + ".x", location.getX());
        this.configuration.set("holograms." + id + ".y", location.getY());
        this.configuration.set("holograms." + id + ".z", location.getZ());
        this.configuration.set("holograms." + id + ".lines", lines);
        this.configuration.save();
        this.configuration.reload();

        final Hologram hologram = new Hologram(id, location);
        hologram.create(lines);
        this.holograms.put(id, hologram);
    }

    public void deleteHologram(final String id) {
        final Map<String, Object> map = this.configuration.getSection("holograms").getAllMap();
        map.remove(id);
        this.configuration.set("holograms", map);
        this.configuration.save();
        this.configuration.reload();

        this.holograms.get(id).remove();
        this.holograms.remove(id);
    }

    public void addLine(final String id, final String text) {
        final Hologram hologram = this.holograms.get(id);
        hologram.addLine(text);

        final List<String> lines = this.configuration.getStringList("holograms." + id + ".lines");
        lines.add(text);
        this.configuration.set("holograms." + id + ".lines", lines);
        this.configuration.save();
        this.configuration.reload();
    }

    public void removeLine(final String id, final int line) {
        final Hologram hologram = this.holograms.get(id);
        hologram.removeLine(line);

        final List<String> lines = this.configuration.getStringList("holograms." + id + ".lines");
        lines.removeIf(s -> s.startsWith(lines.get(line - 1)));
        this.configuration.set("holograms." + id + ".lines", lines);
        this.configuration.save();
        this.configuration.reload();
    }

    public void setLine(final String id, final int line, final String text) {
        final Hologram hologram = this.holograms.get(id);
        hologram.setLine(line, text);

        final List<String> lines = this.configuration.getStringList("holograms." + id + ".lines");
        lines.set(line - 1, text);
        this.configuration.set("holograms." + id + ".lines", lines);
        this.configuration.save();
        this.configuration.reload();
    }

    public void moveHologram(final String id, final Location location) {
        final Hologram hologram = this.holograms.get(id);
        hologram.moveTo(location);

        this.configuration.set("holograms." + id + ".world", location.getWorld().getName());
        this.configuration.set("holograms." + id + ".x", location.getX());
        this.configuration.set("holograms." + id + ".y", location.getY());
        this.configuration.set("holograms." + id + ".z", location.getZ());
        this.configuration.save();
        this.configuration.reload();
    }

    public void removeAll() {
        this.holograms.values().forEach(Hologram::remove);
    }

    public void hologramHideTo(final String id, final Player player) {
        final Hologram hologram = this.holograms.get(id);
        hologram.getLines().forEach(e -> {
            player.hideEntity(this.serverCore, (Entity) e);
        });
    }

    public void hologramShowTo(final String id, final Player player) {
        final Hologram hologram = this.holograms.get(id);
        hologram.getLines().forEach(e -> {
            player.showEntity(this.serverCore, (Entity) e);
        });
    }

    @Getter
    @Setter
    public static class Hologram {

        private final String id;
        private Location location;
        private LinkedList<Line> lines;

        public Hologram(final String id, final Location location) {
            this.id = id;
            this.location = location;
            this.lines = new LinkedList<>();
        }

        public void create(final List<String> lines) {
            int i = 1;
            for (final String line : lines) {
                final ArmorStand armorStand = (ArmorStand) this.location.getWorld().spawnEntity(this.getLocation(i), EntityType.ARMOR_STAND);
                armorStand.getPersistentDataContainer().set(new NamespacedKey(ServerCore.getServerCore(), "container.hologram"), PersistentDataType.INTEGER, 1);
                armorStand.setGravity(false);
                armorStand.setCanPickupItems(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setVisible(false);
                armorStand.setCustomName(line);
                armorStand.setCanMove(false);

                this.lines.add(new Line(line, armorStand));
                i++;
            }
        }

        public void remove() {
            this.lines.forEach(e -> e.getArmorStand().remove());
        }

        public void addLine(final String text) {
            final ArmorStand armorStand = (ArmorStand) this.location.getWorld().spawnEntity(this.getLocation(this.lines.size() + 1), EntityType.ARMOR_STAND);
            armorStand.getPersistentDataContainer().set(new NamespacedKey(ServerCore.getServerCore(), "container.hologram"), PersistentDataType.INTEGER, 1);
            armorStand.setGravity(false);
            armorStand.setCanPickupItems(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setVisible(false);
            armorStand.setCustomName(text);
            armorStand.setCanMove(false);

            this.lines.add(new Line(text, armorStand));
        }

        private Location getLocation(final int line) {
            return new Location(this.location.getWorld(), this.getLocation().getX(), this.getLocation().getY() + (line * -.37), this.getLocation().getZ());
        }

        public void removeLine(final int line) {
            this.lines.get(line - 1).getArmorStand().remove();
            this.lines.remove(line - 1);
            this.remove();

            int i = 1;
            for (final Line e : new ArrayList<>(this.lines)) {
                final String set = e.getText();
                final ArmorStand armorStand = (ArmorStand) this.location.getWorld().spawnEntity(this.getLocation(i), EntityType.ARMOR_STAND);
                armorStand.getPersistentDataContainer().set(new NamespacedKey(ServerCore.getServerCore(), "container.hologram"), PersistentDataType.INTEGER, 1);
                armorStand.setGravity(false);
                armorStand.setCanPickupItems(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setVisible(false);
                armorStand.setCustomName(set);
                armorStand.setCanMove(false);

                this.lines.remove(e);
                this.lines.add(new Line(set, armorStand));
                i++;
            }
        }

        public void moveTo(final Location location) {
            this.location = location;
            this.remove();

            int i = 1;
            for (final Line e : new ArrayList<>(this.lines)) {
                final String set = e.getText();
                final ArmorStand armorStand = (ArmorStand) this.location.getWorld().spawnEntity(this.getLocation(i), EntityType.ARMOR_STAND);
                armorStand.getPersistentDataContainer().set(new NamespacedKey(ServerCore.getServerCore(), "container.hologram"), PersistentDataType.INTEGER, 1);
                armorStand.setGravity(false);
                armorStand.setCanPickupItems(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setVisible(false);
                armorStand.setCustomName(set);
                armorStand.setCanMove(false);

                this.lines.remove(e);
                this.lines.add(new Line(set, armorStand));
                i++;
            }
        }

        public void setLine(final int line, final String text) {
            this.lines.get(line - 1).setText(text);
        }

        @AllArgsConstructor
        @Getter
        public static class Line {

            private String text;
            private ArmorStand armorStand;

            public void setText(final String text) {
                this.text = text;
                this.armorStand.setCustomName(text);
            }

        }

    }

}
