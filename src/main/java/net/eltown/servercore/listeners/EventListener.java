package net.eltown.servercore.listeners;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.eltown.economy.Economy;
import net.eltown.economy.components.economy.event.MoneyChangeEvent;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.commands.administrative.NpcCommand;
import net.eltown.servercore.components.api.intern.GroupAPI;
import net.eltown.servercore.components.api.intern.LevelAPI;
import net.eltown.servercore.components.api.intern.QuestAPI;
import net.eltown.servercore.components.api.intern.SettingsAPI;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.data.settings.AccountSettings;
import net.eltown.servercore.components.data.settings.SettingsCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.*;

import java.util.*;

public record EventListener(ServerCore serverCore) implements Listener {

    public static Set<String> needsIntroduction = new HashSet<>();
    public static Set<String> inIntroduction = new HashSet<>();

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        event.joinMessage(Component.text(""));
        final Player player = event.getPlayer();

        this.serverCore.getSyncAPI().loadPlayer(player, (loaded) -> {
            if (loaded) {
                /*
                 * Teleportation
                 */
                this.serverCore.getTeleportationAPI().handleCachedData(player);

                /*
                 * Groups
                 */
                this.serverCore.getTinyRabbit().sendAndReceive((delivery -> {
                    if (GroupCalls.valueOf(delivery.getKey().toUpperCase()) == GroupCalls.CALLBACK_FULL_GROUP_PLAYER) {
                        final String prefix = delivery.getData()[3];
                        final String[] permissions = delivery.getData()[4].split("#");
                        final String[] aPermissions = delivery.getData()[6].split("#");

                        GroupAPI.attachments.remove(player.getName());
                        GroupAPI.attachments.put(player.getName(), player.addAttachment(this.serverCore));
                        final PermissionAttachment attachment = GroupAPI.attachments.get(player.getName());

                        for (final String p : permissions) {
                            attachment.setPermission(p, true);
                        }

                        for (final String p : aPermissions) {
                            if (!attachment.getPermissions().containsKey(p)) attachment.setPermission(p, true);
                        }

                        //set nametag
                    }
                }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP_PLAYER.name(), player.getName());

                /*
                 * Level
                 */
                this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                    if (LevelCalls.valueOf(delivery.getKey().toUpperCase()) == LevelCalls.CALLBACK_LEVEL) {
                        LevelAPI.cachedData.put(player.getName(), new Level(
                                delivery.getData()[1],
                                Integer.parseInt(delivery.getData()[2]),
                                Double.parseDouble(delivery.getData()[3])
                        ));
                        //player.setScoreTag("§gLevel §l" + Integer.parseInt(delivery.getData()[2]));
                    }
                }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_GET_LEVEL.name(), player.getName());

                /*
                 * Scoreboard
                 */
                Economy.getAPI().getMoney(player.getName(), money -> Bukkit.getScheduler().runTask(this.serverCore, () -> {
                    final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                    final Objective boardObjective = scoreboard.registerNewObjective("eltown", "dummy", Component.text("   §2§lEltown.net  "));
                    boardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

                    final Team economyTeam = scoreboard.registerNewTeam("economy");
                    economyTeam.addEntry("§3");
                    economyTeam.setPrefix("   §f$" + Economy.getAPI().getMoneyFormat().format(money));

                    final Team levelTeam = scoreboard.registerNewTeam("level");
                    levelTeam.addEntry("§6");
                    levelTeam.setPrefix("   §f" + this.serverCore.getLevelAPI().getLevel(player.getName()).getLevel() + " §8[" + this.serverCore.getLevelAPI().getLevelDisplay(player) + "§8]  ");

                    boardObjective.getScore("§1").setScore(0);
                    boardObjective.getScore(" §8» §0Bargeld").setScore(-1);
                    boardObjective.getScore("§3").setScore(-2);
                    boardObjective.getScore("§4").setScore(-3);
                    boardObjective.getScore(" §8» §0Level").setScore(-4);
                    boardObjective.getScore("§6").setScore(-5);
                    boardObjective.getScore("§7").setScore(-6);

                    player.setScoreboard(scoreboard);
                }));

                /*
                 * Settings
                 */
                this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                    if (SettingsCalls.valueOf(delivery.getKey().toUpperCase()) == SettingsCalls.CALLBACK_SETTINGS) {
                        if (!delivery.getData()[1].equals("null")) {
                            final Map<String, String> map = new HashMap<>();
                            final List<String> list = Arrays.asList(delivery.getData()[1].split(">:<"));
                            list.forEach(e -> {
                                map.put(e.split(":")[0], e.split(":")[1]);
                            });
                            SettingsAPI.cachedSettings.put(player.getName(), new AccountSettings(player.getName(), map));
                        } else SettingsAPI.cachedSettings.put(player.getName(), new AccountSettings(player.getName(), new HashMap<>()));
                    }
                }, Queue.SETTINGS_CALLBACK, SettingsCalls.REQUEST_SETTINGS.name(), player.getName());

