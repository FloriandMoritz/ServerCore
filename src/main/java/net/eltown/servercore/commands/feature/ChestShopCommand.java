package net.eltown.servercore.commands.feature;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.language.Language;
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

import java.util.List;

public class ChestShopCommand extends Command {

    private final ServerCore serverCore;

    public ChestShopCommand(final ServerCore serverCore) {
        super("chestshop", "Erstelle einen neuen ChestShop", "", List.of("cs", "shop", "kistenshop"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            final ItemStack itemStack = player.getInventory().getItemInMainHand().clone();
            itemStack.setAmount(1);

            if (this.serverCore.getLevelAPI().getLevel(player.getName()).getLevel() < 2) {
                player.sendMessage(Language.get("chestshop.create.invalid.level"));
                return true;
            }

            if (itemStack.getType() == Material.AIR) {
                player.sendMessage(Language.get("chestshop.create.invalid.item"));
                return true;
            }

            final ShopLicense shopLicense = this.serverCore.getChestShopAPI().getPlayerLicense(player.getName());
            final int chestShops = this.serverCore.getChestShopAPI().countPlayerChestShops(player.getName());
            if (chestShops < (shopLicense.getLicense().maxPossibleShops() + shopLicense.getAdditionalShops())) {
                final CustomWindow chestShopWindow = new CustomWindow("§7» §8ChestShop erstellen");
                chestShopWindow.form()
                        .label("§8» §fDeine Lizenz: §e" + shopLicense.getLicense().displayName() + "\n§8» §fDeine Shops: §e" + chestShops + "§f/§e" + shopLicense.getLicense().maxPossibleShops() + "\n\n§7» §fItem: §9" + itemStack.getI18NDisplayName())
                        .input("§8» §fBitte gebe an, mit welcher Stückzahl du das Item verkaufen möchtest.", "16")
                        .input("§8» §fFür welchen Preis möchtest du das Item verkaufen?", "29.95")
                        .dropdown("§8» §fIch verkaufe Items an Spieler: §eBUY\n§7» §fSpieler verkaufen Items an mich: §eSELL", 0, "BUY", "SELL");

                chestShopWindow.onSubmit((g, h) -> {
                    try {
                        final int amount = Integer.parseInt(h.getInput(1));
                        final double price = Double.parseDouble(h.getInput(2).replace(",", "."));
                        if (amount <= 0) throw new Exception("Invalid chest shop amount.");
                        if (price < 0) throw new Exception("Invalid chest shop price.");

                        final ItemStack sign = new ItemStack(Material.OAK_SIGN, 1);
                        final ItemMeta meta = sign.getItemMeta();
                        meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "chestshops.creator"), PersistentDataType.STRING, player.getName());
                        meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "chestshops.item"), PersistentDataType.STRING, SyncAPI.ItemAPI.itemStackToBase64(itemStack));
                        meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "chestshops.type"), PersistentDataType.STRING, this.getType(h.getDropdown(3)));
                        meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "chestshops.amount"), PersistentDataType.INTEGER, amount);
                        meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "chestshops.price"), PersistentDataType.DOUBLE, price);
                        meta.displayName(Component.text("§r§8» §6ChestShop erstellen"));
                        meta.lore(List.of(
                                Component.text("§r§7Bitte platziere dieses Schild an eine"),
                                Component.text("§r§7Kiste, um einen ChestShop zu erstellen.")
                        ));
                        sign.setItemMeta(meta);
                        player.getInventory().addItem(sign);

                        player.sendMessage(Language.get("chestshop.create.info"));
                    } catch (final Exception e) {
                        player.sendMessage(Language.get("chestshop.create.invalid.input"));
                    }
                });
                chestShopWindow.send(player);
            } else {
                player.sendMessage(Language.get("chestshop.create.too.many.shops"));
            }
        }
        return true;
    }

    private String getType(final int d) {
        return switch (d) {
            case 0 -> "BUY";
            case 1 -> "SELL";
            default -> "null";
        };
    }
}
