package net.eltown.servercore.components.roleplay.feature;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.giftkeys.Giftkey;
import net.eltown.servercore.components.data.rewards.DailyReward;
import net.eltown.servercore.components.event.PlayerClaimDailyRewardEvent;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.RoleplayListener;
import net.eltown.servercore.utils.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record LolaRoleplay(ServerCore serverCore) {

    static final Cooldown playerTalks = new Cooldown(TimeUnit.MINUTES.toMillis(15));
    static final Cooldown talkCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(20));

    static final List<ChainMessage> rewardTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hi, §a%p§7! Liebst du Belohnungen auch so sehr wie ich?", 3),
            new ChainMessage("Vielleicht kannst du etwas einlösen. Schau dich um!", 3),
            new ChainMessage("Einlösen macht Spaß!", 2),
            new ChainMessage("Gutscheeeiiinnnneeee!", 2),
            new ChainMessage("Du kannst Gutscheine auch verschenken.", 2),
            new ChainMessage("Komm täglich zu mir, um Belohnungen zu erhalten!", 2)
    ));

    public void openLolaByNpc(final Player player) {
        this.smallTalk(RoleplayID.FEATURE_LOLA.name(), player, message -> {
            if (message == null) {
                this.openLola(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fLola §8| §7" + message.message().replace("%p", player.getName()));
                            Sound.MOB_VILLAGER_HAGGLE.playSound(player);
                        })
                        .append(message.seconds(), () -> {
                            this.openLola(player);
                            Sound.MOB_VILLAGER_HAGGLE.playSound(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openLola(final Player player) {
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Lola's Rewards", "§8» §fLola §8| §7Bei mir gibt es alles an Geschenken, die für dich sind! Schau dich gerne um und löse ausstehende Sachen ein.");

        this.serverCore.getRewardAPI().getPlayerData(player.getName(), data -> {
            final Calendar calendarNow = new GregorianCalendar();
            calendarNow.setTime(new Date(System.currentTimeMillis()));
            calendarNow.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

            final Calendar calendarReward = new GregorianCalendar();
            calendarReward.setTime(new Date(data.lastReward()));
            calendarReward.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

            if ((calendarReward.get(Calendar.DAY_OF_YEAR) == calendarNow.get(Calendar.DAY_OF_YEAR)) && calendarReward.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)) {
                window.addButton("§8» §1Tägliche Belohnung", "http://eltown.net:3000/img/ui/rewards/daily-reward.png", e -> {
                    final int nextDay = data.day() + 1;
                    if (!(nextDay > 14)) {
                        this.serverCore.getRewardAPI().getRewards(nextDay, dailyRewards -> {
                            final StringBuilder text = new StringBuilder("§8» §fLola §8| §7Deine heutige Belohnung hast du bereits abgeholt! Ich kann dir aber schon zeigen, was es morgen für dich gibt:\n\n");
                            dailyRewards.forEach(p -> {
                                text.append("§8» §r").append(p.description()).append("\n").append("§1Chance: §f").append(p.chance()).append(" Prozent").append("\n\n");
                            });
                            this.openTextLola(player, text.toString());
                        });
                    } else {
                        this.openTextLola(player, "§8» §fLola §8| §7Da du deinen §914-Tage-Streak §7vollendet hast, startest du morgen wieder von vorn. Viel Glück bei den nächsten 14 Tagen!\n\n");
                    }
                });
                return;
            }

            if (((calendarReward.get(Calendar.DAY_OF_YEAR) + 1) == calendarNow.get(Calendar.DAY_OF_YEAR)) && (calendarReward.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)) && !(data.day() >= 14)) {
                this.serverCore.getRewardAPI().getRewards(data.day() + 1, dailyRewards -> {
                    window.addButton("§8» §1Tägliche Belohnung §8[§c§l!§r§8]", "http://eltown.net:3000/img/ui/rewards/daily-reward.png", e -> {
                        final StringBuilder text = new StringBuilder("§8» §fLola §8| §7Herzlichen Glückwunsch! Du bist bei Tag §9" + (data.day() + 1) + " §7angelangt. Hier unten sind deine heutigen Belohnungen aufgelistet, von denen du eine erhälst.\n\n");
                        dailyRewards.forEach(p -> {
                            text.append("§8» §r").append(p.description()).append("\n").append("§1Chance: §f").append(p.chance()).append(" Prozent").append("\n\n");
                        });

                        final ModalWindow redeem = new ModalWindow.Builder("§7» §8Tägliche Belohnung abholen", text.toString(),
                                "§8» §aAbholen", "§8» §cZurück")
                                .onYes(v -> {
                                    this.serverCore.getCoreAPI().getPlayTime(player.getName(), (all, today) -> {
                                        final long minutes = today / 1000 / 60;
                                        if (minutes >= 20) {
                                            this.serverCore.getRewardAPI().addStreak(player.getName());
                                            this.givePlayerDailyReward(player, dailyRewards);
                                        } else {
                                            player.sendMessage(Language.get("reward.onlinetime", 20 - (int) minutes));
                                            Sound.NOTE_BASS.playSound(player);
                                        }
                                    });
                                })
                                .onNo(this::openLola)
                                .build();
                        redeem.send(player);
                    });
                });
            } else if (data.day() >= 14) {
                this.serverCore.getRewardAPI().resetStreak(player.getName());
                this.serverCore.getRewardAPI().getRewards(1, dailyRewards -> {
                    window.addButton("§8» §1Tägliche Belohnung §8[§c§l!§r§8]", "http://eltown.net:3000/img/ui/rewards/daily-reward.png", e -> {
                        final StringBuilder text = new StringBuilder("§8» §fLola §8| §7Da du deinen §914-Tage-Streak §7vollendet hast, startest du wieder von vorn. Viel Glück bei den nächsten 14 Tagen!\n\n");
                        dailyRewards.forEach(p -> {
                            text.append("§8» §r").append(p.description()).append("\n").append("§1Chance: §f").append(p.chance()).append(" Prozent").append("\n\n");
                        });

                        final ModalWindow redeem = new ModalWindow.Builder("§7» §8Tägliche Belohnung abholen", text.toString(),
                                "§8» §aAbholen", "§8» §cZurück")
                                .onYes(v -> {
                                    this.serverCore.getCoreAPI().getPlayTime(player.getName(), (all, today) -> {
                                        final long minutes = today / 1000 / 60;
                                        if (minutes >= 20) {
                                            this.serverCore.getRewardAPI().addStreak(player.getName());
                                            this.givePlayerDailyReward(player, dailyRewards);
                                        } else {
                                            player.sendMessage(Language.get("reward.onlinetime", 20 - (int) minutes));
                                            Sound.NOTE_BASS.playSound(player);
                                        }
                                    });
                                })
                                .onNo(this::openLola)
                                .build();
                        redeem.send(player);
                    });
                });
            } else {
                this.serverCore.getRewardAPI().resetStreak(player.getName());
                this.serverCore.getRewardAPI().getRewards(1, dailyRewards -> {
                    window.addButton("§8» §1Tägliche Belohnung §8[§c§l!§r§8]", "http://eltown.net:3000/img/ui/rewards/daily-reward.png", e -> {
                        final StringBuilder text = new StringBuilder("§8» §fLola §8| §7Du hast leider einen Tag verpasst, daher startest du wieder bei §9Tag 1§7. Komm jeden Tag vorbei, um deine Belohnungen abzuholen. Es lohnt sich!\n\n");
                        dailyRewards.forEach(p -> {
                            text.append("§8» §r").append(p.description()).append("\n").append("§1Chance: §f").append(p.chance()).append(" Prozent").append("\n\n");
                        });

                        final ModalWindow redeem = new ModalWindow.Builder("§7» §8Tägliche Belohnung abholen", text.toString(),
                                "§8» §aAbholen", "§8» §cZurück")
                                .onYes(v -> {
                                    this.serverCore.getCoreAPI().getPlayTime(player.getName(), (all, today) -> {
                                        final long minutes = today / 1000 / 60;
                                        if (minutes >= 20) {
                                            this.serverCore.getRewardAPI().addStreak(player.getName());
                                            this.givePlayerDailyReward(player, dailyRewards);
                                        } else {
                                            player.sendMessage(Language.get("reward.onlinetime", 20 - (int) minutes));
                                            Sound.NOTE_BASS.playSound(player);
                                        }
                                    });
                                })
                                .onNo(this::openLola)
                                .build();
                        redeem.send(player);
                    });
                });
            }
        });

        this.serverCore.getGiftKeyAPI().getCodes(player.getName(), codes -> {
            if (codes == null) {
                window.addButton("§8» §1Gutscheine §8[§c0§8]", "http://eltown.net:3000/img/ui/rewards/giftkeys.png", e -> {
                    player.sendMessage("§8» §fLola §8| §7Oh, anscheinend hast du keine ausstehenden Gutscheine. Spieler können dir Gutscheine schenken oder du kannst welche bei Events erhalten.");
                });
            } else {
                window.addButton("§8» §1Gutscheine §8[§c" + codes.size() + "§8]", "http://eltown.net:3000/img/ui/rewards/giftkeys.png", e -> {
                    this.openPlayerGiftKeys(player, codes);
                });
            }
        });
        window.build().send(player);
    }

    private void openPlayerGiftKeys(final Player player, final Set<String> codes) {
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Deine Gutscheine", "§8» §fLola §8| §7Hier siehst du deine verfügbaren Gutscheine. Klicke einen Gutschein an, um mehr Informationen zu erhalten.");
        codes.forEach(code -> {
            this.serverCore.getGiftKeyAPI().getKey(code, giftkey -> {
                window.addButton("§8» §1" + giftkey.getKey() + "\n§8[§2Einlösbar§8]", g -> {
                    this.openGiftKey(player, giftkey, codes);
                });
            });
        });
        window.addButton("§8» §cZurück", "http://eltown.net:3000/img/ui/back.png", this::openLola);
        window.build().send(player);
    }

    private void openGiftKey(final Player player, final Giftkey giftkey, final Set<String> codes) {
        final String remainingTime = giftkey.getDuration() == -1 ? "§8» §1Zeitlicher Ablauf: §fKeiner" : "§8» §1Zeitlicher Ablauf in: §f" + this.serverCore.getRemainingTimeFuture(giftkey.getDuration());
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Gutschein Information", "§8» §1Gutschein: §f" + giftkey.getKey() + "\n" +
                "§8» §1Eingelöst: §f" + (giftkey.getUses().size() - 1) + "/" + giftkey.getMaxUses() + "\n§8» §1Belohnungen: §f" + giftkey.getRewards().size() + "\n" + remainingTime + "\n\n");
        window.addButton("§8» §aJetzt einlösen", "http://eltown.net:3000/img/ui/rewards/giftkey-redeem.png", e -> {
            this.serverCore.getGiftKeyAPI().getKey(giftkey.getKey(), giftkey1 -> {
                if (giftkey1 == null) {
                    player.sendMessage(Language.get("giftkey.invalid.key", giftkey1.getKey()));
                } else {
                    if (giftkey1.getUses().contains(player.getName())) {
                        player.sendMessage(Language.get("giftkey.already.redeemed"));
                        return;
                    }

                    final ModalWindow confirmWindow = new ModalWindow.Builder("§7» §8Key einlösen", "Möchtest du diesen Key einlösen und die Belohnungen, " +
                            "die dahinter stecken erhalten? Jeder Key kann nur einmal von dir eingelöst werden.\nBitte achte außerdem darauf, dass du genügend freie" +
                            " Inventarplätze hast. Es werden keine Items erstattet, wenn diese nicht zum Inventar hinzugefügt werden konnten!",
                            "§8» §aEinlösen", "§8» §cAbbrechen")
                            .onYes(v -> {
                                this.serverCore.getGiftKeyAPI().redeemKey(giftkey1, player.getName(), giftkeyCalls -> {
                                    switch (giftkeyCalls) {
                                        case CALLBACK_ALREADY_REDEEMED -> {
                                            player.sendMessage(Language.get("giftkey.already.redeemed"));
                                        }
                                        case CALLBACK_NULL -> {
                                            player.sendMessage(Language.get("giftkey.invalid.key", giftkey1.getKey()));
                                        }
                                        case CALLBACK_REDEEMED -> {
                                            giftkey1.getRewards().forEach(reward -> {
                                                final String[] rawReward = reward.split(";");
                                                switch (rawReward[0]) {
                                                    case "item" -> {
                                                        final ItemStack itemStack = SyncAPI.ItemAPI.itemStackFromBase64(rawReward[1]);
                                                        player.getInventory().addItem(itemStack);
                                                        player.sendMessage(Language.get("giftkey.reward.item", itemStack.getI18NDisplayName(), itemStack.getAmount()));
                                                    }
                                                    case "money" -> {
                                                        final double money = Double.parseDouble(rawReward[1]);
                                                        this.serverCore.getEconomyAPI().addMoney(player.getName(), money);
                                                        player.sendMessage(Language.get("giftkey.reward.money", money));
                                                    }
                                                    case "levelxp" -> {
                                                        final double xp = Double.parseDouble(rawReward[1]);
                                                        this.serverCore.getLevelAPI().addExperience(player, xp);
                                                        player.sendMessage(Language.get("giftkey.reward.xp", xp));
                                                    }
                                                    case "crate" -> {
                                                        final String crate = rawReward[1];
                                                        final int i = Integer.parseInt(rawReward[2]);
                                                        this.serverCore.getCrateAPI().addCrate(player.getName(), crate, i);
                                                        player.sendMessage(Language.get("giftkey.reward.crate", this.serverCore.getCrateAPI().convertToDisplay(crate), i));
                                                    }
                                                    default -> player.sendMessage("§cBeim Einlösen des Gutscheins §8[§7" + giftkey.getKey() + "§8] §ctrat ein Fehler auf. §7[§f" + player.getName() + ", " + rawReward[0] + "§7]");
                                                }
                                            });
                                        }
                                    }
                                });
                            })
                            .onNo(v -> this.openPlayerGiftKeys(player, codes))
                            .build();
                    confirmWindow.send(player);
                }
            });
        });
        window.addButton("§8» §9Gutschein verschenken", "http://eltown.net:3000/img/ui/rewards/giftkey-donate.png", e -> {
            final CustomWindow customWindow = new CustomWindow("§7» §8Gutschein verschenken");
            customWindow.form()
                    .label("§8» §fLola §8| §7Du kannst Gutscheine einfach an einen anderen Spieler verschenken. Dieser wird dann in seinem Gutschein-Menü angezeigt und bei dir verschwindet der Gutschein. " +
                            "Du oder andere Spieler können den Code aber trotzdem noch einlösen.")
                    .input("§8» §fBitte gebe einen Spielernamen an.\n§8[§c§l!§r§8] §cAchte auf die Groß- und Kleinschreibung des Namens. Der Name muss korrekt sein!", "EltownSpielerHD123");

            customWindow.onSubmit((g, h) -> {
                final String target = h.getInput(1);
                if (target.isEmpty() || target.equals(player.getName())) {
                    player.sendMessage(Language.get("giftkey.invalid.target"));
                    return;
                }

                this.serverCore.getGiftKeyAPI().addMark(giftkey.getKey(), target, player.getName(), added -> {
                    player.sendMessage(Language.get("giftkey.mark.added", target));
                });
            });
            customWindow.send(player);
        });
        if (codes != null) window.addButton("§8» §cZurück", "http://eltown.net:3000/img/ui/back.png", e -> this.openPlayerGiftKeys(player, codes));
        window.build().send(player);
    }

    private void openTextLola(final Player player, final String text) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8Lola's Rewards", text)
                .addButton("§8» §cZurück", "http://eltown.net:3000/img/ui/back.png", this::openLola)
                .build();
        window.send(player);
    }

    private void givePlayerDailyReward(final Player player, final Set<DailyReward> dailyRewards) {
        final ArrayList<DailyReward> rewards = new ArrayList<>();

        while (rewards.size() <= 0) {
            dailyRewards.forEach(r -> {
                if (r.chance() > ThreadLocalRandom.current().nextInt(101)) rewards.add(r);
            });
        }
        final DailyReward reward = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
        final String[] dataSplit = reward.data().split(";");
        switch (dataSplit[0]) {
            case "item" -> player.getInventory().addItem(SyncAPI.ItemAPI.itemStackFromBase64(dataSplit[1]));
            case "xp" -> this.serverCore.getLevelAPI().addExperience(player, Double.parseDouble(dataSplit[1]));
            case "money" -> this.serverCore.getEconomyAPI().addMoney(player.getName(), Double.parseDouble(dataSplit[1]));
            case "crate" -> this.serverCore.getCrateAPI().addCrate(player.getName(), dataSplit[1], Integer.parseInt(dataSplit[2]));
        }
        player.sendMessage(Language.get("reward.received", reward.description(), reward.day()));
        this.serverCore.getServer().getPluginManager().callEvent(new PlayerClaimDailyRewardEvent(player, dailyRewards, reward));
        Sound.RANDOM_LEVELUP.playSound(player, 1, 2);
    }

    private void smallTalk(final String npc, final Player player, final Consumer<ChainMessage> message) {
        if (talkCooldown.hasCooldown(npc + "//" + player.getName())) {
            message.accept(null);
            return;
        }
        if (!playerTalks.hasCooldown(npc + "//" + player.getName())) {
            message.accept(LolaRoleplay.rewardTalks.get(0));
        } else {
            int index = ThreadLocalRandom.current().nextInt(1, LolaRoleplay.rewardTalks.size());
            message.accept(LolaRoleplay.rewardTalks.get(index));
        }
        RoleplayListener.openQueue.add(player.getName());
    }

}
