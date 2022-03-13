package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.QuestAPI;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.event.*;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record QuestListener(ServerCore serverCore) implements Listener {

    static final List<Block> placed = new ArrayList<>();

    /*
     * collect
     */
    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (!SpawnProtectionListener.isInRadius(event.getPlayer())) {
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
        }
    }

    /*
     * place
     */
    @EventHandler
    public void on(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (!SpawnProtectionListener.isInRadius(event.getPlayer())) {
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
        }
    }

    /*
     * explore
     */
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

    /*
     * craft
     */
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

    /*
     * execute
     */
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

    /*
     * fertilize
     */
    @EventHandler
    public void on(final BlockFertilizeEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("fertilize")) {
                this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
            }
        });
    }

    /*
     * enchant
     */
    /*@EventHandler
    public void on(final EnchantItemEvent event) {
        final Player player = event.getEnchanter();

    }*/

    /*
     * breed
     */
    @EventHandler
    public void on(final EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
            questPlayer.getQuestPlayerData().forEach(questData -> {
                if (questData.getData().startsWith("breed")) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            });
        }
    }

    /*
     * tame
     */
    @EventHandler
    public void on(final EntityTameEvent event) {
        final Player player = (Player) event.getOwner();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("tame")) {
                this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
            }
        });
    }

    /*
     * horsejump
     */
    @EventHandler
    public void on(final HorseJumpEvent event) {
        if (event.getEntity().getPassenger() instanceof Player player) {
            final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
            questPlayer.getQuestPlayerData().forEach(questData -> {
                if (questData.getData().startsWith("horsejump")) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            });
        }
    }

    /*
     * dyewool#<color|null>
     */
    @EventHandler
    public void on(final SheepDyeWoolEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("dyewool")) {
                if (questData.getData().split("#")[1].equals("null")) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                    return;
                }
                if (event.getColor() == DyeColor.valueOf(questData.getData().split("#")[1].toUpperCase())) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

    /*
     * bedleave
     */
    @EventHandler
    public void on(final PlayerBedLeaveEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("bedleave")) {
                this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
            }
        });
    }

    /*
     * eggthrow
     */
    @EventHandler
    public void on(final PlayerEggThrowEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("eggthrow")) {
                this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
            }
        });
    }

    /*
     * fish
     */
    @EventHandler
    public void on(final PlayerFishEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("fish")) {
                if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

    /*
     * harvest
     */
    @EventHandler
    public void on(final PlayerHarvestBlockEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("harvest")) {
                if (event.getHarvestedBlock().getType() == SyncAPI.ItemAPI.itemStackFromBase64(questData.getData().split("#")[1]).getType()) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

    /*
     * consume#item
     */
    @EventHandler
    public void on(final PlayerItemConsumeEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("consume")) {
                if (event.getItem().getType() == SyncAPI.ItemAPI.itemStackFromBase64(questData.getData().split("#")[1]).getType()) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

    /*
     * shear
     */
    @EventHandler
    public void on(final PlayerShearEntityEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("shear")) {
                if (event.getEntity().getType() == EntityType.SHEEP) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

    /*
     * raid
     */
    @EventHandler
    public void on(final RaidFinishEvent event) {
        final List<Player> players = event.getWinners();
        players.forEach(player -> {
            final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
            questPlayer.getQuestPlayerData().forEach(questData -> {
                if (questData.getData().startsWith("raid")) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            });
        });
    }

    /*
     * buy#item#<item>
     * buy#price
     */
    @EventHandler
    public void on(final PlayerBuyItemEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("buy")) {
                if (questData.getData().split("#")[1].equals("item")) {
                    if (event.getBoughtItem().getType() == SyncAPI.ItemAPI.itemStackFromBase64(questData.getData().split("#")[2]).getType()) {
                        this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), event.getBoughtItem().getAmount());
                    }
                } else if (questData.getData().split("#")[1].equals("price")) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), Math.toIntExact(Math.round(event.getPrice())));
                }
            }
        });
    }

    /*
     * claimdaily
     */
    @EventHandler
    public void on(final PlayerClaimDailyRewardEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("claimdaily")) {
                this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
            }
        });
    }

    /*
     * crateopen#<crate>
     */
    @EventHandler
    public void on(final PlayerCrateOpenEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("crateopen")) {
                if (questData.getData().split("#")[1].equalsIgnoreCase(event.getCrate())) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
                }
            }
        });
    }

    /*
     * xpadd
     */
    @EventHandler
    public void on(final PlayerExperienceAddEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("xpadd")) {
                this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), Math.toIntExact(Math.round(event.getExperience())));
            }
        });
    }

    /*
     * sell#item#<item>
     * sell#price
     */
    @EventHandler
    public void on(final PlayerSellItemEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("sell")) {
                if (questData.getData().split("#")[1].equals("item")) {
                    if (event.getSoldItem().getType() == SyncAPI.ItemAPI.itemStackFromBase64(questData.getData().split("#")[2]).getType()) {
                        this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), event.getSoldItem().getAmount());
                    }
                } else if (questData.getData().split("#")[1].equals("price")) {
                    this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), Math.toIntExact(Math.round(event.getPrice())));
                }
            }
        });
    }

    /*
     * vote
     */
    @EventHandler
    public void on(final PlayerVoteEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = QuestAPI.cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(questData -> {
            if (questData.getData().startsWith("vote")) {
                this.serverCore.getQuestAPI().addQuestProgress(player, questData.getQuestNameId(), questData.getQuestSubId(), 1);
            }
        });
    }

}
