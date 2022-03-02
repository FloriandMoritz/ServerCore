package net.eltown.servercore.components.api.intern;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.sync.SyncCalls;
import net.eltown.servercore.components.data.sync.SyncPlayer;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SyncAPI {

    private final ServerCore serverCore;
    @Getter
    private final ArrayList<String> loaded = new ArrayList<>();

    public void savePlayer(final Player player) {
        if (!this.loaded.contains(player.getName())) return;

        final String[] inventories = ItemAPI.playerInventoryToBase64(player.getInventory());
        final String enderchestInventory = ItemAPI.toBase64(player.getEnderChest());
        final int foodLevel = player.getFoodLevel();
        final float saturation = player.getSaturation();
        final float exhaustion = player.getExhaustion();
        final int selectedSlot = player.getInventory().getHeldItemSlot();
        final String potionEffects = ItemAPI.serializePotionEffects(ItemAPI.getPlayerPotionEffects(player));
        final int totalExperience = player.getTotalExperience();
        final int level = player.getLevel();
        final float experience = player.getExp();
        final String gamemode = player.getGameMode().toString();
        final boolean flying = false;

        this.serverCore.getTinyRabbit().send(Queue.SYNC_RECEIVE, SyncCalls.REQUEST_SETSYNC.name(), player.getName(),
                inventories[0],
                inventories[1],
                enderchestInventory,
                foodLevel + "",
                saturation + "",
                exhaustion + "",
                selectedSlot + "",
                potionEffects,
                totalExperience + "",
                level + "",
                experience + "",
                gamemode,
                flying + ""
        );
    }

    public void getPlayer(final Player player, final Consumer<SyncPlayer> callback) {
        this.serverCore.getTinyRabbit().sendAndReceive((delivery -> {
            switch (SyncCalls.valueOf(delivery.getKey())) {
                case GOT_NOSYNC -> this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> this.getPlayer(player, callback), 5);
                case GOT_SYNC -> {
                    this.serverCore.getTinyRabbit().send(Queue.SYNC_RECEIVE, SyncCalls.REQUEST_SETNOSYNC.name(), player.getName());
                    final ItemStack[] inventory = ItemAPI.itemStackArrayFromBase64(delivery.getData()[1]);
                    final ItemStack[] armorInventory = ItemAPI.itemStackArrayFromBase64(delivery.getData()[2]);
                    final ItemStack[] enderchest = ItemAPI.itemStackArrayFromBase64(delivery.getData()[3]);
                    final int foodLevel = Integer.parseInt(delivery.getData()[4]);
                    final float saturation = Float.parseFloat(delivery.getData()[5]);
                    final float exhaustion = Float.parseFloat(delivery.getData()[6]);
                    final int selectedSlot = Integer.parseInt(delivery.getData()[7]);
                    final PotionEffect[] potionEffects = ItemAPI.deserializePotionEffects(delivery.getData()[8]);
                    final int totalExperience = Integer.parseInt(delivery.getData()[9]);
                    final int level = Integer.parseInt(delivery.getData()[10]);
                    final float experience = Float.parseFloat(delivery.getData()[11]);
                    final GameMode gamemode = GameMode.valueOf(delivery.getData()[12]);
                    final boolean flying = false;
                    callback.accept(new SyncPlayer(inventory, armorInventory, enderchest, foodLevel, saturation, exhaustion, selectedSlot, potionEffects, totalExperience, level, experience, gamemode, flying));
                }
            }
        }), Queue.SYNC_CALLBACK, SyncCalls.REQUEST_SYNC.name(), player.getName());
    }

    public void savePlayerAsync(final Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                this.savePlayer(player);
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void loadPlayer(final Player player, final Consumer<Boolean> b) {
        this.loaded.remove(player.getName());

        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setExp(0);
        player.setLevel(0);

        player.sendMessage(Language.get("sync.data.loading"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        this.getPlayer(player, (syncPlayer -> {
            player.getInventory().setContents(syncPlayer.inventory());
            player.getInventory().setArmorContents(syncPlayer.armorInventory());
            player.getEnderChest().setContents(syncPlayer.enderchest());

            player.setFoodLevel(syncPlayer.foodLevel());
            player.setSaturation(syncPlayer.saturation());
            player.setExhaustion(syncPlayer.exhaustion());
            player.getInventory().setHeldItemSlot(syncPlayer.selectedSlot());

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }

            for (PotionEffect effect : syncPlayer.potionEffects()) {
                player.addPotionEffect(effect);
            }

            player.setTotalExperience(syncPlayer.totalExperience());
            player.setLevel(syncPlayer.level());
            player.setExp(syncPlayer.experience());
            player.setGameMode(syncPlayer.gameMode());
            //player.setFlying(syncPlayer.flying());

            loaded.add(player.getName());
            player.sendMessage(Language.get("sync.data.loaded"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            b.accept(true);
        }));
    }

    public static class ItemAPI {

        private static PotionEffect[] getPlayerPotionEffects(final Player player) {
            final PotionEffect[] potionEffects = new PotionEffect[player.getActivePotionEffects().size()];
            int arrayIndex = 0;
            for (final PotionEffect effect : player.getActivePotionEffects()) {
                potionEffects[arrayIndex] = effect;
                arrayIndex++;
            }
            return potionEffects;
        }

        public static String serializePotionEffects(final PotionEffect[] potionEffects) {
            // Return an empty string if there are no effects to serialize
            if (potionEffects.length == 0) {
                return "";
            }

            // Create an output stream that will be encoded into base 64
            final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

            try (final BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(byteOutputStream)) {
                // Define the length of the potion effect array to serialize
                bukkitOutputStream.writeInt(potionEffects.length);

                // Write each serialize each PotionEffect to the output stream
                for (final PotionEffect potionEffect : potionEffects) {
                    bukkitOutputStream.writeObject(serializePotionEffect(potionEffect));
                }

                // Return encoded data, using the encoder from SnakeYaml to get a ByteArray conversion
                return Base64Coder.encodeLines(byteOutputStream.toByteArray());
            } catch (final IOException e) {
                throw new IllegalArgumentException("Failed to serialize potion effect data");
            }
        }

        @SneakyThrows
        public static PotionEffect[] deserializePotionEffects(final String potionEffectData) {
            // Return empty array if there is no potion effect data (don't apply any effects to the player)
            if (potionEffectData.isEmpty()) {
                return new PotionEffect[0];
            }

            // Create a byte input stream to read the serialized data
            try (final ByteArrayInputStream byteInputStream = new ByteArrayInputStream(Base64Coder.decodeLines(potionEffectData))) {
                try (final BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(byteInputStream)) {
                    // Read the length of the Bukkit input stream and set the length of the array to this value
                    final PotionEffect[] potionEffects = new PotionEffect[bukkitInputStream.readInt()];

                    // Set the potion effects in the array from deserialized PotionEffect data
                    int potionIndex = 0;
                    for (final PotionEffect ignored : potionEffects) {
                        potionEffects[potionIndex] = deserializePotionEffect(bukkitInputStream.readObject());
                        potionIndex++;
                    }

                    // Return the finished, serialized potion effect array
                    return potionEffects;
                }
            }
        }

        private static Map<String, Object> serializePotionEffect(final PotionEffect potionEffect) {
            return potionEffect != null ? potionEffect.serialize() : null;
        }

        @SuppressWarnings("unchecked") // Ignore the "Unchecked cast" warning
        private static PotionEffect deserializePotionEffect(final Object serializedPotionEffect) {
            return serializedPotionEffect != null ? new PotionEffect((Map<String, Object>) serializedPotionEffect) : null;
        }

        @SneakyThrows
        public static String[] playerInventoryToBase64(final PlayerInventory playerInventory) {
            //get the main content part, this doesn't return the armor
            final String content = toBase64(playerInventory);
            final String armor = itemStackArrayToBase64(playerInventory.getArmorContents());

            return new String[]{content, armor};
        }

        @SneakyThrows
        public static String itemStackArrayToBase64(final ItemStack[] items) {
            try {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

                // Write the size of the inventory
                dataOutput.writeInt(items.length);

                // Save every element in the list
                for (int i = 0; i < items.length; i++) {
                    dataOutput.writeObject(items[i]);
                }

                // Serialize that array
                dataOutput.close();
                return Base64Coder.encodeLines(outputStream.toByteArray());
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to save item stacks.", e);
            }
        }

        @SneakyThrows
        public static String toBase64(final Inventory inventory) {
            try {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

                // Write the size of the inventory
                dataOutput.writeInt(inventory.getSize());

                // Save every element in the list
                for (int i = 0; i < inventory.getSize(); i++) {
                    dataOutput.writeObject(inventory.getItem(i));
                }

                // Serialize that array
                dataOutput.close();
                return Base64Coder.encodeLines(outputStream.toByteArray());
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to save item stacks.", e);
            }
        }

        @SneakyThrows
        public static Inventory fromBase64(final String data) {
            if (data.isEmpty()) {
                return Bukkit.createInventory(null, 27);
            }

            try {
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
                final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                final Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

                // Read the serialized inventory
                for (int i = 0; i < inventory.getSize(); i++) {
                    inventory.setItem(i, (ItemStack) dataInput.readObject());
                }

                dataInput.close();
                return inventory;
            } catch (final ClassNotFoundException e) {
                throw new IOException("Unable to decode class type.", e);
            }
        }

        @SneakyThrows
        public static ItemStack[] itemStackArrayFromBase64(final String data) {
            if (data.isEmpty()) {
                return new ItemStack[0];
            }

            try {
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
                final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                final ItemStack[] items = new ItemStack[dataInput.readInt()];

                // Read the serialized inventory
                for (int i = 0; i < items.length; i++) {
                    items[i] = (ItemStack) dataInput.readObject();
                }

                dataInput.close();
                return items;
            } catch (final ClassNotFoundException e) {
                throw new IOException("Unable to decode class type.", e);
            }
        }

        @SneakyThrows
        public static String itemStackToBase64(final ItemStack itemStack) {
            try {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

                // Save every element in the list
                dataOutput.writeObject(itemStack);

                // Serialize that array
                dataOutput.close();
                return Base64Coder.encodeLines(outputStream.toByteArray());
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to save item stack.", e);
            }
        }

        @SneakyThrows
        public static ItemStack itemStackFromBase64(final String data) {
            if (data.isEmpty()) {
                return new ItemStack(Material.AIR);
            }

            try {
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
                final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

                final ItemStack item = (ItemStack) dataInput.readObject();

                dataInput.close();
                return item;
            } catch (final ClassNotFoundException e) {
                throw new IOException("Unable to decode class type.", e);
            }
        }

    }

}
