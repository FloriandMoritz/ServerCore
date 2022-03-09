package net.eltown.servercore.listeners;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.commands.administrative.NpcCommand;
import net.eltown.servercore.commands.defaults.SpawnCommand;
import net.eltown.servercore.components.api.intern.GroupAPI;
import net.eltown.servercore.components.api.intern.LevelAPI;
import net.eltown.servercore.components.api.intern.QuestAPI;
import net.eltown.servercore.components.api.intern.SettingsAPI;
import net.eltown.servercore.components.data.crates.CratesCalls;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.data.settings.AccountSettings;
import net.eltown.servercore.components.data.settings.SettingsCalls;
import net.eltown.servercore.components.event.MoneyChangeEvent;
import net.eltown.servercore.components.tinyrabbit.Queue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public record EventListener(ServerCore serverCore) implements Listener {

    public static Set<String> needsIntroduction = new HashSet<>();
    public static Set<String> inIntroduction = new HashSet<>();

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        event.joinMessage(Component.text(""));
        final Player player = event.getPlayer();
        player.setCustomNameVisible(false);

        this.serverCore.getSyncAPI().loadPlayer(player, (loaded) -> {
            if (loaded) {
                /*
                 * Economy
                 */
                this.serverCore.getEconomyAPI().hasAccount(player.getName(), has -> {
                    if (!has) this.serverCore.getEconomyAPI().createAccount(player.getName());
                });

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
                this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> Bukkit.getScheduler().runTask(this.serverCore, () -> {
                    final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                    final Objective boardObjective = scoreboard.registerNewObjective("eltown", "dummy", Component.text("   §2§lEltown.net  "));
                    boardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

                    final Team economyTeam = scoreboard.registerNewTeam("economy");
                    economyTeam.addEntry("§3");
                    economyTeam.setPrefix("   §f$" + this.serverCore.getMoneyFormat().format(money));

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
                 * Crates
                 */
                this.serverCore.getTinyRabbit().send(Queue.CRATES_RECEIVE, CratesCalls.REQUEST_CREATE_PLAYER_DATA.name(), player.getName());

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
                                break;
                            case CALLBACK_NULL:
                                QuestAPI.cachedQuestPlayer.put(player.getName(), new QuestPlayer(player.getName(), new ArrayList<>()));
                                break;
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_PLAYER_DATA.name(), player.getName());

                if (!player.hasPlayedBefore() && this.serverCore.getServerName().equals("server-1")) {
                    player.teleport(SpawnCommand.spawnLocation);
                    this.serverCore.getServer().getScheduler().runTaskLater(this.serverCore, () -> {
                        this.openWelcomeWindow(player);
                        this.serverCore.getEconomyAPI().addMoney(player.getName(), 50);
                        player.getInventory().addItem(
                                new ItemStack(Material.WOODEN_SWORD, 1),
                                new ItemStack(Material.WOODEN_AXE, 1),
                                new ItemStack(Material.WOODEN_PICKAXE, 1),
                                new ItemStack(Material.BAKED_POTATO, 32),
                                new ItemStack(Material.CHAINMAIL_CHESTPLATE, 1),
                                new ItemStack(Material.CHAINMAIL_LEGGINGS, 1)
                        );
                    }, 100);
                }
            }
        });
    }

    private void openWelcomeWindow(final Player player) {
        final String content =
                """
                        §2§lHerzlich Willkommen auf §0§lEltown.net§2§l!§r
                                                        
                        §8» §fEin paar wichtige Informationen, die du wissen solltest:
                                                        
                        §7Auf §0Eltown §7gibt es §06 Marktstände§7, an denen Items §0ge- und verkauft §7werden können. Die Preise der einzelnen Märkte werden durch die Anzahl der ge- oder verkauften Items beeinflusst.
                        §7Auch du kannst mit Items §0handeln§7! Dafür stehen dir die §0ChestShops §7zur Verfügung. Diese sind allerdings erst ab §0Level 2 §7freigeschaltet und du benötigst ein §0Bankkonto§7. Natürlich kann man auch ohne ChestShops handeln, aber dir könnten Betrüger über den Weg laufen. Also sei auf der Hut!
                                                        
                        §7In der Bank kannst du ein §0Bankkonto §7erstellen, um beispielsweise die ChestShops nutzen zu können oder um darin einkaufen zu können. Bankkonten bieten sich aber auch gut an, um mit §0Freunden §7oder §0Gemeinschaften §7Geld zu teilen. Geld kann mit der jeweiligen §0Bankkarte §7an den §0Geldautomaten §7ein- oder ausgezahlt werden.
                                                        
                        §7Im Rathaus kannst du §0Termine mit Mitarbeitern §7vereinbaren, um zum Beispiel eine neue §0ChestShop-Lizenz §7zu erwerben. Demnächst wird das Rathaus noch weiter in den Mittelpunkt rücken.
                                                        
                        §7Auf unserem §0Discord-Server §7kannst du bei §0Problemen oder Fragen §7auch ganz einfach ein Ticket öffnen, damit sich ein Teammitglied um dich kümmert. Falls du keinen Discord-Account hast, dann kannst du auch hier im Spiel ein Ticket öffnen mit §0/ticket §7oder mit §0/support§7.
                                                        
                        §fÜber kommende §0Updates §fwirst du auf unserem §0Discord-Server §finformiert. Du kannst diesen unter §9https://discord.eltown.net §fbeitreten.
                                                        
                                                        
                        §8» §fDas war's auch schon! Du erhälst §950$ Startgeld und ein paar nützliche Items§f. Viel Spaß beim Erkunden und Spielen!
                        """;
        player.sendMessage(content);
        player.sendMessage("§r");
        player.sendMessage("§r");
        player.sendMessage("§r");
        player.sendMessage("§8» §fCore §8| §7Unseren Discord-Server findest du unter §9https://discord.eltown.net§7.");
        player.sendMessage("§8» §fCore §8| §2Bei Fragen, melde dich einfach im Chat. Viel Spaß beim Erkunden und Spielen!");
    }

    @EventHandler
    public void on(final MoneyChangeEvent event) {
        final Player player = event.getPlayer();
        Objects.requireNonNull(player.getScoreboard().getTeam("economy")).setPrefix("   §f$" + this.serverCore.getMoneyFormat().format(event.getMoney()));
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
        } else if (entity.getType() == EntityType.ARMOR_STAND) {
            final ArmorStand armorStand = (ArmorStand) entity;
            if (armorStand.getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "fnpc.key"), PersistentDataType.STRING)) {
                if (event.getDamager() instanceof final Player player) {
                    if (player.isOp() && player.getInventory().getItemInMainHand().getType() == Material.BARRIER) {
                        armorStand.remove();
                        player.sendMessage("§8» §fCore §8| §7Der FNPC wurde entfernt.");
                    } else event.setCancelled(true);
                } else event.setCancelled(true);
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

    @EventHandler
    public void on(final PlayerCommandSendEvent event) {
        final List<String> list = new ArrayList<>(event.getCommands());

        for (final String command : list) {
            if (command.contains(":") || command.contains("/") || command.equalsIgnoreCase("ver") || command.toLowerCase().contains("version") || command.toLowerCase().contains("icanhasbukkit")
                    || command.equalsIgnoreCase("me") || command.equalsIgnoreCase("teammsg")) {
                event.getCommands().remove(command);
            }
        }
    }

    @EventHandler
    public void on(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final String message = event.getMessage();
        if (!player.isOp()) {
            if (message.toLowerCase().contains("bukkit") || message.toLowerCase().contains("spigot") || message.toLowerCase().contains("minecraft") || message.toLowerCase().contains("paper") ||
                    message.equalsIgnoreCase("ver") || message.toLowerCase().contains("version") || message.equalsIgnoreCase("me") || message.equalsIgnoreCase("tell") || message.equalsIgnoreCase("teammsg")) {
                player.sendMessage("§8» §fCore §8| §7Dieser Befehl existiert nicht.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(final PlayerArmorStandManipulateEvent event) {
        if (event.getRightClicked().getPersistentDataContainer().has(new NamespacedKey(ServerCore.getServerCore(), "container.hologram"), PersistentDataType.INTEGER) ||
                event.getRightClicked().getPersistentDataContainer().has(new NamespacedKey(ServerCore.getServerCore(), "fnpc.key"), PersistentDataType.INTEGER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void on(final CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.WITHER && this.serverCore.getServerName().equals("server-1")) {
            event.setCancelled(true);
        }
    }

}
