package net.eltown.servercore.components.enchantments;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.listeners.DrillListener;
import net.eltown.servercore.components.enchantments.listeners.EmeraldFarmerListener;
import net.eltown.servercore.components.enchantments.listeners.LumberjackListener;
import net.eltown.servercore.components.enchantments.listeners.VeinMiningListener;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public record CustomEnchantments(ServerCore serverCore) {

    public CustomEnchantments(final ServerCore serverCore) {
        this.serverCore = serverCore;

        this.serverCore.getServer().getPluginManager().registerEvents(new DrillListener(this), this.serverCore);
        this.serverCore.getServer().getPluginManager().registerEvents(new EmeraldFarmerListener(this), this.serverCore);
        this.serverCore.getServer().getPluginManager().registerEvents(new LumberjackListener(this), this.serverCore);
        this.serverCore.getServer().getPluginManager().registerEvents(new VeinMiningListener(this), this.serverCore);
    }

    public void enchantItem(final Player player, final Enchantment enchantment, int level) {
        if (level > enchantment.maxLevel()) level = enchantment.maxLevel();

        final ItemStack itemStack = player.getInventory().getItemInMainHand();

        final ItemMeta meta = itemStack.getItemMeta();
        final PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(this.serverCore, enchantment.name().toUpperCase()), PersistentDataType.INTEGER, level);
        itemStack.setItemMeta(meta);

        if (itemStack.lore() == null) {
            final List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§r" + enchantment.color + enchantment.enchantment + getLevelString(level)));
            itemStack.lore(lore);
        } else {
            final List<String> lore = new LinkedList<>(itemStack.getLore());
            for (final String value : lore) {
                if (value.equals("§r" + enchantment.color + enchantment.enchantment + getLevelString(level))) {
                    lore.remove(value);
                    continue;
                }
                if (value.startsWith("§r" + enchantment.color + enchantment.enchantment))
                    lore.remove(value);
            }
            lore.add("§r" + enchantment.color + enchantment.enchantment + getLevelString(level));
            itemStack.setLore(lore);
        }
        player.getInventory().setItemInMainHand(itemStack);
    }

    public boolean hasEnchantment(final ItemStack itemStack, final Enchantment enchantment) {
        return itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.serverCore, enchantment.name().toUpperCase()), PersistentDataType.INTEGER);
    }

    public int getLevel(final ItemStack itemStack, final Enchantment enchantment) {
        return itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, enchantment.name().toUpperCase()), PersistentDataType.INTEGER);
    }

    public String getLevelString(final int lvl) {
        return switch (lvl) {
            case 1 -> " I";
            case 2 -> " II";
            case 3 -> " III";
            case 4 -> " IV";
            case 5 -> " V";
            case 6 -> " VI";
            case 7 -> " VII";
            case 8 -> " VIII";
            case 9 -> " IX";
            case 10 -> " X";
            default -> " ".concat(String.valueOf(lvl));
        };
    }

    public enum Enchantment {

        // common: §a  uncommon: §e  rare: §b  mythic: §5 legendary: §6
        LUMBERJACK("Holzfäller", "§a", 2),
        THERMAL_PROTECTION("Wärmeschutz", "§b", 1),
        COLD_PROTECTION("Kälteschutz", "§b", 1),
        MAGNET("Magnetfeld", "§e", 3),
        DRILL("Bohrer", "§b", 3),
        EMERALD_FARMER("Smaragdfarmer", "§a", 1),
        EXPERIENCE("Erfahrung", "§e", 4),
        NIGHT_VISION("Nachtsicht", "§e", 1),
        RUNNER("Läufer", "§a", 2),
        VEIN_MINING("Aderabbau", "§b", 4);

        private final String enchantment;
        private final String color;
        private final int maxLevel;

        Enchantment(final String name, final String color, final int maxLevel) {
            this.enchantment = name;
            this.color = color;
            this.maxLevel = maxLevel;
        }

        public String color() {
            return color;
        }

        public String enchantment() {
            return enchantment;
        }

        public int maxLevel() {
            return maxLevel;
        }
    }

}
