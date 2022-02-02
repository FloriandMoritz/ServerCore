package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GiveFurnaceCommand extends Command {

    private final ServerCore serverCore;

    public GiveFurnaceCommand(final ServerCore serverCore) {
        super("givefurnace");
        this.serverCore = serverCore;
        this.setDescription("Lasse dir einen gelevelten Ofen geben");
        this.setPermission("core.command.givefurnace");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player player) {
            final CustomWindow giveFurnaceWindow = new CustomWindow("§7» §8Ofen geben");
            giveFurnaceWindow.form()
                    .slider("§8» §fGebe das Level des Ofens an", 1, 7, 1, 1)
                    .slider("§8» §fZusätzliche Brenngeschwindigkeit", 0, 100, 1, 0)
                    .slider("§8» §fZusätzliche Chance auf doppelten Ertrag", 0, 100, 1, 0)
                    .slider("§8» §fZusätzliche Erfahrungspunkte", 0, 100, 1, 0)
                    .slider("§8» §fAnzahl des Items", 1, 64, 1, 1);

            giveFurnaceWindow.onSubmit((g, h) -> {
                final int level = (int) h.getSlider(0);
                final int smeltingBoost = (int) h.getSlider(1);
                final int doubleChance = (int) h.getSlider(2);
                final int xpBoost = (int) h.getSlider(3);
                final int amount = (int) h.getSlider(4);

                final ItemStack furnace = new ItemStack(Material.FURNACE, amount);
                final ItemMeta meta = furnace.getItemMeta();
                meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.level"), PersistentDataType.INTEGER, level);
                meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.smelting"), PersistentDataType.INTEGER, smeltingBoost);
                meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.double"), PersistentDataType.INTEGER, doubleChance);
                meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.xp"), PersistentDataType.INTEGER, xpBoost);
                meta.lore(new ArrayList<>(List.of(
                        Component.text("§r§8» §bOfen-Level: §7" + level),
                        Component.text(""),
                        Component.text("§r§fZusätzliche Werte:"),
                        Component.text("§r§8» §bGeschwindigkeit: §a+ §7" + smeltingBoost + "%"),
                        Component.text("§r§8» §bDoppelter Ertrag: §a+ §7" + doubleChance + "%"),
                        Component.text("§r§8» §bXP-Boost: §a+ §7" + xpBoost + "%")
                )));
                furnace.setItemMeta(meta);
                player.getInventory().addItem(furnace);
                player.sendMessage("§8» §fCore §8| §7Der Ofen wurde erstellt.");
            });
            giveFurnaceWindow.send(player);
        }
        return true;
    }
}
