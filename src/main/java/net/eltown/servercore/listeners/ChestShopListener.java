package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.bank.BankAccount;
import net.eltown.servercore.components.data.chestshop.ChestShop;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public record ChestShopListener(ServerCore serverCore) implements Listener {

    @EventHandler
    public void on(final SignChangeEvent event) {
        final Player player = event.getPlayer();
        if (this.serverCore.getChestShopAPI().cachedChestShops.get(event.getBlock().getLocation()) != null) {
            final ChestShop chestShop = this.serverCore.getChestShopAPI().cachedChestShops.get(event.getBlock().getLocation());
            if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
                event.line(0, Component.text("§c[§4ChestShop§c]"));
                event.line(1, Component.text("§cKaufe: §4" + chestShop.getShopCount() + "x"));
                event.line(2, Component.text("§c$§4" + this.serverCore.getMoneyFormat().format(chestShop.getShopPrice())));
                event.line(3, Component.text("§4" + player.getName()));
            } else {
                event.line(0, Component.text("§c[§4ChestShop§c]"));
                event.line(1, Component.text("§cVerkaufe: §4" + chestShop.getShopCount() + "x"));
                event.line(2, Component.text("§c$§4" + this.serverCore.getMoneyFormat().format(chestShop.getShopPrice())));
                event.line(3, Component.text("§4" + player.getName()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final ItemStack item = event.getItemInHand();

        this.serverCore.getChestShopAPI().cachedChestShops.forEach((e, h) -> {
            if (h.getChestLocation().getBlock().getRelative(BlockFace.UP).equals(block)) event.setCancelled(true);
        });

        if (this.serverCore.getChestShopAPI().isSign(item.getType())) {
            if (event.getBlockAgainst().getType() == Material.CHEST) {
                if (item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "chestshops.creator"), PersistentDataType.STRING)) {
                    if (!this.chestIsUsed(event.getBlockAgainst())) {
                        final ShopLicense shopLicense = this.serverCore.getChestShopAPI().getPlayerLicense(player.getName());
                        final int chestShops = this.serverCore.getChestShopAPI().countPlayerChestShops(player.getName());
                        if (chestShops < (shopLicense.getLicense().maxPossibleShops() + shopLicense.getAdditionalShops())) {
                            final String creator = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "chestshops.creator"), PersistentDataType.STRING);
                            if (creator.equals(player.getName())) {
                                final String rawItem = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "chestshops.item"), PersistentDataType.STRING);
                                final int amount = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "chestshops.amount"), PersistentDataType.INTEGER);
                                final double price = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "chestshops.price"), PersistentDataType.DOUBLE);
                                final ChestShop.ShopType type = ChestShop.ShopType.valueOf(item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "chestshops.type"), PersistentDataType.STRING).toUpperCase());

                                this.serverCore.getChestShopAPI().cachedChestShops.put(block.getLocation(), new ChestShop(block.getLocation(), event.getBlockAgainst().getLocation(),
                                        -1, player.getName(), type, price, amount, SyncAPI.ItemAPI.itemStackFromBase64(rawItem), null));
                                player.sendMessage(Language.get("chestshop.create.bank.info"));

                                this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
                                    if (this.serverCore.getChestShopAPI().isWallSign(block.getType())) {
                                        final Sign sign = (Sign) block.getState();
                                        if (type == ChestShop.ShopType.BUY) {
                                            sign.line(0, Component.text("§c[§4ChestShop§c]"));
                                            sign.line(1, Component.text("§cKaufe: §4" + amount + "x"));
                                            sign.line(2, Component.text("§c$§4" + this.serverCore.getMoneyFormat().format(price)));
                                            sign.line(3, Component.text("§4" + player.getName()));
                                        } else if (type == ChestShop.ShopType.SELL) {
                                            sign.line(0, Component.text("§c[§4ChestShop§c]"));
                                            sign.line(1, Component.text("§cVerkaufe: §4" + amount + "x"));
                                            sign.line(2, Component.text("§c$§4" + this.serverCore.getMoneyFormat().format(price)));
                                            sign.line(3, Component.text("§4" + player.getName()));
                                        }
                                        sign.setGlowingText(true);
                                        sign.update(true);
                                    }
                                }, 40);
                            } else {
                                player.sendMessage(Language.get("chestshop.create.invalid.player"));
                                event.setCancelled(true);
                            }
                        } else {
                            player.sendMessage(Language.get("chestshop.create.too.many.shops"));
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(Language.get("chestshop.create.chest.already.used"));
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private static final Cooldown interactCooldown = new Cooldown(TimeUnit.MILLISECONDS.toMillis(250));
    private static final Cooldown messageCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(4));
    private static final HashMap<String, String> cachedBankAccounts = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        final ItemStack item = player.getInventory().getItemInMainHand();

        if (block == null) return;
        if (!interactCooldown.hasCooldown(player.getName())) {
            if (this.serverCore.getChestShopAPI().isWallSign(block.getType())) {
                if (this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation()) != null) {
                    final ChestShop chestShop = this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation());
                    if (chestShop.getOwner().equals(player.getName()) && chestShop.getId() == -1) {
                        /*
                         * Register chest shop with bank card
                         */
                        if (item.getType() == Material.PAPER) {
                            if (item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING)) {
                                final String account = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING);
                                this.serverCore.getBankAPI().getAccount(account, bankAccount -> {
                                    if (bankAccount != null) {
                                        this.requestBankLogin(player, bankAccount, "Bitte gebe das Passwort des Kontos an, um eine neue Verknüpfung zu erstellen.", a -> {
                                            if (a) {
                                                this.serverCore.getChestShopAPI().cachedChestShops.remove(block.getLocation());
                                                this.serverCore.getChestShopAPI().createChestShop(chestShop.getSignLocation(), chestShop.getChestLocation(), player, chestShop.getShopType(),
                                                        chestShop.getShopPrice(), chestShop.getShopCount(), chestShop.getItem(), bankAccount.getAccount());

                                                final Sign sign = (Sign) block.getState();
                                                if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
                                                    sign.line(0, Component.text("§a[§2ChestShop§a]"));
                                                    sign.line(1, Component.text("§0Kaufe: §2" + chestShop.getShopCount() + "x"));
                                                    sign.line(2, Component.text("§f$" + this.serverCore.getMoneyFormat().format(chestShop.getShopPrice())));
                                                    sign.line(3, Component.text("§2" + player.getName()));
                                                } else if (chestShop.getShopType() == ChestShop.ShopType.SELL) {
                                                    sign.line(0, Component.text("§a[§2ChestShop§a]"));
                                                    sign.line(1, Component.text("§0Verkaufe: §2" + chestShop.getShopCount() + "x"));
                                                    sign.line(2, Component.text("§f$" + this.serverCore.getMoneyFormat().format(chestShop.getShopPrice())));
                                                    sign.line(3, Component.text("§2" + player.getName()));
                                                }
                                                sign.setGlowingText(true);
                                                sign.update(true);

                                                player.sendMessage(Language.get("chestshop.created"));
                                            } else {
                                                player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                            }
                                        });
                                    } else {
                                        player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                    }
                                });
                            } else {
                                player.sendMessage(Language.get("roleplay.bank.card.needed"));
                                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                            }
                        } else {
                            player.sendMessage(Language.get("roleplay.bank.card.needed"));
                            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                            chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                        }
                    } else {
                        if (chestShop.getId() != -1) {
                            final Chest chest = (Chest) chestShop.getChestLocation().getBlock().getState();
                            if (chest != null) {
                                /*
                                 * Edit existing chest shop
                                 */
                                if (chestShop.getOwner().equals(player.getName()) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                                    final CustomWindow editChestShopWindow = new CustomWindow("§7» §8ChestShop bearbeiten");
                                    editChestShopWindow.form()
                                            .label("§7» §fChestShopID: §9" + chestShop.getId())
                                            .input("§7» §fStückzahl bearbeiten:", "10", "" + chestShop.getShopCount())
                                            .input("§7» §fPreis bearbeiten:", "29.95", "" + this.serverCore.getMoneyFormat().format(chestShop.getShopPrice()))
                                            .toggle("§7» §fDas Item in deiner Hand als neues Kauf- oder Verkaufsitem setzen.", false);

                                    editChestShopWindow.onSubmit((g, h) -> {
                                        try {
                                            final int givenCount = Integer.parseInt(h.getInput(1));
                                            final double givenPrice = Double.parseDouble(h.getInput(2).replace(",", "."));
                                            final boolean updateItem = h.getToggle(3);

                                            if (givenCount <= 0) throw new Exception("Invalid chest shop amount.");
                                            if (givenPrice < 0) throw new Exception("Invalid chest shop price.");

                                            if (givenCount != chestShop.getShopCount()) {
                                                this.serverCore.getChestShopAPI().updateAmount(chestShop, givenCount);
                                                player.sendMessage(Language.get("chestshop.edit.amount"));
                                            }
                                            if (givenPrice != chestShop.getShopPrice()) {
                                                this.serverCore.getChestShopAPI().updatePrice(chestShop, givenPrice);
                                                player.sendMessage(Language.get("chestshop.edit.price"));
                                            }
                                            if (updateItem) {
                                                if (!(player.getInventory().getItemInMainHand().getType() == Material.AIR)) {
                                                    this.serverCore.getChestShopAPI().updateItem(player, chestShop, SyncAPI.ItemAPI.itemStackToBase64(player.getInventory().getItemInMainHand()));
                                                    player.sendMessage(Language.get("chestshop.edit.item"));
                                                } else {
                                                    player.sendMessage(Language.get("chestshop.create.invalid.item"));
                                                }
                                            }
                                        } catch (final Exception e) {
                                            player.sendMessage(Language.get("chestshop.create.invalid.input"));
                                        }
                                    });
                                    editChestShopWindow.send(player);
                                    return;
                                } else if (chestShop.getOwner().equals(player.getName()) && event.getAction() == Action.LEFT_CLICK_BLOCK) return;

                                /*
                                 * Interacting with existing chest shop
                                 */
                                final AtomicInteger count = new AtomicInteger(0);
                                if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
                                    /*
                                     * Buy
                                     */
                                    for (final ItemStack itemStack : chest.getInventory().getContents()) {
                                        if (itemStack == null) continue;
                                        final ItemStack chestItem = itemStack.clone();
                                        chestItem.setAmount(1);
                                        if (SyncAPI.ItemAPI.itemStackToBase64(chestItem).equals(SyncAPI.ItemAPI.itemStackToBase64(chestShop.getItem()))) count.addAndGet(itemStack.getAmount());
                                    }

                                    if (count.get() >= chestShop.getShopCount()) {
                                        final ItemStack buyItem = chestShop.getItem().clone();
                                        buyItem.setAmount(chestShop.getShopCount());
                                        if (item.getType() == Material.PAPER && item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING)) {
                                            final String account = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING);
                                            this.serverCore.getBankAPI().getAccount(account, bankAccount -> {
                                                if (bankAccount != null) {
                                                    if (cachedBankAccounts.containsKey(player.getName())) {
                                                        if (!cachedBankAccounts.get(player.getName()).equals(bankAccount.getAccount())) {
                                                            this.requestBankLogin(player, bankAccount, "Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das Passwort erneut eingeben zu müssen.", b -> {
                                                                if (b) {
                                                                    cachedBankAccounts.remove(player.getName());
                                                                    cachedBankAccounts.put(player.getName(), bankAccount.getAccount());
                                                                    player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                                } else {
                                                                    player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                                }
                                                            });
                                                            return;
                                                        }
                                                        if (bankAccount.getBalance() >= chestShop.getShopPrice()) {
                                                            if (this.serverCore.canAddItem(player.getInventory(), buyItem)) {
                                                                this.serverCore.getBankAPI().withdrawMoney(bankAccount.getAccount(), chestShop.getShopPrice());
                                                                this.serverCore.getBankAPI().depositMoney(chestShop.getBankAccount(), chestShop.getShopPrice());

                                                                chest.getInventory().removeItem(buyItem);
                                                                chest.update(true);
                                                                player.getInventory().addItem(buyItem);

                                                                if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId()))
                                                                    player.sendMessage(Language.get("chestshop.interact.bought.bank", buyItem.getI18NDisplayName(), chestShop.getShopCount(), this.serverCore.getMoneyFormat().format(chestShop.getShopPrice()), bankAccount.getDisplayName()));
                                                                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                                                chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.EMERALD_BLOCK.createBlockData());
                                                            } else {
                                                                if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.self"));
                                                                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                                chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                            }
                                                        } else {
                                                            if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.buy.no.money.bank"));
                                                            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                            chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                        }
                                                    } else {
                                                        this.requestBankLogin(player, bankAccount, "Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das Passwort erneut eingeben zu müssen.", b -> {
                                                            if (b) {
                                                                cachedBankAccounts.remove(player.getName());
                                                                cachedBankAccounts.put(player.getName(), bankAccount.getAccount());
                                                                player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                            } else {
                                                                player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                                                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                    chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                }

                                            });
                                        } else {
                                            this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                                                if (money >= chestShop.getShopPrice()) {
                                                    if (this.serverCore.canAddItem(player.getInventory(), buyItem)) {
                                                        this.serverCore.getEconomyAPI().reduceMoney(player.getName(), chestShop.getShopPrice());
                                                        this.serverCore.getBankAPI().depositMoney(chestShop.getBankAccount(), chestShop.getShopPrice());

                                                        chest.getInventory().removeItem(buyItem);
                                                        chest.update(true);
                                                        player.getInventory().addItem(buyItem);

                                                        if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.bought", buyItem.getI18NDisplayName(), chestShop.getShopCount(), this.serverCore.getMoneyFormat().format(chestShop.getShopPrice())));
                                                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.EMERALD_BLOCK.createBlockData());
                                                    } else {
                                                        if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.self"));
                                                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                    }
                                                } else {
                                                    if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.money"));
                                                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                    chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                }
                                            });
                                        }
                                    } else {
                                        if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.out.of.stock"));
                                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                    }
                                } else if (chestShop.getShopType() == ChestShop.ShopType.SELL) {
                                    /*
                                     * Sell
                                     */
                                    for (final ItemStack itemStack : player.getInventory().getContents()) {
                                        if (itemStack == null) continue;
                                        final ItemStack chestItem = itemStack.clone();
                                        chestItem.setAmount(1);
                                        if (SyncAPI.ItemAPI.itemStackToBase64(chestItem).equals(SyncAPI.ItemAPI.itemStackToBase64(chestShop.getItem()))) count.addAndGet(itemStack.getAmount());
                                    }

                                    if (count.get() >= chestShop.getShopCount()) {
                                        final ItemStack buyItem = chestShop.getItem().clone();
                                        buyItem.setAmount(chestShop.getShopCount());
                                        if (item.getType() == Material.PAPER && item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING)) {
                                            final String account = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING);
                                            this.serverCore.getBankAPI().getAccount(account, sellAccount -> {
                                                this.serverCore.getBankAPI().getAccount(chestShop.getBankAccount(), chestAccount -> {
                                                    if (sellAccount != null && chestAccount != null) {
                                                        if (cachedBankAccounts.containsKey(player.getName())) {
                                                            if (!cachedBankAccounts.get(player.getName()).equals(sellAccount.getAccount())) {
                                                                this.requestBankLogin(player, sellAccount, "Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das Passwort erneut eingeben zu müssen.", b -> {
                                                                    if (b) {
                                                                        cachedBankAccounts.remove(player.getName());
                                                                        cachedBankAccounts.put(player.getName(), sellAccount.getAccount());
                                                                        player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                                    } else {
                                                                        player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                                    }
                                                                });
                                                                return;
                                                            }
                                                            if (chestAccount.getBalance() >= chestShop.getShopPrice()) {
                                                                if (this.serverCore.canAddItem(chest.getInventory(), buyItem)) {
                                                                    this.serverCore.getBankAPI().withdrawMoney(chestAccount.getAccount(), chestShop.getShopPrice());
                                                                    this.serverCore.getBankAPI().depositMoney(sellAccount.getAccount(), chestShop.getShopPrice());

                                                                    chest.getInventory().addItem(buyItem);
                                                                    chest.update(true);
                                                                    player.getInventory().removeItem(buyItem);

                                                                    if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId()))
                                                                        player.sendMessage(Language.get("chestshop.interact.sold.bank", buyItem.getI18NDisplayName(), chestShop.getShopCount(), this.serverCore.getMoneyFormat().format(chestShop.getShopPrice()), sellAccount.getDisplayName()));
                                                                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                                                    chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.EMERALD_BLOCK.createBlockData());
                                                                } else {
                                                                    if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.chest"));
                                                                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                                    chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                                }
                                                            } else {
                                                                if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.money.bank"));
                                                                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                                chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                            }
                                                        } else {
                                                            this.requestBankLogin(player, sellAccount, "Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das Passwort erneut eingeben zu müssen.", b -> {
                                                                if (b) {
                                                                    cachedBankAccounts.remove(player.getName());
                                                                    cachedBankAccounts.put(player.getName(), sellAccount.getAccount());
                                                                    player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                                } else {
                                                                    player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                                }
                                                            });
                                                        }
                                                    } else {
                                                        player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                                                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                    }
                                                });
                                            });
                                        } else {
                                            this.serverCore.getBankAPI().getAccount(chestShop.getBankAccount(), bankAccount -> {
                                                if (bankAccount.getBalance() >= chestShop.getShopPrice()) {
                                                    if (this.serverCore.canAddItem(chest.getInventory(), buyItem)) {
                                                        this.serverCore.getBankAPI().withdrawMoney(bankAccount.getAccount(), chestShop.getShopPrice());
                                                        this.serverCore.getEconomyAPI().addMoney(player.getName(), chestShop.getShopPrice());

                                                        chest.getInventory().addItem(buyItem);
                                                        chest.update(true);
                                                        player.getInventory().removeItem(buyItem);

                                                        if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId()))
                                                            player.sendMessage(Language.get("chestshop.interact.sold", buyItem.getI18NDisplayName(), chestShop.getShopCount(), this.serverCore.getMoneyFormat().format(chestShop.getShopPrice())));
                                                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.EMERALD_BLOCK.createBlockData());
                                                    } else {
                                                        if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.chest"));
                                                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                    }
                                                } else {
                                                    if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.money.bank"));
                                                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                                    chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                                }
                                            });
                                        }
                                    } else {
                                        if (!messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.items"));
                                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                        chestShop.getChestLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, chestShop.getSignLocation().clone().add(0, 1, 0), 20, Material.REDSTONE_BLOCK.createBlockData());
                                    }
                                }
                            }
                        }
                    }
                    event.setCancelled(true);
                }
            } else if (block.getType() == Material.CHEST) {
                this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                    if (e.getChestLocation().equals(block.getLocation())) {
                        if (!e.getOwner().equals(player.getName()) && !player.isOp()) {
                            player.sendMessage(Language.get("chestshop.interact.chest"));
                            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                            event.setCancelled(true);
                        }
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (block.getType() == Material.CHEST) {
            this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                if (e.getChestLocation().equals(block.getLocation()) && e.getId() == -1) {
                    if (e.getOwner().equals(player.getName())) {
                        player.sendMessage(Language.get("chestshop.create.destroy"));
                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                    }
                    event.setCancelled(true);
                }
            });

            this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                if (e.getChestLocation().equals(block.getLocation()) && e.getId() != -1) {
                    if (e.getOwner().equals(player.getName())) {
                        player.sendMessage(Language.get("chestshop.break.chest"));
                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                    }
                    event.setCancelled(true);
                }
            });
        } else if (this.serverCore.getChestShopAPI().isWallSign(block.getType())) {
            if (this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation()) != null && this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation()).getId() == -1) {
                player.sendMessage(Language.get("chestshop.create.destroy"));
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                event.setCancelled(true);
            } else {
                this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                    if (e.getSignLocation().equals(block.getLocation())) {
                        if (e.getOwner().equals(player.getName()) || player.isOp()) {
                            final ModalWindow deleteConfirmWindow = new ModalWindow.Builder("§7» §8ChestShop entfernen", "§cMöchtest du wirklich deinen ChestShop entfernen? Diese Aktion kann nicht rückgängig gemacht werden!",
                                    "§8» §aEntfernen", "§8» §cAbbrechen")
                                    .onYes(b -> {
                                        this.serverCore.getChestShopAPI().removeChestShop(player, e.getSignLocation(), e.getId());
                                        player.sendMessage(Language.get("chestshop.break.sign"));
                                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                                    })
                                    .onNo(b -> {
                                        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
                                    })
                                    .build();
                            deleteConfirmWindow.send(player);
                        }
                        event.setCancelled(true);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
            this.serverCore.getChestShopAPI().spawnChestShopDisplayItem(player, e, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        cachedBankAccounts.remove(player.getName());
    }

    private boolean chestIsUsed(final Block block) {
        final AtomicBoolean b = new AtomicBoolean(false);
        this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
            if (e.getChestLocation().equals(block.getLocation())) {
                b.set(true);
            }
        });
        return b.get();
    }

    private void requestBankLogin(final Player player, final BankAccount bankAccount, final String infoText, final Consumer<Boolean> accept) {
        final CustomWindow bankLoginWindow = new CustomWindow("§7» §8Bankkonto-Login benötigt");
        bankLoginWindow.form()
                .label("§8» §fKonto: §9" + bankAccount.getAccount() + "\n§8» §fName: §9" + bankAccount.getDisplayName())
                .label("§8» §f" + infoText)
                .input("§8» §7Bitte gebe das Passwort des Kontos an, um fortfahren zu können.", "Passwort");

        bankLoginWindow.onSubmit((g, h) -> {
            final String password = h.getInput(2);
            if (password.isEmpty()) accept.accept(false);
            else accept.accept(password.equals(bankAccount.getPassword()));
        });
        bankLoginWindow.send(player);
    }

}
