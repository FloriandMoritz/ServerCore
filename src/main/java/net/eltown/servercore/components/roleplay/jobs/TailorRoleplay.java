package net.eltown.servercore.components.roleplay.jobs;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.RoleplayListener;
import net.eltown.servercore.utils.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record TailorRoleplay(ServerCore serverCore) {

    static final List<ChainMessage> karlTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hi, §a%p§7! Du siehst heute aber gut aus!", 3),
            new ChainMessage("Jedes Stück ist ein Einzelstück!", 2),
            new ChainMessage("In meinen Klamotten gibt es keine Motten!", 2),
            new ChainMessage("Ich liebe bunte Farben!", 2)
    ));

    public void openKarlByNpc(final Player player) {
        this.smallTalk(karlTalks, RoleplayID.JOB_TAILOR.name(), player, message -> {
            if (message == null) {
                this.openKarl(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fKarl §8| §7" + message.message().replace("%p", player.getName()));
                        })
                        .append(message.seconds(), () -> {
                            this.openKarl(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    private void openKarl(final Player player) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8Schneider Karl", "§8» §fKarl §8| §7Was kann ich für dich tun? Zum Glück habe ich noch alle Stoffe in verschiedenen Farben da!")
                .addButton("§8» §fKopfbedeckungen", "http://eltown.net:3000/img/job/tailor/01.png", e -> this.openSelectColor(player, Material.LEATHER_HELMET, 14.99))
                .addButton("§8» §fOberbekleidung", "http://eltown.net:3000/img/job/tailor/02.png", e -> this.openSelectColor(player, Material.LEATHER_CHESTPLATE, 27.99))
                .addButton("§8» §fHosen", "http://eltown.net:3000/img/job/tailor/03.png", e -> this.openSelectColor(player, Material.LEATHER_LEGGINGS, 22.99))
                .addButton("§8» §fSchuhe", "http://eltown.net:3000/img/job/tailor/04.png", e -> this.openSelectColor(player, Material.LEATHER_BOOTS, 19.99))
                .build();
        window.send(player);
    }

    public void openSelectColor(final Player player, final Material material, final double price) {
        final List<String> colors = new ArrayList<>(List.of("Weiß", "Silber", "Grau", "Schwarz", "Rot", "Kastanienbraun", "Gelb", "Olivgrün", "Limettengrün", "Grün", "Aquamarin", "Türkis",
                "Blau", "Marineblau", "Fuchsienfarbig", "Lila", "Orange"));
        final CustomWindow window = new CustomWindow("§7» §8Schneider Karl");
        window.form()
                .label("§8» §f" + player.getName() + " §8| §7Das sieht recht Interessant aus...")
                .dropdown("§8» §fKarl §8| §7Tolle Auswahl! Ich habe noch einige Farben auf Lager.", colors.toArray(new String[0]));

        window.onSubmit((g, h) -> {
            final ItemStack itemStack = new ItemStack(material, 1);
            final LeatherArmorMeta meta = (LeatherArmorMeta) itemStack.getItemMeta();
            meta.setColor(this.getColor(colors.get(h.getDropdown(1))));
            meta.lore(new ArrayList<>(List.of(Component.text("§6Karl's §fOriginal"))));
            itemStack.setItemMeta(meta);
            this.openConfirmBuy(player, itemStack, price);
        });
        window.send(player);
    }

    public void openConfirmBuy(final Player player, final ItemStack itemStack, final double price) {
        final ModalWindow window = new ModalWindow.Builder("§7» §8Schneider Karl", "§8» §fKarl §8| §7Möchtest du dieses Meisterwerk für §9$" + this.serverCore.getMoneyFormat().format(price) + " §7kaufen?" +
                "\n\n§cDieser Artikel ist vom Umtausch ausgeschlossen!",
                "§8» §aKaufen", "§8» §cAbbrechen")
                .onYes(e -> {
                    if (this.serverCore.canAddItem(player.getInventory(), itemStack)) {
                        this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                            if (money >= price) {
                                this.serverCore.getEconomyAPI().reduceMoney(player.getName(), price);
                                player.getInventory().addItem(itemStack);
                                Sound.RANDOM_LEVELUP.playSound(player, 1, 3);
                                player.sendMessage("§8» §fKarl §8| §7Vielen Dank für das Geschäft! Auf Wiedersehen!");
                            } else {
                                Sound.NOTE_BASS.playSound(player);
                                player.sendMessage("§8» §fKarl §8| §7Für diese Qualität muss man auch ein wenig Geld auf den Tisch legen.");
                            }
                        });
                    } else {
                        Sound.NOTE_BASS.playSound(player);
                        player.sendMessage("§8» §fKarl §8| §7Oh, du trägst viel mit dir herum. Komm gleich am besten wieder, wenn du ein paar deiner Sachen abgelegt hast.");
                    }
                })
                .onNo(this::openKarl)
                .build();
        window.send(player);
    }

    private Color getColor(final String color) {
        return switch (color) {
            case "Silber" -> Color.SILVER;
            case "Grau" -> Color.GRAY;
            case "Schwarz" -> Color.BLACK;
            case "Rot" -> Color.RED;
            case "Kastanienbraun" -> Color.MAROON;
            case "Gelb" -> Color.YELLOW;
            case "Olivgrün" -> Color.OLIVE;
            case "Limettengrün" -> Color.LIME;
            case "Grün" -> Color.GREEN;
            case "Aquamarin" -> Color.AQUA;
            case "Türkis" -> Color.TEAL;
            case "Blau" -> Color.BLUE;
            case "Marineblau" -> Color.NAVY;
            case "Fuchsienfarbig" -> Color.FUCHSIA;
            case "Lila" -> Color.PURPLE;
            case "Orange" -> Color.ORANGE;
            default -> Color.WHITE;
        };
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
        RoleplayListener.openQueue.add(player.getName());
    }

}
