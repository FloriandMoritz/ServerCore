package net.eltown.servercore.components.roleplay.government;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.RoleplayListener;
import net.eltown.servercore.utils.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public record TownhallRoleplay(ServerCore serverCore) {

    public TownhallRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        final World world = this.serverCore.getServer().getWorld("world");

        cachedAgencies.put(4, new Agency(4, "Amtsgericht", "Herr Meier", new Location(world, 73, 73, 93), new Location(world, 76, 73, 87), new Location(world, 75, 74, 87), "null"));
        cachedAgencies.put(10, new Agency(10, "Bauamt", "Herr Keppel", new Location(world, 89, 80, 53), new Location(world, 83, 80, 51), new Location(world, 83, 81, 52), "null"));
        cachedAgencies.put(13, new Agency(13, "Steuern", "Frau Bärwald", new Location(world, 64, 80, 90), new Location(world, 61, 80, 83), new Location(world, 60, 81, 83), "null"));

        cachedAdviceBureau.put(1, new AdviceBureau(1, "Frau Haaf", new Location(world, 93, 73, 71), new Location(world, 91, 74, 73), "null"));
        cachedAdviceBureau.put(2, new AdviceBureau(2, "Frau Damer", new Location(world, 93, 73, 65), new Location(world, 91, 74, 67), "null"));
        cachedAdviceBureau.put(3, new AdviceBureau(3, "Herr Ibler", new Location(world, 93, 73, 59), new Location(world, 91, 74, 61), "null"));
    }

    public static final HashMap<Integer, Agency> cachedAgencies = new HashMap<>();
    public static final HashMap<Integer, AdviceBureau> cachedAdviceBureau = new HashMap<>();
    public static final HashMap<String, Agency> cachedAppointments = new HashMap<>();

    static final List<ChainMessage> receptionTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Herzlich Willkommen im Rathaus, §a%p§7! Wie kann ich Ihnen weiterhelfen?", 4),
            new ChainMessage("Oh, wie kann ich Ihnen weiterhelfen?", 2),
            new ChainMessage("Was kann ich Ihnen Gutes tun?", 2)
    ));

    public void openReceptionByNpc(final Player player) {
        this.smallTalk(receptionTalks, RoleplayID.TOWNHALL_RECEPTION.name(), player, message -> {
            if (message == null) {
                this.openReception(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fHerr Kaufmann §8| §7" + message.message().replace("%p", player.getName()));
                        })
                        .append(message.seconds(), () -> {
                            this.openReception(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    private void openReception(final Player player) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8Rezeptionist", "§8» §fHerr Kaufmann §8| §7Mit welcher Verwaltungskraft möchten Sie sprechen?")
                .addButton("§8» §eBauamt\n§fHerr Keppel §8| §9Zimmer 10, 1. OG", "http://eltown.net:3000/img/job/rathaus/receptionist_building.png", e -> {
                    this.openConfirmAppointment(player, cachedAgencies.get(10), "Zimmer 10, 1. OG");
                })
                /*.addButton("§8» §eSteuern\n§fFrau Bärwald §8| §9Zimmer 13, 1. OG", "http://eltown.net:3000/img/job/rathaus/receptionist_taxes.png", e -> {
                    this.openConfirmAppointment(player, cachedAgencies.get(13), "Zimmer 13, 1. OG");
                })*/
                .build();
        window.send(player);
    }

    private void openConfirmAppointment(final Player player, final Agency agency, final String roomInfo) {
        final ModalWindow window = new ModalWindow.Builder("§7» §8Rezeptionist", "§8» §fHerr Kaufmann §8| §e" + agency.getName() + " §7hat sicherlich noch einen Termin frei. Soll ich Ihnen einen buchen?",
                "§8» §aTermin buchen", "§8» §cAbbrechen")
                .onYes(e -> {
                    if (!this.isAlreadyInAdviceBureauAppointment(player.getName())) {
                        if (!dateExpire.hasCooldown(player.getName())) {
                            this.setAppointment(player.getName(), agency);
                            player.sendMessage("§8» §fHerr Kaufmann §8| §7Der Termin wurde gebucht. Dieser ist nun §e7 Minuten §7lang gültig. §e" + agency.getName() + "'s §7Büro finden Sie hier: §e" + roomInfo);
                            player.sendMessage("§8» §fHerr Kaufmann §8| §7Klicke das jeweilige Türschild an, um den Termin wahrzunehmen.");
                        } else player.sendMessage("§8» §fHerr Kaufmann §8| §7Sie haben bereits einen Termin gebucht.");
                    } else player.sendMessage("§8» §fHerr Kaufmann §8| §7Sie haben bereits einen laufenden Termin im Bürgerbüro.");
                })
                .onNo(this::openReception)
                .build();
        window.send(player);
    }

    public void openHerrKeppel(final Player player, final Agency agency) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8" + agency.getName(), "§8» §f" + player.getName() + " §8| §7Hallo, ich habe einen Termin bei Ihnen!\n\n" +
                "§8» §f" + agency.getName() + " §8| §7Hallo, §a" + player.getName() + "§7! Schön, dass Sie da sind. Was kann ich für Sie tun?")
                .addButton("§8» §eGrundstücke\n§8- §fBaurechte §8-", "http://eltown.net:3000/img/job/rathaus/building_build_permissions.png", e -> {
                    final int plot = this.getNextPlot(player);
                    final String nextPlot = plot != 0 ? "§8» §f" + agency.getName() + " §8| §7Das klingt gut! Ich könnte Ihnen weitere Grundstücksrechte anbieten." : "§8» §f" + agency.getName() + " §8| §7Aktuell kann ich Ihnen leider kein weiteres Grundstück anbieten. Vielleicht stehen bald wieder welche zur Verfügung.";

                    final SimpleWindow.Builder selectWindow = new SimpleWindow.Builder("§7» §8" + agency.getName(), "§8» §f" + player.getName() + " §8| §7Ich denke aktuell über weitere Grundstücke nach...\n\n" + nextPlot + "\n\n");
                    if (plot != 0) {
                        selectWindow.addButton("§8» §9Weiteres Grundstück erwerben", "", g -> {
                            final ModalWindow confirmWindow = new ModalWindow.Builder("§7» §8" + agency.getName(), "§8» §f" + agency.getName() + " §8| §7Möchten Sie ein weiteres Grundstück für §a$5.000 §7kaufen?\n\n§cDiese Aktion kann unter keinen Umständen rückgängig gemacht werden!",
                                    "§8» §aGrundstück kaufen", "§8» §cAbbrechen")
                                    .onYes(v -> {
                                        this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                                            if (money >= 5000) {
                                                this.serverCore.getEconomyAPI().reduceMoney(player.getName(), 5000);
                                                if (plot != 3) this.serverCore.getGroupAPI().removePlayerPermission(player.getName(), "plots.claim." + (plot - 1));
                                                this.serverCore.getGroupAPI().addPlayerPermission(player.getName(), "plots.claim." + plot);
                                                Sound.RANDOM_LEVELUP.playSound(player, 1, 3);
                                                player.sendMessage("§8» §f" + agency.getName() + " §8| §7Sehr gut! Sie haben ein weiteres Grundstück soeben erworben.");
                                            } else {
                                                Sound.NOTE_BASS.playSound(player);
                                                player.sendMessage("§8» §f" + player.getName() + " §8| §7Ich habe leider zu wenig Geld dabei...");
                                            }
                                        });
                                    })
                                    .onNo(v -> this.openHerrKeppel(player, agency))
                                    .build();
                            confirmWindow.send(player);
                        });
                    }
                    selectWindow.build().send(player);
                })
                .addButton("§8» §cTermin beenden", "http://eltown.net:3000/img/ui/cancel.png", e -> {
                    this.endAppointment(player, cachedAgencies.get(10));
                })
                .build();
        window.send(player);
    }

    public void openAdviceBureau(final Player player, final AdviceBureau adviceBureau) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8" + adviceBureau.getName(), "§8» §f" + player.getName() + " §8| §7Guten Tag! Ich hätte da ein Anliegen...\n\n" +
                "§8» §f" + adviceBureau.getName() + " §8| §7Guten Tag, §a" + player.getName() + "§7! Was kann ich für Sie tun?")
                .addButton("§8» §eChestShop\n§8- §fLizenz beantragen §8-", "http://eltown.net:3000/img/job/rathaus/taxes_chestshop_license.png", e -> {
                    final ShopLicense shopLicense = this.serverCore.getChestShopAPI().getPlayerLicense(e.getName());
                    final ShopLicense nextShopLicense = this.getNextLicense(e.getName());
                    final String nextLevel = nextShopLicense != null ? "Ich kann Ihnen eine bessere Lizenz anbieten, sofern Sie möchten." : "Da Sie bereits die beste Lizenz besitzen, " +
                            "kann ich Ihnen keine bessere anbieten.";

                    final SimpleWindow.Builder licenseWindow = new SimpleWindow.Builder("§7» §8" + adviceBureau.getName(), "§8» §f" + e.getName() + " §8| §7Ich habe ein Anliegen bezüglich der ChestShop-Lizenzen.\n\n" +
                            "§8» §f" + adviceBureau.getName() + " §8| §7Alles klar. Sie besitzen aktuell die §e" + shopLicense.getLicense().displayName() + "§7-Lizenz mit §e" + shopLicense.getLicense().maxPossibleShops() + "§7 " +
                            "erstellbaren Shops.\n§7" + nextLevel);
                    if (nextShopLicense != null) {
                        licenseWindow.addButton("§8» §9" + nextShopLicense.getLicense().displayName() + "-Lizenz\n§a$" +
                                this.serverCore.getMoneyFormat().format(nextShopLicense.getLicense().money()), "http://eltown.net:3000/img/job/rathaus/taxes_chestshop_license_upgrade.png", g -> {
                            final ModalWindow confirmWindow = new ModalWindow.Builder("§7» §8" + adviceBureau.getName(), "§8» §f" + player.getName() + " §8| §7Das klingt interessant...\n\n" +
                                    "§8» §f" + adviceBureau.getName() + " §8| §7Möchten Sie diese Lizenz für eine einmalige Zahlung von §a$" + this.serverCore.getMoneyFormat().format(nextShopLicense.getLicense().money()) + "§7 kaufen?",
                                    "§8» §aLizenz kaufen", "§8» §cAbbrechen")
                                    .onYes(v -> {
                                        this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                                            if (money >= nextShopLicense.getLicense().money()) {
                                                this.serverCore.getEconomyAPI().reduceMoney(player.getName(), nextShopLicense.getLicense().money());
                                                this.serverCore.getChestShopAPI().setLicense(player.getName(), nextShopLicense.getLicense());
                                                Sound.RANDOM_LEVELUP.playSound(player, 1, 3);
                                                player.sendMessage("§8» §f" + adviceBureau.getName() + " §8| §7Sehr gut! Sie haben die Lizenz nun gekauft. Sie können direkt die Vorteile dieser Lizenz nutzen.");
                                            } else {
                                                Sound.NOTE_BASS.playSound(player);
                                                player.sendMessage("§8» §f" + player.getName() + " §8| §7Ich habe leider nicht genug Geld dabei...");
                                            }
                                        });
                                    })
                                    .onNo(v -> this.openAdviceBureau(player, adviceBureau))
                                    .build();
                            confirmWindow.send(player);
                        });
                    }
                    licenseWindow.build().send(player);
                })
                .addButton("§8» §cGespräch beenden", "http://eltown.net:3000/img/ui/cancel.png", e -> {
                    this.endAdviceBureauAppointment(player, cachedAdviceBureau.get(adviceBureau.getHolder()));
                })
                .build();
        window.send(player);
    }

    private void endAppointment(final Player player, final Agency agency) {
        if (agency.getCurrentPlayer().equals(player.getName())) {
            cachedAgencies.get(agency.getRoom()).setCurrentPlayer("null:0");
            player.teleport(agency.getOut());
            Sound.RANDOM_DOOR_CLOSE.playSound(player);
            player.sendMessage("§8» §f" + agency.getName() + " §8| §7Tschüss! Bis bald.");
        }
    }

    private void endAdviceBureauAppointment(final Player player, final AdviceBureau adviceBureau) {
        if (adviceBureau.getCurrentPlayer().equals(player.getName())) {
            cachedAdviceBureau.get(adviceBureau.getHolder()).setCurrentPlayer("null:0");
            player.sendMessage("§8» §f" + adviceBureau.getName() + " §8| §7Tschüss! Bis bald.");
        }
    }

    public void setAppointment(final String player, final Agency agency) {
        cachedAppointments.put(player, agency);
    }

    public void removeAppointment(final String player) {
        cachedAppointments.remove(player);
    }

    public void takeAppointment(final String player, final Agency agency) {
        cachedAgencies.get(agency.getRoom()).setCurrentPlayer(player);
        dateExpire.removeCooldown(player);
        this.setDoorSignText(agency.getSign(), "§cBesetzt§r");

        this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
            final Agency a = cachedAgencies.get(agency.getRoom());
            if (!a.getCurrentPlayer().equals("null:0")) {
                final Player t = this.serverCore.getServer().getPlayer(player);
                if (t != null) {
                    t.teleport(a.getOut());
                    Sound.RANDOM_DOOR_CLOSE.playSound(t);
                    t.sendMessage("§8» §f" + agency.getName() + " §8| §7Den Termin müssten wir jetzt beenden, da ich gleich einen anderen habe. Auf Wiedersehen!");
                }
            }

            this.finishAppointment(a);
            this.setDoorSignText(a.getSign(), "§2Frei§r");
        }, 3600);
    }

    public void takeAdviceBureauAppointment(final String player, final AdviceBureau adviceBureau) {
        cachedAdviceBureau.get(adviceBureau.getHolder()).setCurrentPlayer(player);
        this.setDoorSignText(adviceBureau.getSign(), "§cBesetzt§r");

        this.serverCore.getServer().getScheduler().scheduleSyncDelayedTask(this.serverCore, () -> {
            final AdviceBureau a = cachedAdviceBureau.get(adviceBureau.getHolder());
            if (!a.getCurrentPlayer().equals("null:0")) {
                final Player t = this.serverCore.getServer().getPlayer(player);
                if (t != null) {
                    t.sendMessage("§8» §f" + adviceBureau.getName() + " §8| §7Das Gespräch müssten wir nun beenden, da ich noch etwas zu tun habe. Auf Wiedersehen!");
                }
            }

            this.finishAdviceBureauAppointment(a);
            this.setDoorSignText(a.getSign(), "§2Frei§r");
        }, 3600);
    }

    public void finishAppointment(final Agency agency) {
        cachedAgencies.get(agency.getRoom()).setCurrentPlayer("null");
    }

    public void finishAdviceBureauAppointment(final AdviceBureau adviceBureau) {
        cachedAdviceBureau.get(adviceBureau.getHolder()).setCurrentPlayer("null");
    }

    public boolean isInAppointment(final String player, final int room) {
        return cachedAgencies.get(room).getCurrentPlayer().equals(player);
    }

    public boolean isInAdviceBureauAppointment(final String player, final int holder) {
        return cachedAdviceBureau.get(holder).getCurrentPlayer().equals(player);
    }

    public boolean isAlreadyInAdviceBureauAppointment(final String player) {
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        cachedAdviceBureau.values().forEach(e -> {
            if (e.getCurrentPlayer().equals(player)) atomicBoolean.set(true);
        });
        return atomicBoolean.get();
    }

    public void setDoorSignText(final Location location, final String information) {
        final Sign sign = (Sign) location.getBlock().getState();
        if (sign != null) {
            sign.line(2, Component.text(information));
            sign.update(true);
        }
    }

    public HashMap<Location, Agency> getSignLocations() {
        final HashMap<Location, Agency> map = new HashMap<>();
        cachedAgencies.values().forEach(e -> {
            map.put(e.getSign(), e);
        });
        return map;
    }

    public HashMap<Location, AdviceBureau> getAdviceBureauSignLocations() {
        final HashMap<Location, AdviceBureau> map = new HashMap<>();
        cachedAdviceBureau.values().forEach(e -> {
            map.put(e.getSign(), e);
        });
        return map;
    }

    private ShopLicense getNextLicense(final String player) {
        final ShopLicense shopLicense = this.serverCore.getChestShopAPI().getPlayerLicense(player);
        return switch (shopLicense.getLicense()) {
            case STANDARD -> new ShopLicense(player, ShopLicense.ShopLicenseType.SMALL_BUSINESS, shopLicense.getAdditionalShops());
            case SMALL_BUSINESS -> new ShopLicense(player, ShopLicense.ShopLicenseType.BUSINESS, shopLicense.getAdditionalShops());
            case BUSINESS -> new ShopLicense(player, ShopLicense.ShopLicenseType.BIG_BUSINESS, shopLicense.getAdditionalShops());
            case BIG_BUSINESS -> new ShopLicense(player, ShopLicense.ShopLicenseType.COMPANY, shopLicense.getAdditionalShops());
            default -> null;
        };
    }

    private int getNextPlot(final Player player) {
        if (player.hasPermission("plots.claim.3")) return 4;
        else if (player.hasPermission("plots.claim.4")) return 5;
        else if (player.hasPermission("plots.claim.5")) return 6;
        else if (player.hasPermission("plots.claim.6")) return 0;
        else return 3;
    }

    public static final Cooldown dateExpire = new Cooldown(TimeUnit.MINUTES.toMillis(7));

    static final Cooldown playerTalks = new Cooldown(TimeUnit.MINUTES.toMillis(15));
    static final Cooldown talkCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(20));

    private void smallTalk(final List<ChainMessage> messages, final String npc, final Player player, final Consumer<ChainMessage> message) {
        if (talkCooldown.hasCooldown(npc + "//" + player.getName())) {
            message.accept(null);
            return;
        }
        if (!playerTalks.hasCooldown(npc + "//" + player.getName())) {
            message.accept(messages.get(0));
        } else {
            int index = ThreadLocalRandom.current().nextInt(1, messages.size());
            message.accept(messages.get(index));
        }
        RoleplayListener.openQueue.add(player.getName());
    }

    @AllArgsConstructor
    @Data
    public static class Agency {

        private final int room;
        private final String job;
        private final String name;
        private final Location in;
        private final Location out;
        private final Location sign;
        private String currentPlayer;

    }

    @AllArgsConstructor
    @Data
    public static class AdviceBureau {

        private final int holder;
        private final String name;
        private final Location to;
        private final Location sign;
        private String currentPlayer;

    }

}
