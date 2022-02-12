package net.eltown.servercore.components.roleplay.jobs;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.RoleplayListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record CookRoleplay(ServerCore serverCore) {

    static final List<ChainMessage> cookTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Guten Tag, §a%p§7! Ich habe sehr leckere Gerichte im Angebot!", 5),
            new ChainMessage("Heute gibt es einen leckeren Mittagstisch!", 3),
            new ChainMessage("Lass dich nicht von mir stören!", 2),
            new ChainMessage("Mhhhh, das duftet gut!", 2),
            new ChainMessage("Treue Gäste gefallen mir!", 2),
            new ChainMessage("Was darf es heute sein?", 2),
            new ChainMessage("Soll ich etwas für dich aussuchen?", 2)
    ));

    public void openCookByNpc(final Player player) {
        this.smallTalk(cookTalks, RoleplayID.JOB_COOK.name(), player, message -> {
            if (message == null) {
                this.openCook(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fDuke §8| §7" + message.message().replace("%p", player.getName()));
                        })
                        .append(message.seconds(), () -> {
                            this.openCook(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openCook(final Player player) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8Koch Duke", "§8» §7Wähle eines der aufgelisteten Gerichte aus, welches du bestellen möchtest.")
                .addButton("Kartoffeln und Steak mit Beilage\n§3§lMenü 1   §r§a$9.95", "http://45.138.50.23:3000/img/job/cook/01.png", e -> {
                    this.order(player, 9.95, new ItemStack(Material.COOKED_BEEF, 1), new ItemStack(Material.BAKED_POTATO, 2), new ItemStack(Material.CARROT, 1));
                })
                .addButton("Gemüseteller\n§3§lMenü 2   §r§a$6,95", "http://45.138.50.23:3000/img/job/cook/02.png", e -> {
                    this.order(player, 6.95, new ItemStack(Material.BEETROOT, 1), new ItemStack(Material.BAKED_POTATO, 2), new ItemStack(Material.CARROT, 1));
                })
                .addButton("Pilzsuppe mit Brot\n§3§lMenü 3   §r§a$7,95", "http://45.138.50.23:3000/img/job/cook/03.png", e -> {
                    this.order(player, 7.95, new ItemStack(Material.MUSHROOM_STEW, 1), new ItemStack(Material.BREAD, 2));
                })
                .addButton("Rote Beete Suppe mit Brot\n§3§lMenü 4   §r§a$7,95", "http://45.138.50.23:3000/img/job/cook/04.png", e -> {
                    this.order(player, 7.95, new ItemStack(Material.BEETROOT_SOUP, 1), new ItemStack(Material.BREAD, 2));
                })
                .addButton("Haseneintopf mit Brot\n§3§lMenü 5   §r§a$8,49", "http://45.138.50.23:3000/img/job/cook/05.png", e -> {
                    this.order(player, 8.95, new ItemStack(Material.RABBIT_STEW, 1), new ItemStack(Material.BREAD, 2));
                })
                .addButton("Wasser 0,33l\n§3§lGetränk   §r§a$2,49", "http://45.138.50.23:3000/img/job/cook/06.png", e -> {
                    this.order(player, 2.49, new ItemStack(Material.POTION, 1));
                })
                .build();
        window.send(player);
    }

    private void order(final Player player, final double price, final ItemStack... itemStacks) {
        for (final ItemStack itemStack : itemStacks) {
            if (!this.serverCore.canAddItem(player.getInventory(), itemStack)) {
                player.sendMessage(Language.get("roleplay.job.cook.item.inventory.full"));
                return;
            }
        }

        this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
            if (money >= price) {
                this.serverCore.getEconomyAPI().reduceMoney(player.getName(), price);
                player.getInventory().addItem(itemStacks);
                player.sendMessage(Language.get("roleplay.job.cook.item.bought", this.serverCore.getMoneyFormat().format(price)));
            } else {
                player.sendMessage(Language.get("roleplay.job.cook.item.not.enough.money"));
            }
        });
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