                /*
                 * Friends
                 */
                this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIEND_DATA.name(), player.getName());

                /*
                 * Quests
                 */
                this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                    final String[] d = delivery.getData();
                    try {
                        switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                            case CALLBACK_PLAYER_DATA:
                                final List<QuestPlayer.QuestData> questPlayerData = new ArrayList<>();

                                if (!d[1].equals("null")) {
                                    for (String s : d[1].split("-#-")) {
                                        final String[] sSplit = s.split("-:-");
                                        questPlayerData.add(new QuestPlayer.QuestData(sSplit[0], sSplit[1], sSplit[2], Integer.parseInt(sSplit[3]), Integer.parseInt(sSplit[4]), Long.parseLong(sSplit[5])));
                                    }
                                }
                                QuestAPI.cachedQuestPlayer.put(player.getName(), new QuestPlayer(player.getName(), questPlayerData));
                                this.serverCore.getQuestAPI().checkIfQuestIsExpired(player.getName());
                                System.out.println("player data loaded");
                                break;
                            case CALLBACK_NULL:
                                QuestAPI.cachedQuestPlayer.put(player.getName(), new QuestPlayer(player.getName(), new ArrayList<>()));
                                break;
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_PLAYER_DATA.name(), player.getName());
            }
        });
    }

    @EventHandler
    public void on(final MoneyChangeEvent event) {
        final Player player = event.getPlayer();
        Objects.requireNonNull(player.getScoreboard().getTeam("economy")).setPrefix("   §f$" + Economy.getAPI().getMoneyFormat().format(event.getMoney()));
    }

    @EventHandler
    public void on(final PlayerQuitEvent event) {
        event.quitMessage(Component.text(""));
        if (this.serverCore.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) {
            this.serverCore.getSyncAPI().savePlayer(event.getPlayer());

            this.serverCore.getQuestAPI().checkIfQuestIsExpired(event.getPlayer().getName());
            this.serverCore.getQuestAPI().updateQuestPlayerData(event.getPlayer().getName());
        }
    }

    @EventHandler
    public void on(final VillagerCareerChangeEvent event) {
        final Villager villager = event.getEntity();
        if (villager.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void on(final EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();
        if (entity.getType() == EntityType.VILLAGER) {
            final Villager villager = (Villager) entity;
            if (villager.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING)) {
                event.setCancelled(true);

                if (event.getDamager() instanceof final Player player) {
                    if (player.isOp()) {
                        if (player.getInventory().getItemInMainHand().getType() == Material.BARRIER) {
                            villager.setHealth(0);
                            player.sendMessage("§8» §fCore §8| §7Der NPC wurde entfernt.");
                        } else if (player.getInventory().getItemInMainHand().getType() == Material.WOODEN_AXE) {
                            NpcCommand.selectedVillager.put(player.getName(), villager);
                            player.sendMessage("§8» §fCore §8| §7Der NPC wurde zur Bearbeitung ausgewählt.");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(final EntityMoveEvent event) {
        final Entity entity = event.getEntity();
        if (event.getEntityType() == EntityType.VILLAGER) {
            final Villager villager = (Villager) entity;
            if (villager.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING)) {
                if (event.getTo().getX() != event.getFrom().getX() || event.getTo().getY() != event.getFrom().getY() || event.getTo().getZ() != event.getFrom().getZ()) {
                    event.setCancelled(true);
                }
            }
        }
    }

}
