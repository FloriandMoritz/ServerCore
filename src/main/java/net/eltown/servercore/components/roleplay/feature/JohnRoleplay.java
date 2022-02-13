package net.eltown.servercore.components.roleplay.feature;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.crates.Raffle;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.components.tasks.RaffleTask;
import net.eltown.servercore.listeners.RoleplayListener;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record JohnRoleplay(ServerCore serverCore) {

    public static ArmorStand hologram = null;
    public static boolean crateInUse = false;

    public JohnRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
            final Location location = new Location(this.serverCore.getServer().getWorld("world"), 18, 70, 12);
            hologram = (ArmorStand) location.getWorld().spawnEntity(location.add(0.5, -1.2, 0.5), EntityType.ARMOR_STAND);
            hologram.getPersistentDataContainer().set(new NamespacedKey(ServerCore.getServerCore(), "container.hologram"), PersistentDataType.INTEGER, 1);
            hologram.setGravity(false);
            hologram.setCollidable(false);
            hologram.setInvulnerable(true);
            hologram.setCanPickupItems(false);
            hologram.setCustomNameVisible(true);
            hologram.setVisible(false);
            hologram.setCustomName("§5§lGlückstruhe");
            hologram.setCanMove(false);
        }, 170);
    }

    static final Cooldown playerTalks = new Cooldown(TimeUnit.MINUTES.toMillis(15));
    static final Cooldown talkCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(20));

    static final List<ChainMessage> johnTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Guten Tag, §a%p§7! Was wünschst du dir heute?", 3),
            new ChainMessage("Mit etwas Glück, bekommst du das was du möchtest!", 3),
            new ChainMessage("Also, ich habe immer Glück!", 2),
            new ChainMessage("Gewinne, Gewinne, Gewinne!", 2)
    ));

    public void openJohnByNpc(final Player player) {
        this.smallTalk(RoleplayID.FEATURE_JOHN.name(), player, message -> {
            if (message == null) {
                this.openJohn(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fJohn §8| §7" + message.message().replace("%p", player.getName()));
                        })
                        .append(message.seconds(), () -> {
                            this.openJohn(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    private void openJohn(final Player player) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8John", "§8» §fJohn §8| §7Schön, dass du hier bist! Welche Truhe möchtest du heute kaufen? Lass dir gerne Zeit und suche dir eine aus!")
                .addButton("§8» §7§lGewöhnliche §r§7Truhe\n§f§oAb: §r§f$49.95", "http://eltown.net:3000/img/ui/crates/common-display.png", e -> {
                    final CustomWindow selectOfferWindow = new CustomWindow("§7» §8Angebot wählen");
                    selectOfferWindow.form()
                            .stepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot. §9Ich möchte eine eigene Menge angeben.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §95 Truhen §7für §9$209.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §910 Truhen §7für §9$419.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §920 Truhen §7für §9$839.95 §7klingt sehr interessant.");

                    selectOfferWindow.onSubmit((g, h) -> {
                        switch (h.getStepSlide(0)) {
                            case 0 -> this.openSelectCrate(player, "common", 49.95);
                            case 1 -> this.openBuyCrate(player, "common", 5, 209.95);
                            case 2 -> this.openBuyCrate(player, "common", 10, 419.95);
                            case 3 -> this.openBuyCrate(player, "common", 20, 839.95);
                        }
                    });
                    selectOfferWindow.send(player);
                })
                .addButton("§8» §1§lUngewöhnliche §r§1Truhe\n§f§oAb: §r§f$199.95", "http://eltown.net:3000/img/ui/crates/uncommon-display.png", e -> {
                    final CustomWindow selectOfferWindow = new CustomWindow("§7» §8Angebot wählen");
                    selectOfferWindow.form()
                            .stepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot. §9Ich möchte eine eigene Menge angeben.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §95 Truhen §7für §9$919.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §910 Truhen §7für §9$1839.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §920 Truhen §7für §9$3679.95 §7klingt sehr interessant.");

                    selectOfferWindow.onSubmit((g, h) -> {
                        switch (h.getStepSlide(0)) {
                            case 0 -> this.openSelectCrate(player, "uncommon", 199.95);
                            case 1 -> this.openBuyCrate(player, "uncommon", 5, 919.95);
                            case 2 -> this.openBuyCrate(player, "uncommon", 10, 1839.95);
                            case 3 -> this.openBuyCrate(player, "uncommon", 20, 3679.95);
                        }
                    });
                    selectOfferWindow.send(player);
                })
                .addButton("§8» §5§lEpische §r§5Truhe\n§f§oAb: §r§f$399.95", "http://eltown.net:3000/img/ui/crates/epic-display.png", e -> {
                    final CustomWindow selectOfferWindow = new CustomWindow("§7» §8Angebot wählen");
                    selectOfferWindow.form()
                            .stepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot. §9Ich möchte eine eigene Menge angeben.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §93 Truhen §7für §9$949.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §98 Truhen §7für §9$2699.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §916 Truhen §7für §9$5399.95 §7klingt sehr interessant.");

                    selectOfferWindow.onSubmit((g, h) -> {
                        switch (h.getStepSlide(0)) {
                            case 0 -> this.openSelectCrate(player, "epic", 399.95);
                            case 1 -> this.openBuyCrate(player, "epic", 3, 949.95);
                            case 2 -> this.openBuyCrate(player, "epic", 8, 2699.95);
                            case 3 -> {this.openBuyCrate(player, "epic", 16, 5399.95);}
                        }
                    });
                    selectOfferWindow.send(player);
                })
                .addButton("§8» §g§lLegendäre §r§gTruhe\n§f§oAb: §r§f$799.95", "http://eltown.net:3000/img/ui/crates/legendary-display.png", e -> {
                    final CustomWindow selectOfferWindow = new CustomWindow("§7» §8Angebot wählen");
                    selectOfferWindow.form()
                            .stepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot. §9Ich möchte eine eigene Menge angeben.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §93 Truhen §7für §9$2149.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §98 Truhen §7für §9$5899.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §916 Truhen §7für §9$11799.95 §7klingt sehr interessant.");

                    selectOfferWindow.onSubmit((g, h) -> {
                        switch (h.getStepSlide(0)) {
                            case 0 -> this.openSelectCrate(player, "legendary", 799.95);
                            case 1 -> this.openBuyCrate(player, "legendary", 3, 2149.95);
                            case 2 -> this.openBuyCrate(player, "legendary", 8, 5899.95);
                            case 3 -> this.openBuyCrate(player, "legendary", 16, 11799.95);
                        }
                    });
                    selectOfferWindow.send(player);
                })
                .build();
        window.send(player);
    }

    public void openSelectCrate(final Player player, final String crate, final double price) {
        final CustomWindow window = new CustomWindow("");
        window.form()
                .slider("§8» §fJohn §8| §7Wie oft möchtest du " + this.serverCore.getCrateAPI().convertToDisplay(crate) + " §7kaufen? Eine kostet §9$" + this.serverCore.getMoneyFormat().format(price) + "§7", 1, 50, 1, 1);

        window.onSubmit((g, h) -> {
            final int amount = (int) h.getSlider(0);
            this.openBuyCrate(player, crate, amount, amount * price);
        });
        window.send(player);
    }

    public void openBuyCrate(final Player player, final String crate, final int amount, final double price) {
        final ModalWindow window = new ModalWindow.Builder("§7» §8Glücksboxen kaufen", "§8» §fJohn §8| §7Möchtest du die §9" + amount + "x " + this.serverCore.getCrateAPI().convertToDisplay(crate) + " §7für §9$" + this.serverCore.getMoneyFormat().format(price) + " §7kaufen?" +
                "\n\n§cDiese Aktion kann nicht rückgängig gemacht werden! Die gekauften Glücksboxen werden sofort auf deinen Account übertragen.",
                "§8» §aKaufen", "§8» §cAbbrechen")
                .onYes(v -> {
                    this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                        if (money >= price) {
                            this.serverCore.getEconomyAPI().reduceMoney(player.getName(), price);
                            this.serverCore.getCrateAPI().addCrate(player.getName(), crate, amount);
                            player.sendMessage(Language.get("crate.bought", this.serverCore.getCrateAPI().convertToDisplay(crate), amount, price));
                        } else {
                            player.sendMessage(Language.get("crate.not.enough.money"));
                        }
                    });
                })
                .onNo(this::openJohn)
                .build();
        window.send(player);
    }

    public void openCrate(final Player player) {
        if (crateInUse) {
            player.sendMessage(Language.get("crate.already.in.use"));
            return;
        }
        player.playSound(player.getLocation(), "random.enderchestopen", 2, 3);
        this.serverCore.getCrateAPI().getPlayerData(player.getName(), map -> {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8Glückstruhen", "§8» §fWähle eine Truhe aus, um diese zu öffnen. Du kannst dir bei John Truhen kaufen. Manchmal hat er sogar spezielle Angebote!\n\n")
                    .addButton("§8» §7§lGewöhnliche §r§7Truhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("common", 0) + "§8]", "http://eltown.net:3000/img/ui/crates/common-display.png", e -> {
                        if (crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            return;
                        }
                        if (map.getOrDefault("common", 0) > 0) {
                            this.raffleCrate(player, "common");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                        }
                    })
                    .addButton("§8» §1§lUngewöhnliche §r§1Truhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("uncommon", 0) + "§8]", "http://eltown.net:3000/img/ui/crates/uncommon-display.png", e -> {
                        if (crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            return;
                        }
                        if (map.getOrDefault("uncommon", 0) > 0) {
                            this.raffleCrate(player, "uncommon");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                        }
                    })
                    .addButton("§8» §5§lEpische §r§5Truhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("epic", 0) + "§8]", "http://eltown.net:3000/img/ui/crates/epic-display.png", e -> {
                        if (crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            return;
                        }
                        if (map.getOrDefault("epic", 0) > 0) {
                            this.raffleCrate(player, "epic");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                        }
                    })
                    .addButton("§8» §g§lLegendäre §r§gTruhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("legendary", 0) + "§8]", "http://eltown.net:3000/img/ui/crates/legendary-display.png", e -> {
                        if (crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            return;
                        }
                        if (map.getOrDefault("legendary", 0) > 0) {
                            this.raffleCrate(player, "legendary");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                        }
                    })
                    .build();
            window.send(player);
        });
    }

    public void raffleCrate(final Player player, final String crate) {
        this.serverCore.getCrateAPI().removeCrate(player.getName(), crate, 1);
        this.serverCore.getCrateAPI().getCrateRewards(crate, rewards -> {
            final Raffle raffle = new Raffle(new ArrayList<>(rewards));
            this.serverCore.getServer().getScheduler().runTask(this.serverCore, new RaffleTask(this.serverCore, raffle, player));
            crateInUse = true;
        });
    }

    private void smallTalk(final String npc, final Player player, final Consumer<ChainMessage> message) {
        if (talkCooldown.hasCooldown(npc + "//" + player.getName())) {
            message.accept(null);
            return;
        }
        if (!playerTalks.hasCooldown(npc + "//" + player.getName())) {
            message.accept(JohnRoleplay.johnTalks.get(0));
        } else {
            int index = ThreadLocalRandom.current().nextInt(1, JohnRoleplay.johnTalks.size());
            message.accept(JohnRoleplay.johnTalks.get(index));
        }
        RoleplayListener.openQueue.add(player.getName());
    }

}
