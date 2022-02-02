package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.furnace.Furnace;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record FurnaceListener(ServerCore serverCore) implements Listener {

    @EventHandler
    public void on(final FurnaceStartSmeltEvent event) {
        final Block block = event.getBlock();
        final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(block.getLocation());
        if (furnace != null) {
            event.setTotalCookTime(200 - (200 * (furnace.getFurnaceLevel().getSmeltingBoost() + furnace.getSmeltingBoost()) / 100));
        }

    }

    @EventHandler
    public void on(final FurnaceExtractEvent event) {
        final Block block = event.getBlock();
        final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(block.getLocation());
        if (furnace != null) {
            event.setExpToDrop(event.getExpToDrop() * (furnace.getXpBoost() / 100));
        }
    }

    @EventHandler
    public void on(final FurnaceSmeltEvent event) {
        final Block block = event.getBlock();
        final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(block.getLocation());
        if (furnace != null) {
            final ItemStack itemStack = event.getResult();
            boolean b = Math.random() * 100 <= (furnace.getFurnaceLevel().getDoubleChance() + furnace.getDoubleChance());
            if (event.getResult().getAmount() > 61) b = false;
            if (b) {
                itemStack.setAmount(itemStack.getAmount() * 2);
                event.setResult(itemStack);
            }
        }
    }

    private static final Cooldown placeCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(5));

    @EventHandler
    public void on(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final ItemStack itemStack = event.getItemInHand();
        if (block.getType() == Material.FURNACE) {
            if (!placeCooldown.hasCooldown(player.getName())) {
                final PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
                if (!container.isEmpty() && container.has(new NamespacedKey(this.serverCore, "furnace.level"), PersistentDataType.INTEGER)) {
                    final int level = container.get(new NamespacedKey(this.serverCore, "furnace.level"), PersistentDataType.INTEGER);
                    final int smeltingBoost = container.get(new NamespacedKey(this.serverCore, "furnace.smelting"), PersistentDataType.INTEGER);
                    final int doubleChance = container.get(new NamespacedKey(this.serverCore, "furnace.double"), PersistentDataType.INTEGER);
                    final int xpBoost = container.get(new NamespacedKey(this.serverCore, "furnace.xp"), PersistentDataType.INTEGER);

                    this.serverCore.getFurnaceAPI().placeFurnace(player.getName(), block.getLocation(), level, smeltingBoost, doubleChance, xpBoost);
                }
            } else {
                player.sendActionBar(Component.text("§8[§c!§8] §cBitte warte einen Moment."));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (block.getType() == Material.FURNACE) {
            final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(block.getLocation());
            if (furnace != null) {
                if (furnace.getOwner().equals(player.getName()) || player.isOp()) {
                    event.setDropItems(false);

                    final ItemStack furnaceItem = new ItemStack(Material.FURNACE, 1);
                    final ItemMeta meta = furnaceItem.getItemMeta();
                    meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.level"), PersistentDataType.INTEGER, furnace.getFurnaceLevel().getLevel());
                    meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.smelting"), PersistentDataType.INTEGER, furnace.getSmeltingBoost());
                    meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.double"), PersistentDataType.INTEGER, furnace.getDoubleChance());
                    meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "furnace.xp"), PersistentDataType.INTEGER, furnace.getXpBoost());
                    meta.lore(new ArrayList<>(List.of(
                            Component.text("§r§8» §bOfen-Level: §7" + furnace.getFurnaceLevel().getLevel()),
                            Component.text(""),
                            Component.text("§r§fZusätzliche Werte:"),
                            Component.text("§r§8» §bGeschwindigkeit: §a+ §7" + furnace.getSmeltingBoost() + "%"),
                            Component.text("§r§8» §bDoppelter Ertrag: §a+ §7" + furnace.getDoubleChance() + "%"),
                            Component.text("§r§8» §bXP-Boost: §a+ §7" + furnace.getXpBoost() + "%")
                    )));
                    furnaceItem.setItemMeta(meta);
                    player.getInventory().addItem(furnaceItem);

                    this.serverCore.getFurnaceAPI().breakFurnace(furnace);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if ((event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking() && player.getGameMode() == GameMode.SURVIVAL) || (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking() && player.getGameMode() == GameMode.CREATIVE)) {
            final Block block = event.getClickedBlock();
            if (block.getType() == Material.FURNACE) {
                final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(block.getLocation());
                if (furnace != null) {
                    final Furnace.FurnaceLevel nextLevel = this.serverCore.getFurnaceAPI().cachedFurnaceLevel.get(furnace.getFurnaceLevel().getLevel() + 1);
                    if (nextLevel != null) {
                        final SimpleWindow furnaceWindow = new SimpleWindow.Builder("§7» §8Ofen Einstellungen", this.furnaceDashboard(furnace, nextLevel))
                                .addButton("§8» §aUpgrade durchführen", e -> {
                                    this.openBuyUpgradeWindow(player, furnace, nextLevel);
                                })
                                .build();
                        furnaceWindow.send(player);
                    } else {
                        final SimpleWindow furnaceWindow = new SimpleWindow.Builder("§7» §8Ofen Einstellungen", this.furnaceDashboard(furnace))
                                /*.addButton("§8» §aBauteile anbringen", e -> {

                                })*/
                                .build();
                        furnaceWindow.send(player);
                    }
                } else {
                    final Furnace nullFurnace = new Furnace(player.getName(), -1, block.getLocation(), this.serverCore.getFurnaceAPI().cachedFurnaceLevel.get(0), 0, 0, 0);
                    final SimpleWindow furnaceWindow = new SimpleWindow.Builder("§7» §8Ofen Einstellungen", this.furnaceDashboard(nullFurnace, this.serverCore.getFurnaceAPI().cachedFurnaceLevel.get(1)))
                            .addButton("§8» §aUpgrade durchführen", e -> {
                                this.openBuyUpgradeWindow(player, nullFurnace, this.serverCore.getFurnaceAPI().cachedFurnaceLevel.get(1));
                            })
                            .build();
                    furnaceWindow.send(player);
                }
                event.setCancelled(true);
            }
        }
    }

    private String furnaceDashboard(final Furnace furnace, final Furnace.FurnaceLevel nextLevel) {
        return "§8» §9Ofen-Level: §f" + furnace.getFurnaceLevel().getLevel() + "\n" +
                "§8» §9Geschwindigkeit: §f" + furnace.getFurnaceLevel().getSmeltingBoost() + " Prozent §7[§a+ §f" + furnace.getSmeltingBoost() + " Prozent§7]" + "\n" +
                "§8» §9Doppelter Ertrag: §f" + furnace.getFurnaceLevel().getDoubleChance() + " Prozent §7[§a+ §f" + furnace.getDoubleChance() + " Prozent§7]" + "\n" +
                "§8» §9XP-Boost: §f" + furnace.getFurnaceLevel().getXpBoost() + " Prozent §7[§a+ §f" + furnace.getXpBoost() + " Prozent§7]" + "\n" +
                "\n" +
                "§f§lNächstes verfügbares Level:§r\n" +
                "§8» §cOfen-Level: §f" + nextLevel.getLevel() + "\n" +
                "§8» §cGeschwindigkeit: §f" + nextLevel.getSmeltingBoost() + " Prozent\n" +
                "§8» §cDoppelter Ertrag: §f" + nextLevel.getDoubleChance() + " Prozent\n" +
                "§8» §cXP-Boost: §f" + nextLevel.getXpBoost() + " Prozent\n" +
                "\n" +
                "§8» §cBenötigtes Level: §f" + nextLevel.getNeededLevel() + "\n" +
                "§8» §cKosten: §f$" + this.serverCore.getMoneyFormat().format(nextLevel.getPrice()) + "\n";
    }

    private String furnaceDashboard(final Furnace furnace) {
        return "§8» §9Ofen-Level: §f" + furnace.getFurnaceLevel().getLevel() + "\n" +
                "§8» §9Geschwindigkeit: §f" + furnace.getFurnaceLevel().getSmeltingBoost() + " Prozent §7[§a+ §f" + furnace.getSmeltingBoost() + " Prozent§7]" + "\n" +
                "§8» §9Doppelter Ertrag: §f" + furnace.getFurnaceLevel().getDoubleChance() + " Prozent §7[§a+ §f" + furnace.getDoubleChance() + " Prozent§7]" + "\n" +
                "§8» §9XP-Boost: §f" + furnace.getFurnaceLevel().getXpBoost() + " Prozent §7[§a+ §f" + furnace.getXpBoost() + " Prozent§7]" + "\n" +
                "\n" +
                "§8» §cDieser Ofen hat das höchste kaufbare Level erreicht. Du kannst allerdings weitere Ofenbauteile anbringen, um ihn noch besser zu machen.\n";
    }

    private void openBuyUpgradeWindow(final Player player, final Furnace furnace, final Furnace.FurnaceLevel level) {
        if (this.serverCore.getLevelAPI().getLevel(player.getName()).getLevel() < level.getNeededLevel()) {
            player.sendMessage(Language.get("furnace.upgrade.need.level", level.getNeededLevel()));
            return;
        }

        final ModalWindow confirmBuyWindow = new ModalWindow.Builder("§7» §8Upgrade kaufen", "§fMöchtest du diesen Ofen wirklich für §9$" + this.serverCore.getMoneyFormat().format(level.getPrice()) + " §f"
                + "auf §9Level " + level.getLevel() + " §fupgraden?\n\n§8[§c!§8] §cDiese Aktion kann unter keinen Umständen rückgängig gemacht werden!",
                "§8» §aUpgrade durchführen", "§8» §cAbbrechen")
                .onYes(e -> {
                    this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                        if (money >= level.getPrice()) {
                            this.serverCore.getEconomyAPI().reduceMoney(player.getName(), level.getPrice());
                            this.serverCore.getFurnaceAPI().upgradeFurnace(furnace, level);
                            player.sendMessage(Language.get("furnace.upgrade.success", level.getLevel()));
                        } else {
                            player.sendMessage(Language.get("furnace.upgrade.need.money"));
                        }
                    });
                })
                .onNo(e -> {
                })
                .build();
        confirmBuyWindow.send(player);
    }

}
