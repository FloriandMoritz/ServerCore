package net.eltown.servercore.commands.administrative;

import lombok.SneakyThrows;
import net.eltown.servercore.ServerCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ImportChestCommand extends Command implements Listener {

    private final ServerCore serverCore;
    private final Jedis redis;
    private HashMap<String, String> map = new HashMap<>();

    public static Map<String, String> translationKeys = new HashMap<>();
    public static YamlConfiguration translationConfig = new YamlConfiguration();
    public static File translationFile;

    @SneakyThrows
    public ImportChestCommand(final ServerCore serverCore) {
        super("importchest");
        this.setPermission("core.command.importchest");
        this.serverCore = serverCore;
        this.redis = new Jedis();

        translationFile = new File(this.serverCore.getDataFolder(), "translationKeys.yml");
        translationConfig.load(translationFile);
        for (final String translation : translationConfig.getString("translationKeys").split(Pattern.quote("[T]"))) {
            final String[] data = translation.split("->");
            translationKeys.put(data[0], data[1]);
        }
        serverCore.getServer().getPluginManager().registerEvents(this, serverCore);
    }

    @SneakyThrows
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            if (args.length == 1) {
                final String id = args[0];
                if (this.redis.exists("exportChest/" + id)) {
                    this.map.put(player.getName(), id);
                    player.sendMessage("Bitte Kiste anklicken.");
                } else player.sendMessage("ID existiert nicht.");
            } else if (args.length == 2) {
                translationKeys.put(args[0], args[1]);

                List<String> toSave = new ArrayList<>();
                for (Map.Entry<String, String> keys : translationKeys.entrySet()) {
                    toSave.add(keys.getKey() + "->" + keys.getValue());
                }

                translationConfig.set("translationKeys", String.join("[T]", toSave));
                translationConfig.save(translationFile);

                player.sendMessage("Added Translationkey " + args[0] + "->" + args[1]);
            }
        }
        return false;
    }

    @EventHandler
    public void on(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;
        final Block block = event.getClickedBlock();

        if (block.getState() instanceof Chest) {
            if (this.map.containsKey(player.getName())) {
                event.setCancelled(true);
                final String data = this.redis.get("exportChest/" + this.map.get(player.getName()));
                this.map.remove(player.getName());

                final BlockState blockState = block.getState();
                final Chest chest = (Chest) blockState;

                blockState.update();
                for (final String eItem : data.split(Pattern.quote("[B]"))) {
                    final String[] iData = eItem.split(Pattern.quote("[-]"));

                    final int index = Integer.parseInt(iData[0]);
                    String mat = iData[1].toUpperCase();
                    final int count = Integer.parseInt(iData[2]);
                    final int damage = Integer.parseInt(iData[3]);
                    final String customName = iData[4];
                    final List<String> lore = List.of(iData[5].split(Pattern.quote("[,]")));
                    final boolean enchantments = iData[6].equalsIgnoreCase("y");

                    if (translationKeys.containsKey(mat + ":" + damage)) mat = translationKeys.get(mat + ":" + damage);

                    try {
                        final ItemStack itemStack = new ItemStack(Material.valueOf(mat));
                        itemStack.setAmount(count);
                        final ItemMeta meta = itemStack.getItemMeta();
                        if (!customName.equals("n") && !customName.isEmpty() && customName != null) meta.displayName(Component.text(customName));
                        if (!iData[5].isEmpty() && !lore.isEmpty() && lore != null) meta.setLore(lore);
                        itemStack.setItemMeta(meta);
                        chest.getInventory().setItem(index, itemStack);


                    } catch (Exception ex) {
                        ex.printStackTrace();
                        player.sendMessage(ex.getMessage());
                        player.sendMessage("Ein Fehler ist aufgetreten! Item: " + mat + ":" + damage);
                    }
                }

                blockState.setBlockData(chest.getBlockData());
                player.sendMessage("Importiert.");
            }
        }
    }
}
