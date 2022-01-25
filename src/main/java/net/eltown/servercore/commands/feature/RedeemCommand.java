package net.eltown.servercore.commands.feature;

import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RedeemCommand extends Command {

    final ServerCore serverCore;

    public RedeemCommand(final ServerCore serverCore) {
        super("redeem", "", "", List.of("einlösen", "gutschein", "code", "key"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            final CustomWindow redeemWindow = new CustomWindow("§7» §8Key einlösen");
            redeemWindow.form()
                    .input("§8» §fBitte gebe einen Code an, um diesen einzulösen.", "XXXXXXX");

            redeemWindow.onSubmit((g, h) -> {
                final String key = h.getInput(0);
                if (key.isEmpty()) {
                    player.sendMessage(Language.get("giftkey.invalid.input"));
                    return;
                }

                this.serverCore.getGiftKeyAPI().getKey(key, giftkey -> {
                    if (giftkey == null) {
                        player.sendMessage(Language.get("giftkey.invalid.key", key));
                    } else {
                        if (giftkey.getUses().contains(player.getName())) {
                            player.sendMessage(Language.get("giftkey.already.redeemed"));
                            return;
                        }

                        final ModalWindow confirmWindow = new ModalWindow.Builder("§7» §8Key einlösen", "Möchtest du diesen Key einlösen und die Belohnungen, " +
                                "die dahinter stecken erhalten? Jeder Key kann nur einmal von dir eingelöst werden.\nBitte achte außerdem darauf, dass du genügend freie" +
                                " Inventarplätze hast. Es werden keine Items erstattet, wenn diese nicht zum Inventar hinzugefügt werden konnten!",
                                "§8» §aEinlösen", "§8» §cAbbrechen")
                                .onYes(e -> {
                                    this.serverCore.getGiftKeyAPI().redeemKey(giftkey, player.getName(), giftkeyCalls -> {
                                        switch (giftkeyCalls) {
                                            case CALLBACK_ALREADY_REDEEMED -> {
                                                player.sendMessage(Language.get("giftkey.already.redeemed"));
                                            }
                                            case CALLBACK_NULL -> {
                                                player.sendMessage(Language.get("giftkey.invalid.key", key));
                                            }
                                            case CALLBACK_REDEEMED -> {
                                                giftkey.getRewards().forEach(reward -> {
                                                    final String[] rawReward = reward.split(";");
                                                    switch (rawReward[0]) {
                                                        case "item" -> {
                                                            final ItemStack itemStack = SyncAPI.ItemAPI.itemStackFromBase64(rawReward[1]);
                                                            player.getInventory().addItem(itemStack);
                                                            player.sendMessage(Language.get("giftkey.reward.item", itemStack.getI18NDisplayName(), itemStack.getAmount()));
                                                        }
                                                        case "money" -> {
                                                            final double money = Double.parseDouble(rawReward[1]);
                                                            Economy.getAPI().addMoney(player.getName(), money);
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
                                                            player.sendMessage(Language.get("giftkey.reward.crate", crate, i));
                                                        }
                                                        default -> player.sendMessage("§cBeim Einlösen des Gutscheins §8[§7" + giftkey.getKey() + "§8] §ctrat ein Fehler auf. §7[§f" + player.getName() + ", " + rawReward[0] + "§7]");
                                                    }
                                                });
                                            }
                                        }
                                    });
                                })
                                .onNo(e -> {})
                                .build();
                        confirmWindow.send(player);
                    }
                });
            });
            redeemWindow.send(player);
        }
        return false;
    }
}
