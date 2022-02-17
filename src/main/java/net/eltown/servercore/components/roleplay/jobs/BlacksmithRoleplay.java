package net.eltown.servercore.components.roleplay.jobs;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.CustomEnchantments;
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
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record BlacksmithRoleplay(ServerCore serverCore) {

    private static final List<ChainMessage> blacksmithTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hallo §a%p§7! Ich bin Ben, der Schmied! Schau dir doch mal mein Angebot an, wenn du möchtest.", 5),
            new ChainMessage("Meine Angebote sind der Hammer, oder?", 3),
            new ChainMessage("Falls du Fragen zu Verzauberungen hast, frag!", 3),
            new ChainMessage("Ich garantiere, dass jede Verzauberung funktioniert!", 3),
            new ChainMessage("Mein Stand ist vom Amt geprüft, da kannst du dir sicher sein!", 3),
            new ChainMessage("Dieser Job ist ein echter Knochenjob!", 3),
            new ChainMessage("Kauf ruhig, ich habe Zeit!", 2)
    ));

    public void openBlacksmithShopByNpc(final Player player) {
        this.smallTalk(blacksmithTalks, RoleplayID.JOB_BLACKSMITH.name(), player, message -> {
            if (message == null) {
                this.openBlacksmithShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fBen §8| §7" + message.message().replace("%p", player.getName()));
                        })
                        .append(message.seconds(), () -> {
                            this.openBlacksmithShop(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    private void openBlacksmithShop(final Player player) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8Schmied Ben", "§8» §fBen §8| §7Aktuell biete ich Reparaturen und bestimmte Verzauberungen an. Schau dich gerne bei mir um, es lohnt sich!")
                .addButton("§8» §fBen's spezielle\n§fVerzauberungen", "http://eltown.net:3000/img/ui/enchanted_book.png", this::openEnchantments)
                .addButton("§8» §fMein Angebot", "http://eltown.net:3000/img/ui/paper.png", this::openEnchantmentInfo)
                .addButton("§8» §fReparatur Service", "http://eltown.net:3000/img/ui/anvil.png", this::openRepairService)
                .build();
        window.send(player);
    }

    static List<CustomEnchantments.Enchantment> enchantments = new LinkedList<>(List.of(
            CustomEnchantments.Enchantment.DRILL,
            CustomEnchantments.Enchantment.EMERALD_FARMER,
            CustomEnchantments.Enchantment.LUMBERJACK,
            CustomEnchantments.Enchantment.VEIN_MINING,
            CustomEnchantments.Enchantment.EXPERIENCE
    ));

    public void openEnchantments(final Player player) {
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Meine Verzauberungen", "§8» §fBen §8| §7Informiere dich gut über meine Verzauberungen bei §8» §fMein Angebot§7, damit nichts schief läuft. Natürlich kannst du bei mir auch deine Verzauberungen upgraden!");

        enchantments.forEach(enchantment -> {
            window.addButton("§8» §f" + enchantment.enchantment() + "\n§8- §fAb: §9$" + this.serverCore.getMoneyFormat().format(enchantment.price()) + " §8-", e -> {
                if (enchantment.level() == 1) this.openConfirmBuyEnchantment(player, enchantment, 1);
                else this.openSelectEnchantmentLevel(player, enchantment);
            });
        });
        window.addButton("§8» §cZurück", "http://eltown.net:3000/img/ui/back.png", this::openBlacksmithShop);
        window.build().send(player);
    }

    public void openSelectEnchantmentLevel(final Player player, final CustomEnchantments.Enchantment enchantment) {
        final CustomWindow window = new CustomWindow("§7» §8Verzauberungslevel wählen");
        window.form()
                .label("§8» §fBen §8| §7Bei dieser Verzauberung kannst du ein Level wählen. Die Kosten für das erste Level betragen §a$" + this.serverCore.getMoneyFormat().format(enchantment.price()) + "§7.\n" +
                        "Die Level werden mit dem genannten Preis multipliziert.")
                .slider("§8» §fLevel", 1, enchantment.maxLevel(), 1, 1);

        window.onSubmit((g, h) -> {
            this.openConfirmBuyEnchantment(player, enchantment, (int) h.getSlider(1));
        });
        window.send(player);
    }

    public void openConfirmBuyEnchantment(final Player player, final CustomEnchantments.Enchantment enchantment, final int level) {
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (this.serverCore.getCustomEnchantments().canApply(itemStack, enchantment)) {
            if (this.serverCore.getCustomEnchantments().hasEnchantment(itemStack, enchantment)) {
                final int currentLevel = this.serverCore.getCustomEnchantments().getLevel(itemStack, enchantment);
                if (currentLevel < level) {
                    final int changedLevel = level - currentLevel;
                    final double finalPrice = enchantment.price() * changedLevel;
                    final ModalWindow window = new ModalWindow.Builder("§7» §8Kaufbestätigung", "§8» §fBen §8| §7Möchtest du das Item in deiner Hand wirklich mit §9" + enchantment.enchantment() + "§7 (§9Level " + level + "§7) verzaubern?" +
                            "\nDa dein Item diese Verzauberung schon mit §9Level " + currentLevel + " §7besitzt, betragen die Kosten dafür §a$" + this.serverCore.getMoneyFormat().format(finalPrice) + " §7(§9Upgrade um " + changedLevel + " Level§7).",
                            "§8» §aKaufen", "§8» §cAbbrechen")
                            .onYes(e -> {
                                this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                                    if (money >= finalPrice) {
                                        this.serverCore.getEconomyAPI().reduceMoney(player.getName(), finalPrice);
                                        this.serverCore.getCustomEnchantments().enchantItem(player, enchantment, level);
                                        Sound.RANDOM_ANVIL_USE.playSound(player, 1, 2);
                                        player.sendMessage(Language.get("roleplay.blacksmith.enchantment.bought", enchantment.enchantment(), level, this.serverCore.getMoneyFormat().format(finalPrice)));
                                    } else {
                                        Sound.NOTE_BASS.playSound(player);
                                        player.sendMessage(Language.get("roleplay.blacksmith.enchantment.not.enough.money"));
                                    }
                                });
                            })
                            .onNo(this::openBlacksmithShop)
                            .build();
                    window.send(player);
                } else {
                    Sound.NOTE_BASS.playSound(player);
                    player.sendMessage(Language.get("roleplay.blacksmith.enchantment.level.too.high"));
                }
            } else {
                final double finalPrice = enchantment.price() * level;
                final ModalWindow window = new ModalWindow.Builder("§7» §8Kaufbestätigung", "§8» §fBen §8| §7Möchtest du das Item in deiner Hand wirklich mit §9" + enchantment.enchantment() + "§7 (§9Level " + level + "§7) verzaubern?" +
                        "\nDie Kosten dafür betragen §a$" + this.serverCore.getMoneyFormat().format(finalPrice) + "§f.",
                        "§8» §aKaufen", "§8» §cAbbrechen")
                        .onYes(e -> {
                            this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                                if (money >= finalPrice) {
                                    this.serverCore.getEconomyAPI().reduceMoney(player.getName(), finalPrice);
                                    this.serverCore.getCustomEnchantments().enchantItem(player, enchantment, level);
                                    Sound.RANDOM_ANVIL_USE.playSound(player, 1, 2);
                                    player.sendMessage(Language.get("roleplay.blacksmith.enchantment.bought", enchantment.enchantment(), level, this.serverCore.getMoneyFormat().format(finalPrice)));
                                } else {
                                    Sound.NOTE_BASS.playSound(player);
                                    player.sendMessage(Language.get("roleplay.blacksmith.enchantment.not.enough.money"));
                                }
                            });
                        })
                        .onNo(this::openBlacksmithShop)
                        .build();
                window.send(player);
            }
        } else {
            Sound.NOTE_BASS.playSound(player);
            player.sendMessage(Language.get("roleplay.blacksmith.enchantment.invalid.tool"));
        }
    }

    public void openEnchantmentInfo(final Player player) {
        final String info = """
                §8» §fBen §8| §7Hier findest du alles, was du zu meinen Verzauberungen wissen musst!


                §8» §aHolzfäller §7(Level 1-2)
                §7Diese Verzauberung kann einen Baum viel schneller abholzen als eine normale Axt! Die Level-1-Axt ist jedoch nicht so zuverlässig...
                §8» §2Passendes Item: §7Axt
                §8» §cNachteile: §7Weniger XP-Punkte

                §8» §bBohrer §7(Level 1-3)
                §7Mit dem Bohrer kannst du in einem 3x3-Feld Steine (ab Level 1), Tiefenschiefer (ab Level 2) oder Nethersteine (ab Level 3) abbauen. Damit ist das Suchen von Erzen deutlich einfacher!
                §8» §2Passendes Item: §7Spitzhacke
                §8» §cNachteile: §7Kann ausschließlich Steine, Tiefenschiefer oder Netherstein abbauen

                §8» §aSmaragdfarmer
                §7Nur mit dieser Verzauberung kannst du Smaragde abbauen. Diese sind sehr wertvoll und geben dir viel XP-Punkte.
                §8» §2Passendes Item: §7Spitzhacke

                §8» §eErfahrung §7(Level 1-4)
                §7Mit Erfahrung Level 1 erhälst du bei Erzen 25 Prozent mehr XP-Punkte, mit Level 2 75 Prozent mehr, mit Level 3 125 Prozent mehr und mit Level 4 175 Prozent mehr.
                §8» §2Passendes Item: §7Spitzhacke
                §8» §cNachteile: §7Kann nicht bei Smaragden angewendet werden

                §8» §bAderabbau §7(Level 1-4)
                §7Baue Rohstoffadern mit nur wenigen Klicks ab. Je höher das Level ist, desto mehr kann eine Ader mit einem Klick abgebaut werden.
                §8» §2Passendes Item: §7Spitzhacke
                §8» §cNachteile: §7Weniger XP-Punkte

                """;
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8Mein Angebot", info)
                .addButton("§8» §cZurück", "http://eltown.net:3000/img/ui/back.png", this::openBlacksmithShop)
                .build();
        window.send(player);
    }

    public void openRepairService(final Player player) {
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        final Damageable damageable = (Damageable) itemStack.getItemMeta();
        final double costs = (damageable.getDamage() * .29) + 60;
        if (damageable.getDamage() != 0) {
            final ModalWindow window = new ModalWindow.Builder("§7» §8Item reparieren", "§fLasse das Item in deiner Hand hier reparieren." +
                    "\n\n§fGrundgebühr: §a$60\n§fSchadensbehebung: §a$" + this.serverCore.getMoneyFormat().format(damageable.getDamage() * .29) + "\n§fBenötigte XP-Level: §a10" +
                    "\n\n§f§lZu zahlen: §r§a$" + this.serverCore.getMoneyFormat().format(costs) + " §fund §a10 XP-Level",
                    "§7» §aJetzt reparieren", "§7» §cAbbrechen")
                    .onYes(e -> {
                        if (player.getLevel() >= 10) {
                            this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                                if (money >= costs) {
                                    this.serverCore.getEconomyAPI().reduceMoney(player.getName(), costs);
                                    player.setLevel(player.getLevel() - 10);
                                    damageable.setDamage(0);
                                    itemStack.setItemMeta(damageable);
                                    player.getInventory().setItemInMainHand(itemStack);
                                    Sound.RANDOM_ANVIL_USE.playSound(player, 1, 2);
                                    player.sendMessage(Language.get("roleplay.blacksmith.repair.repaired", this.serverCore.getMoneyFormat().format(costs)));
                                } else {
                                    Sound.NOTE_BASS.playSound(player);
                                    player.sendMessage(Language.get("roleplay.blacksmith.repair.not.enough.money"));
                                }
                            });
                        } else {
                            Sound.NOTE_BASS.playSound(player);
                            player.sendMessage(Language.get("roleplay.blacksmith.repair.not.enough.xp"));
                        }
                    })
                    .onNo(this::openBlacksmithShop)
                    .build();
            window.send(player);
        } else {
            Sound.NOTE_BASS.playSound(player);
            player.sendMessage(Language.get("roleplay.blacksmith.repair.invalid.damage"));
        }
    }

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
        RoleplayListener.openQueue.remove(player.getName());
    }

}
