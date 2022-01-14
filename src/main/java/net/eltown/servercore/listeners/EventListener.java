package net.eltown.servercore.listeners;

import net.eltown.economy.Economy;
import net.eltown.economy.components.economy.event.MoneyChangeEvent;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.GroupAPI;
import net.eltown.servercore.components.api.intern.LevelAPI;
import net.eltown.servercore.components.api.intern.SettingsAPI;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.settings.AccountSettings;
import net.eltown.servercore.components.data.settings.SettingsCalls;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
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
        }
    }

}
