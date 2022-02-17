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
                if (value.contains(enchantment.color + enchantment.enchantment)) lore.remove(value);
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

    private boolean containsType(final String haystack, final String... needles) {
        for (final String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    public boolean canApply(final ItemStack itemStack, final Enchantment enchantment) {
        switch (enchantment.type()) {
            case ARMOR -> {
                return this.containsType(itemStack.getType().name(), "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
            }
            case ARMOR_HEAD -> {
                return this.containsType(itemStack.getType().name(), "HELMET");
            }
            case ARMOR_TORSO -> {
                return this.containsType(itemStack.getType().name(), "CHESTPLATE");
            }
            case ARMOR_LEGS -> {
                return this.containsType(itemStack.getType().name(), "LEGGINGS");
            }
            case ARMOR_FEET -> {
                return this.containsType(itemStack.getType().name(), "BOOTS");
            }
            case TOOL -> {
                return this.containsType(itemStack.getType().name(), "SWORD", "PICKAXE", "_AXE", "HOE", "SHOVEL");
            }
            case SWORD -> {
                return this.containsType(itemStack.getType().name(), "SWORD");
            }
            case PICKAXE -> {
                return this.containsType(itemStack.getType().name(), "PICKAXE");
            }
            case AXE -> {
                return this.containsType(itemStack.getType().name(), "_AXE");
            }
            case HOE -> {
                return this.containsType(itemStack.getType().name(), "HOE");
            }
            case SHOVEL -> {
                return this.containsType(itemStack.getType().name(), "SHOVEL");
            }
            case BOW -> {
                return itemStack.getType().name().equals("BOW");
            }
        }
        return false;
    }

    public enum Enchantment {

        // common: §a  uncommon: §e  rare: §b  mythic: §5 legendary: §6
        LUMBERJACK("Holzfäller", "§a", 2, EnchantmentType.AXE, 4, 799.95),
        THERMAL_PROTECTION("Wärmeschutz", "§b", 1, EnchantmentType.ARMOR, 0, 199.95),
        COLD_PROTECTION("Kälteschutz", "§b", 1, EnchantmentType.ARMOR, 0, 199.95),
        MAGNET("Magnetfeld", "§e", 3, EnchantmentType.TOOL, 14, 1799.95),
        DRILL("Bohrer", "§b", 3, EnchantmentType.PICKAXE, 8, 2499.95),
        EMERALD_FARMER("Smaragdfarmer", "§a", 1, EnchantmentType.PICKAXE, 7, 349.95),
        EXPERIENCE("Erfahrung", "§e", 4, EnchantmentType.TOOL, 6, 799.95),
        NIGHT_VISION("Nachtsicht", "§e", 1, EnchantmentType.ARMOR_HEAD, 0, 499.95),
        RUNNER("Läufer", "§a", 2, EnchantmentType.ARMOR_FEET, 0, 799.95),
        VEIN_MINING("Aderabbau", "§b", 4, EnchantmentType.PICKAXE, 12, 1399.95);

        private final String enchantment;
        private final String color;
        private final int maxLevel;
        private final EnchantmentType type;
        private final int level;
        private final double price;

        Enchantment(final String name, final String color, final int maxLevel, final EnchantmentType type, final int level, final double price) {
            this.enchantment = name;
            this.color = color;
            this.maxLevel = maxLevel;
            this.type = type;
            this.level = level;
            this.price = price;
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

        public EnchantmentType type() {
            return type;
        }

        public int level() {
            return level;
        }

        public double price() {
            return price;
        }
    }

    public enum EnchantmentType {

        ARMOR,
        ARMOR_HEAD,
        ARMOR_TORSO,
        ARMOR_LEGS,
        ARMOR_FEET,
        TOOL,
        AXE,
        PICKAXE,
        SWORD,
        SHOVEL,
        HOE,
        BOW

    }

}
