package net.eltown.servercore.commands.feature;

import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SettingsAPI;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FriendCommand extends Command {

    private final ServerCore serverCore;

    public FriendCommand(final ServerCore serverCore) {
        super("friend", "Verwalte deine Freunde", "", List.of("f", "freunde"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.openMain(player);
        }
        return true;
    }

    private void openMain(final Player player) {
        this.serverCore.getFriendAPI().getFriendData(player.getName(), (friends, requests) -> {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8Freunde", "")
                    .addButton("§8» §9Deine Freunde §8[§f" + friends.size() + "§8]", "", e -> this.openFriends(player, friends))
                    .addButton("§8» §9Offene Anfragen §8[§f" + requests.size() + "§8]", "", e -> this.openRequests(player, requests))
                    .addButton("§8» §9Freund hinzufügen", "", e -> this.openAddFriend(player, friends, requests))
                    .addButton("§8» §9Einstellungen", "", this::openSettings)
                    .build();
            window.send(player);
        });
    }

    private void openAddFriend(final Player player, final List<String> friends, final List<String> requests) {
        this.serverCore.getCoreAPI().proxyGetOnlinePlayers(players -> {
            final List<String> selectPlayer = new ArrayList<>(Collections.singletonList("Spieler wählen!"));
            selectPlayer.addAll(players);
            selectPlayer.remove(player.getName());

            final CustomWindow addFriendWindow = new CustomWindow("§7» §8Freund hinzufügen");
            addFriendWindow.form()
                    .dropdown("§8» §fWähle einen Spieler, der grade online ist, aus, um diesem eine Freundschaftsanfrage zu senden.", selectPlayer.toArray(new String[0]))
                    .input("§8» §fAlternativ kannst du auch einen Spieler angeben, der aktuell nicht online ist.", "HanzFranz");

            addFriendWindow.onSubmit((g, h) -> {
                final String dropdown = selectPlayer.get(h.getDropdown(0));
                final String input = h.getInput(1);
                String target = "";

                if (!dropdown.equals("Spieler wählen!")) {
                    target = dropdown;
                } else {
                    if (!input.isEmpty()) {
                        target = input;
                    } else {
                        player.sendMessage(Language.get("friend.invalid.input"));
                        return;
                    }
                }

                if (target.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(Language.get("friend.invalid.input"));
                    return;
                }

                if (friends.contains(target)) {
                    player.sendMessage(Language.get("friend.already.friends", target));
                    return;
                }

                if (requests.contains(target)) {
                    this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIENDSHIP.name(), player.getName(), target);
                    this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), player.getName(), target);
                    this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), target, player.getName());
                    this.serverCore.getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), target, Language.get("friend.friendship.created", player.getName()));
                    player.sendMessage(Language.get("friend.friendship.created", target));
                    return;
                }

                final String finalTarget = target;
                this.serverCore.getFriendAPI().getFriendData(target, (targetFriends, targetRequests) -> {
                    if (targetFriends == null && targetRequests == null) {
                        player.sendMessage(Language.get("friend.invalid.player"));
                    } else {
                        this.serverCore.getSettingsAPI().getEntry(finalTarget, "friend/requests", "true", value -> {
                            if (value.equals("true")) {
                                this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIEND_REQUEST.name(), player.getName(), finalTarget);
                                player.sendMessage(Language.get("friend.request.created", finalTarget));
                                this.serverCore.getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), finalTarget, Language.get("friend.request.received", player.getName()));
                            } else {
                                player.sendMessage(Language.get("friend.settings.no.requests", finalTarget));
                            }
                        });
                    }
                });
            });
            addFriendWindow.send(player);
        });
    }

    private void openFriends(final Player player, final List<String> friends) {
        if (friends.size() == 0) {
            player.sendMessage(Language.get("friend.no.friends"));
            return;
        }

        final SimpleWindow.Builder friendsWindow = new SimpleWindow.Builder("§7» §8Freundesliste", "§8» §fHier sind deine Freunde aufgelistet. Klicke einen an, um diesen zu verwalten oder zu interagieren.");
        friends.forEach(e -> {
            this.serverCore.getCoreAPI().proxyPlayerIsOnline(e, is -> {
                if (is) {
                    friendsWindow.addButton("§8» §f" + e + " §8[§aOnline§8]\n§9Verwalten", g -> this.openManageFriend(player, e));
                } else {
                    friendsWindow.addButton("§8» §f" + e + " §8[§cOffline§8]\n§9Verwalten", g -> this.openManageFriend(player, e));
                }
            });
        });
        friendsWindow.addButton("§8» §cZurück", "", this::openMain);
        friendsWindow.build().send(player);
    }

    private void openFriends(final Player player) {
        this.serverCore.getFriendAPI().getFriendData(player.getName(), (friends, requests) -> {
            if (friends.size() == 0) {
                player.sendMessage(Language.get("friend.no.friends"));
                return;
            }

            final SimpleWindow.Builder friendsWindow = new SimpleWindow.Builder("§7» §8Freundesliste", "§8» §fHier sind deine Freunde aufgelistet. Klicke einen an, um diesen zu verwalten oder zu interagieren.");
            friends.forEach(e -> {
                this.serverCore.getCoreAPI().proxyPlayerIsOnline(e, is -> {
                    if (is) {
                        friendsWindow.addButton("§8» §f" + e + " §8[§aOnline§8]\n§9Verwalten", g -> this.openManageFriend(player, e));
                    } else {
                        friendsWindow.addButton("§8» §f" + e + " §8[§cOffline§8]\n§9Verwalten", g -> this.openManageFriend(player, e));
                    }
                });
            });
            friendsWindow.addButton("§8» §cZurück", "", this::openMain);
            friendsWindow.build().send(player);
        });
    }

    private void openManageFriend(final Player player, final String friend) {
        final StringBuilder content = new StringBuilder("§8» §fHier kannst du deinen Freund oder deine Freundin §9" + friend + " §fverwalten. Schau dir die Informationen an oder interagiere mit den folgenden Möglichkeiten.\n\n");

        Economy.getAPI().getMoney(friend, money -> {
            this.serverCore.getSettingsAPI().getEntry(friend, "friend/level", "true", value -> {
                if (value.equals("true")) {
                    content.append("§8» §9Level: §f").append(this.serverCore.getLevelAPI().getLevel(friend).getLevel()).append("\n");
                }
            });

            this.serverCore.getSettingsAPI().getEntry(friend, "friend/money", "true", value -> {
                if (value.equals("true")) {
                    content.append("§8» §9Bargeld: §f$").append(Economy.getAPI().getMoneyFormat().format(money)).append("\n");
                }
            });

            this.serverCore.getSettingsAPI().getEntry(friend, "friend/onlinetime", "true", value -> {
                if (value.equals("true")) {
                    this.serverCore.getTinyRabbit().sendAndReceive(delivery2 -> {
                        if (delivery2.getKey().equalsIgnoreCase("REQUEST_PLAYTIME")) {
                            final long day = Long.parseLong(delivery2.getData()[1]);
                            final long hours = day / 1000 / 60 / 60;
                            content.append("§8» §9Spielzeit: §f").append(hours).append(hours == 1 ? " Stunde" : " Stunden").append("\n");
                        }
                    }, Queue.PROXY_PLAYTIME, "REQUEST_PLAYTIME", friend);
                }
            });

            content.append("\n");

            final SimpleWindow.Builder manageWindow = new SimpleWindow.Builder("", content.toString());
            this.serverCore.getCoreAPI().proxyGetOnlinePlayers(players -> {
                if (players.contains(friend)) {
                    manageWindow.addButton("§8» §fTPA senden", "", e -> {
                        this.serverCore.getTeleportationAPI().sendTpa(player.getName(), friend, alreadySent -> {
                            if (alreadySent) {
                                player.sendMessage(Language.get("tpa.already.sent", friend));
                            } else {
                                player.sendMessage(Language.get("tpa.sent", friend));
                            }
                        });
                    });
                }
            });
            if (this.serverCore.getServerName().equals("server-1")) {

            }
            manageWindow.addButton("§8» §cFreund entfernen", g -> {
                this.serverCore.getFriendAPI().areFriends(player.getName(), friend, are -> {
                    if (are) {
                        final ModalWindow removeFriendWindow = new ModalWindow.Builder("§7» §8Freund entfernen", "§fMöchtest du die Freundschaft mit §9" + friend + " §fbeenden? Diese Aktion kann nicht rückgängig gemacht werden.",
                                "§8» §aEntfernen", "§8» §cAbbrechen")
                                .onYes(v -> {
                                    this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIENDSHIP.name(), player.getName(), friend);
                                    this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIENDSHIP.name(), friend, player.getName());
                                    this.serverCore.getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), friend, Language.get("friend.friendship.removed", player.getName()));
                                    player.sendMessage(Language.get("friend.friendship.removed", friend));
                                })
                                .onNo(v -> this.openManageFriend(player, friend))
                                .build();
                        removeFriendWindow.send(player);
                    } else {
                        player.sendMessage(Language.get("friend.no.friendship", friend));
                    }
                });
            });

            manageWindow.addButton("§8» §cZurück", "", this::openFriends);
            manageWindow.build().send(player);
        });
    }

    private void openRequests(final Player player, final List<String> requests) {
        if (requests.size() == 0) {
            player.sendMessage(Language.get("friend.no.requests"));
            return;
        }

        final SimpleWindow.Builder requestsWindow = new SimpleWindow.Builder("§7» §8Freundschaftsanfragen", "§8» §fKlicke eine Anfrage an, um diese anzunehmen oder abzulehnen.\n\n");
        requests.forEach(e -> {
            requestsWindow.addButton("§8» §f" + e + "\n§9Verwalten", g -> {
                final ModalWindow requestAcceptForm = new ModalWindow.Builder("§7» §8Freundschaftsanfrage verwalten", "§fMöchtest du die Freundschaftsanfrage von §9" + e + " §fannehmen oder ablehnen?\n\nWenn du diese annimmst, dann stehen dir in Verbindung mit diesem Spieler alle Freunde-Funktionen zur Verfügung.",
                        "§8» §aAnnehmen", "§8» §cAblehnen")
                        .onYes(v -> {
                            this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIENDSHIP.name(), player.getName(), e);
                            this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), player.getName(), e);
                            this.serverCore.getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), e, Language.get("friend.friendship.created", player.getName()));
                            player.sendMessage(Language.get("friend.friendship.created", e));
                        })
                        .onNo(v -> {
                            this.serverCore.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), player.getName(), e);
                            this.serverCore.getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), e, Language.get("friend.request.denied.other", player.getName()));
                            player.sendMessage(Language.get("friend.request.denied", e));
                        })
                        .build();
                requestAcceptForm.send(player);
            });
        });
        requestsWindow.addButton("§8» §cZurück", "", this::openMain);
        requestsWindow.build().send(player);
    }

    private void openSettings(final Player player) {
        final CustomWindow settingsWindow = new CustomWindow("§7» §8Einstellungen");
        settingsWindow.form()
                .label("§8» §fHier kannst du Einstellungen bezüglich deiner Freunde treffen.\n\n")
                .toggle("§8» §9Level: §fAlle deine Freunde können zu jeder Zeit dein aktuelles Level einsehen.", SettingsAPI.cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/level", "true").equals("true"))
                .toggle("§8» §9Bargeld: §fAlle deine Freunde können zu jeder Zeit sehen, wie viel Bargeld du hast.", SettingsAPI.cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/money", "true").equals("true"))
                .toggle("§8» §9Spielzeit: §fAlle deine Freunde können sehen, wie viele Stunden du schon gespielt hast.", SettingsAPI.cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/onlinetime", "true").equals("true"))
                .toggle("§8» §9Freundschaftsanfragen: §fJeder Spieler darf dir Freundschaftsanfragen senden.", SettingsAPI.cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/requests", "true").equals("true"));

        settingsWindow.onSubmit((g, h) -> {
            final boolean level = h.getToggle(1);
            final boolean money = h.getToggle(2);
            final boolean onlinetime = h.getToggle(3);
            final boolean requests = h.getToggle(4);

            this.serverCore.getSettingsAPI().updateSettings(player.getName(), "friend/level", String.valueOf(level));
            this.serverCore.getSettingsAPI().updateSettings(player.getName(), "friend/money", String.valueOf(money));
            this.serverCore.getSettingsAPI().updateSettings(player.getName(), "friend/onlinetime", String.valueOf(onlinetime));
            this.serverCore.getSettingsAPI().updateSettings(player.getName(), "friend/requests", String.valueOf(requests));

            player.sendMessage(Language.get("friend.settings.updated"));
        });
        settingsWindow.send(player);
    }
}
