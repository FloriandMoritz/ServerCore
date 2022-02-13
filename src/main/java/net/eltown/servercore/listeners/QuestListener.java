package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.QuestAPI;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record QuestListener(ServerCore serverCore) implements Listener {

    static final List<Block> placed = new ArrayList<>();

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        //if (!SpawnProtectionListener.isInRadius(event.getPlayer())) {
        if (!this.serverCore.getServerName().equals("server-1")) {
            if (!placed.contains(block)) {
                final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
                questPlayer.getQuestPlayerData().forEach(questData -> {
                    if (questData.getData().startsWith("collect")) {
                        final ItemStack itemStack = SyncAPI.ItemAPI.itemStackFromBase64(questData.getData().split("#")[1]);
                        if (block.getType() == itemStack.getType()) {
                            this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                        }
                    }
                });
            }
        }
        //}
    }

    @EventHandler
    public void on(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        //if (!SpawnProtectionListener.isInRadius(event.getPlayer())) {
        if (!this.serverCore.getServerName().equals("server-1")) {
            placed.add(block);
        }

        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("place")) {
                final ItemStack itemStack = SyncAPI.ItemAPI.itemStackFromBase64(questData.getData().split("#")[1]);
                if (block.getType() == itemStack.getType()) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
        //}
    }

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        if (questPlayer == null) return;
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("explore")) {
                final String[] d = questData.getData().split("#");
                final String[] d1 = d[1].split(">");
                final String[] d2 = d[2].split(">");
                final Location pos1 = new Location(this.serverCore.getServer().getWorld(d1[3]), Double.parseDouble(d1[0]), Double.parseDouble(d1[1]), Double.parseDouble(d1[2]));
                final Location pos2 = new Location(this.serverCore.getServer().getWorld(d2[3]), Double.parseDouble(d2[0]), Double.parseDouble(d2[1]), Double.parseDouble(d2[2]));

                if (this.serverCore.isInArea(player.getLocation(), pos1, pos2)) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

    @EventHandler
    public void on(final CraftItemEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final ItemStack output = event.getRecipe().getResult();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("craft")) {
                final ItemStack item = SyncAPI.ItemAPI.itemStackFromBase64(questData.getData().split("#")[1]);
                if (output.getType() == item.getType()) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), output.getAmount());
                }
            }
        });
    }

    @EventHandler
    public void on(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final String command = event.getMessage().replace("/", "");
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("execute")) {
                if (command.equalsIgnoreCase(questData.getData().split("#")[1])) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

}
