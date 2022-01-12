package net.eltown.servercore.components.api.intern;

import lombok.SneakyThrows;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.chestshop.ChestShop;
import net.eltown.servercore.components.data.chestshop.ChestshopCalls;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.tinyrabbit.Queue;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ChestShopAPI {

    private final ServerCore serverCore;

    public final HashMap<Location, ChestShop> cachedChestShops = new HashMap<>();
    public final HashMap<Long, Integer> cachedDisplays = new HashMap<>();
    private final HashMap<String, ShopLicense> cachedLicenses = new HashMap<>();

    public ChestShopAPI(final ServerCore serverCore) {
        this.serverCore = serverCore;

        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            if (ChestshopCalls.valueOf(delivery.getKey().toUpperCase()) == ChestshopCalls.CALLBACK_LOAD_DATA) {
                if (!d[1].equals("null")) {
                    final List<String> chestShops = Arrays.asList(d[1].split("-;-"));
                    chestShops.forEach(e -> {
                        final String[] c = e.split("#");
                        final Location sign = new Location(serverCore.getServer().getWorld(c[7]), Double.parseDouble(c[1]), Double.parseDouble(c[2]), Double.parseDouble(c[3]));
                        final Location chest = new Location(serverCore.getServer().getWorld(c[7]), Double.parseDouble(c[4]), Double.parseDouble(c[5]), Double.parseDouble(c[6]));

                        this.cachedChestShops.put(sign, new ChestShop(
                                sign,
                                chest,
                                Long.parseLong(c[0]),
                                c[8],
                                ChestShop.ShopType.valueOf(c[9].toUpperCase()),
                                Double.parseDouble(c[12]),
                                Integer.parseInt(c[10]),
                                SyncAPI.ItemAPI.itemStackFromBase64(c[11]),
                                c[13]
                        ));
                    });
                }

                final List<String> licenses = Arrays.asList(d[2].split("-;-"));
                licenses.forEach(e -> {
                    final String[] c = e.split("#");
                    this.cachedLicenses.put(c[0], new ShopLicense(c[0], ShopLicense.ShopLicenseType.valueOf(c[1].toUpperCase()), Integer.parseInt(c[2])));
                });
            }
        }, Queue.CHESTSHOP_CALLBACK, ChestshopCalls.REQUEST_LOAD_DATA.name());

        final List<ChestShop> toRemove = new ArrayList<>();
        this.cachedChestShops.values().forEach(e -> {
            if (!this.isWallSign(e.getSignLocation().getBlock().getType()) || e.getChestLocation().getBlock().getType() != Material.CHEST) {
                toRemove.add(e);
            }
        });
        toRemove.forEach(e -> this.removeChestShop(e.getSignLocation(), e.getId()));
    }

    public void createChestShop(final Location signLocation, final Location chestLocation, final Player owner, final ChestShop.ShopType shopType, final double price, final int sellAmount, final ItemStack item, final String bankAccount) {
        final long id = 1095216660480L + ThreadLocalRandom.current().nextLong(0L, 2147483647L);
        this.cachedChestShops.put(signLocation, new ChestShop(signLocation, chestLocation, id, owner.getName(), shopType, price, sellAmount, item, bankAccount));

        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_CREATE_CHESTSHOP.name(),
                String.valueOf(id),
                String.valueOf(signLocation.getX()),
                String.valueOf(signLocation.getY()),
                String.valueOf(signLocation.getZ()),
                String.valueOf(chestLocation.getX()),
                String.valueOf(chestLocation.getY()),
                String.valueOf(chestLocation.getZ()),
                signLocation.getWorld().getName(),
                owner.getName(),
                shopType.name().toUpperCase(),
                String.valueOf(sellAmount),
                SyncAPI.ItemAPI.itemStackToBase64(item),
                String.valueOf(price),
                bankAccount
        );

        final ItemStack itemDisplay = item.clone();
        itemDisplay.setAmount(1);
        this.spawnChestShopDisplayItem(owner, this.cachedChestShops.get(signLocation), true);
    }

    public void updateAmount(final ChestShop chestShop, final int update) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setShopCount(update);

        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_UPDATE_AMOUNT.name(), String.valueOf(chestShop.getId()), String.valueOf(update));

        final Sign sign = (Sign) chestShop.getSignLocation().getBlock().getState();
        if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
            sign.line(0, Component.text("§a[§2ChestShop§a]"));
            sign.line(1, Component.text("§0Kaufe: §2" + update + "x"));
            sign.line(2, Component.text("§f$" + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice())));
            sign.line(3, Component.text("§2" + chestShop.getOwner()));
        } else {
            sign.line(0, Component.text("§a[§2ChestShop§a]"));
            sign.line(1, Component.text("§0Verkaufe: §2" + update + "x"));
            sign.line(2, Component.text("§f$" + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice())));
            sign.line(3, Component.text("§2" + chestShop.getOwner()));
        }
        sign.update(true);
    }

    public void updatePrice(final ChestShop chestShop, final double price) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setShopPrice(price);

        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_UPDATE_PRICE.name(), String.valueOf(chestShop.getId()), String.valueOf(price));

        final Sign sign = (Sign) chestShop.getSignLocation().getBlock().getState();
        if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
            sign.line(0, Component.text("§a[§2ChestShop§a]"));
            sign.line(1, Component.text("§0Kaufe: §2" + chestShop.getShopCount() + "x"));
            sign.line(2, Component.text("§f$" + Economy.getAPI().getMoneyFormat().format(price)));
            sign.line(3, Component.text("§2" + chestShop.getOwner()));
        } else {
            sign.line(0, Component.text("§a[§2ChestShop§a]"));
            sign.line(1, Component.text("§0Verkaufe: §2" + chestShop.getShopCount() + "x"));
            sign.line(2, Component.text("§f$" + Economy.getAPI().getMoneyFormat().format(price)));
            sign.line(3, Component.text("§2" + chestShop.getOwner()));
        }
        sign.update(true);
    }

    public void updateItem(final Player player, final ChestShop chestShop, final String item) {
        final ItemStack itemStack = SyncAPI.ItemAPI.itemStackFromBase64(item);
        this.cachedChestShops.get(chestShop.getSignLocation()).setItem(itemStack);

        this.sendPacket(player, new PacketPlayOutEntityDestroy(this.cachedDisplays.get(chestShop.getId())), true);
        this.spawnChestShopDisplayItem(player, chestShop, true);

        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_UPDATE_ITEM.name(), String.valueOf(chestShop.getId()), item);
    }

    public void removeChestShop(final Player player, final Location signLocation, final long id) {
        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_REMOVE_SHOP.name(), String.valueOf(id));

        this.sendPacket(player, new PacketPlayOutEntityDestroy(this.cachedDisplays.get(id)), true);

        this.cachedChestShops.remove(signLocation);
    }

    @Deprecated
    private void removeChestShop(final Location signLocation, final long id) {
        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_REMOVE_SHOP.name(), String.valueOf(id));

        this.cachedChestShops.remove(signLocation);
    }

    @SneakyThrows
    public void spawnChestShopDisplayItem(final Player player, final ChestShop chestShop, final boolean spawnToAll) {
        this.cachedDisplays.remove(chestShop.getId());
        final Method asNMSCopy = net.minecraft.world.item.ItemStack.class.getMethod("fromBukkitCopy", ItemStack.class);
        if (asNMSCopy != null) {
            final ItemStack displayItem = chestShop.getItem().clone();
            displayItem.setAmount(1);
            final net.minecraft.world.item.ItemStack nmsStack = (net.minecraft.world.item.ItemStack) asNMSCopy.invoke(asNMSCopy.getClass(), displayItem);
            if (nmsStack != null) {
                final Object object = CraftWorld.class.cast(chestShop.getChestLocation().getWorld());
                if (object != null) {
                    final Method getHandle = object.getClass().getMethod("getHandle");
                    if (getHandle != null) {
                        final EntityItem entityItem = new EntityItem((World) getHandle.invoke(object), chestShop.getChestLocation().getX() + 0.5, chestShop.getChestLocation().getY() + 1, chestShop.getChestLocation().getZ() + 0.5, nmsStack);
                        final int id = entityItem.ae();
                        this.cachedDisplays.put(chestShop.getId(), id);
                        entityItem.m(true);
                        entityItem.g(-1);
                        entityItem.e(true);
                        entityItem.persist = true;
                        entityItem.g(new Vec3D(0.0d, 0.0d, 0.0d));
                        entityItem.a(32767);
                        entityItem.j(2147483647);
                        this.sendPacket(player, new PacketPlayOutEntityDestroy(id), spawnToAll);
                        this.sendPacket(player, new PacketPlayOutSpawnEntity(entityItem), spawnToAll);
                        this.sendPacket(player, new PacketPlayOutEntityVelocity(entityItem), spawnToAll);
                        this.sendPacket(player, new PacketPlayOutEntityMetadata(id, entityItem.ai(), true), spawnToAll);
                    }
                }
            }
        }
    }

    private void sendPacket(final Player player, final Packet packet, final boolean sendAll) {
        if (player != null) {
            if (sendAll) {
                this.serverCore.getServer().getOnlinePlayers().forEach(e -> {
                    final ServerPlayerConnection connection = this.getServerPlayerConnection(e);
                    if (connection != null) {
                        connection.a(packet);
                    }
                });
            } else {
                final ServerPlayerConnection connection = this.getServerPlayerConnection(player);
                if (connection != null) {
                    connection.a(packet);
                }
            }
        }
    }

    @SneakyThrows
    private ServerPlayerConnection getServerPlayerConnection(final Player player) {
        final Object object = CraftPlayer.class.cast(player);
        if (object != null) {
            final Method method = object.getClass().getMethod("getHandle");
            if (method != null) {
                final Object object1 = method.invoke(object);
                if (object1 != null) {
                    final Field field = object1.getClass().getField("b");
                    if (field != null) {
                        return (ServerPlayerConnection) field.get(object1);
                    }
                }
            }
        }
        return null;
    }

    public int countPlayerChestShops(final String player) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        this.cachedChestShops.values().forEach(e -> {
            if (e.getOwner().equals(player)) atomicInteger.addAndGet(1);
        });
        return atomicInteger.get();
    }

    public ShopLicense getPlayerLicense(final String player) {
        final ShopLicense shopLicense = this.cachedLicenses.get(player);
        if (shopLicense != null) {
            return shopLicense;
        } else return new ShopLicense(player, ShopLicense.ShopLicenseType.STANDARD, 0);
    }

    public void setLicense(final String player, final ShopLicense.ShopLicenseType licenseType) {
        this.cachedLicenses.get(player).setLicense(licenseType);
        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_SET_LICENSE.name(), player, licenseType.name().toUpperCase());
    }

    public void setAdditionalShops(final String player, final int additionalShops) {
        this.cachedLicenses.get(player).setAdditionalShops(additionalShops);
        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_SET_ADDITIONAL_SHOPS.name(), player, String.valueOf(additionalShops));
    }

    public void addAdditionalShops(final String player, final int additionalShops) {
        this.cachedLicenses.get(player).setAdditionalShops(this.cachedLicenses.get(player).getAdditionalShops() + additionalShops);
        this.serverCore.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_ADD_ADDITIONAL_SHOPS.name(), player, String.valueOf(additionalShops));
    }

    public boolean isWallSign(final Material material) {
        return material == Material.OAK_WALL_SIGN || material == Material.SPRUCE_WALL_SIGN || material == Material.BIRCH_WALL_SIGN || material == Material.ACACIA_WALL_SIGN
                || material == Material.JUNGLE_WALL_SIGN || material == Material.DARK_OAK_WALL_SIGN || material == Material.WARPED_WALL_SIGN || material == Material.CRIMSON_WALL_SIGN;
    }

    public boolean isSign(final Material material) {
        return material == Material.OAK_SIGN || material == Material.SPRUCE_SIGN || material == Material.BIRCH_SIGN || material == Material.ACACIA_SIGN
                || material == Material.JUNGLE_SIGN || material == Material.DARK_OAK_SIGN || material == Material.WARPED_SIGN || material == Material.CRIMSON_SIGN;
    }
}
