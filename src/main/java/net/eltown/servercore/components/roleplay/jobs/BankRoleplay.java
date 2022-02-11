package net.eltown.servercore.components.roleplay.jobs;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.bank.BankAccount;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.modal.ModalWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.RoleplayListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record BankRoleplay(ServerCore serverCore) {

    public void openBankLogin(final Player player) {
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.PAPER) {
            if (itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING)) {
                final String account = itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING);

                this.serverCore.getBankAPI().getAccount(account, bankAccount -> {
                    if (bankAccount != null) {
                        final CustomWindow window = new CustomWindow("§7» §8Bankkonto-Login");
                        window.form()
                                .label("§8» §fKonto: §9" + account + "\n§8» §fName: §9" + bankAccount.getDisplayName())
                                .input("§8» §fBitte gebe das Passwort des Kontos an.", "0000");

                        window.onSubmit((g, h) -> {
                            final String password = h.getInput(1);

                            if (password.equals(bankAccount.getPassword())) {
                                this.openBankAccount(player, bankAccount.getAccount());
                            } else {
                                player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                            }
                        });
                        window.send(player);
                    } else {
                        player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                    }
                });
            } else {
                player.sendMessage(Language.get("roleplay.bank.invalid.account"));
            }
        } else {
            player.sendMessage(Language.get("roleplay.bank.card.needed"));
        }
    }

    public void openBankAccount(final Player player, final String account) {
        this.serverCore.getBankAPI().getAccount(account, bankAccount -> {
            final SimpleWindow window = new SimpleWindow.Builder("§7» §8Bankkonto", "§8» §fKonto: §9" + account + "\n§8» §fName: §9" + bankAccount.getDisplayName() + "\n§8» §fInhaber: §9" + bankAccount.getOwner() + "\n\n§8» §fGuthaben: §a$" + this.serverCore.getMoneyFormat().format(bankAccount.getBalance()) + "\n")
                    .addButton("§8» §3Guthaben einzahlen", "http://45.138.50.23:3000/img/job/banker/deposit.png", e -> this.openAccountDeposit(e, bankAccount.getAccount()))
                    .addButton("§8» §3Guthaben abheben", "http://45.138.50.23:3000/img/job/banker/withdraw.png", e -> this.openAccountWithdraw(e, bankAccount.getAccount()))
                    .build();
            window.send(player);
        });
    }

    public void openAccountDeposit(final Player player, final String account) {
        this.serverCore.getBankAPI().getAccount(account, bankAccount -> {
            this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                final CustomWindow window = new CustomWindow("§7» §8Guthaben einzahlen");
                window.form()
                        .label("§8» §fKonto: §9" + account + "\n§8» §fName: §9" + bankAccount.getDisplayName() + "\n§8» §fInhaber: §9" + bankAccount.getOwner() + "\n\n§8» §fGuthaben: §a$" + this.serverCore.getMoneyFormat().format(bankAccount.getBalance()) + "\n§8» §fBargeld: §a$" + this.serverCore.getMoneyFormat().format(money) + "\n")
                        .input("§8» Bitte gebe an, wie viel Geld du auf dieses Konto einzahlen möchtest.", "3.99");

                window.onSubmit((g, h) -> {
                    try {
                        final double amount = Double.parseDouble(h.getInput(1));
                        if (amount <= 0) throw new Exception("Invalid bank interact amount");

                        if (money >= amount) {
                            this.serverCore.getEconomyAPI().reduceMoney(player.getName(), amount);
                            this.serverCore.getBankAPI().depositMoney(account, amount);
                            this.serverCore.getBankAPI().insertBankLog(account, "Geld eingezahlt", "§7" + player.getName() + " hat $" + amount + " eingezahlt.");
                            player.sendMessage(Language.get("roleplay.bank.deposit.success", this.serverCore.getMoneyFormat().format(amount), this.serverCore.getMoneyFormat().format(bankAccount.getBalance() + amount)));
                        } else {
                            player.sendMessage(Language.get("roleplay.bank.no.money"));
                        }
                    } catch (final Exception e) {
                        player.sendMessage(Language.get("roleplay.bank.invalid.input"));
                    }
                });
                window.send(player);
            });
        });
    }

    public void openAccountWithdraw(final Player player, final String account) {
        this.serverCore.getBankAPI().getAccount(account, bankAccount -> {
            final CustomWindow window = new CustomWindow("§7» §8Guthaben abheben");
            window.form()
                    .label("§8» §fKonto: §9" + account + "\n§8» §fName: §9" + bankAccount.getDisplayName() + "\n§8» §fInhaber: §9" + bankAccount.getOwner() + "\n\n§8» §fGuthaben: §a$" + this.serverCore.getMoneyFormat().format(bankAccount.getBalance()) + "\n")
                    .input("§8» Bitte gebe an, wie viel Geld du von diesem Konto abheben möchtest.", "3.99");

            window.onSubmit((g, h) -> {
                try {
                    final double amount = Double.parseDouble(h.getInput(1));
                    if (amount <= 0) throw new Exception("Invalid bank interact amount");

                    this.serverCore.getBankAPI().getAccount(account, finalBankAccount -> {
                        if (finalBankAccount.getBalance() >= amount) {
                            this.serverCore.getBankAPI().withdrawMoney(account, amount);
                            this.serverCore.getBankAPI().insertBankLog(account, "Geld abgehoben", "§7" + player.getName() + " hat $" + amount + " abgehoben.");
                            this.serverCore.getEconomyAPI().addMoney(player.getName(), amount);
                            player.sendMessage(Language.get("roleplay.bank.withdraw.success", this.serverCore.getMoneyFormat().format(amount), this.serverCore.getMoneyFormat().format(bankAccount.getBalance() - amount)));
                        } else {
                            player.sendMessage(Language.get("roleplay.bank.bank.no.money"));
                        }
                    });
                } catch (final Exception e) {
                    player.sendMessage(Language.get("roleplay.bank.invalid.input"));
                }
            });
            window.send(player);
        });
    }

    static final List<ChainMessage> jamesTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Guten Tag, §a%p§7! Bei uns ist dein Geld sicher!", 3),
            new ChainMessage("Ich mache Geld mit Geld!", 2),
            new ChainMessage("Ich kann alle Fragen zu deinem Konto beantworten.", 3),
            new ChainMessage("Dein Geld ist hier am sichersten!", 2)
    ));

    public void openBankManagerByNpc(final Player player) {
        this.smallTalk(jamesTalks, RoleplayID.JOB_BANKER.name(), player, message -> {
            if (message == null) {
                this.openBankManager(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fJames §8| §7" + message.message().replace("%p", player.getName()));
                        })
                        .append(message.seconds(), () -> {
                            this.openBankManager(player);
                            RoleplayListener.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openBankManager(final Player player) {
        final SimpleWindow window = new SimpleWindow.Builder("§7» §8Banker James", "\n\n")
                .addButton("§8» §fNeues Konto eröffnen", "http://45.138.50.23:3000/img/job/banker/create_account.png", this::openCreateBankAccount)
                .addButton("§8» §fBankkonto Beratung", "http://45.138.50.23:3000/img/job/banker/manage-account.png", this::openBankManagerService)
                .addButton("§8» §fBankkarte verloren", "http://45.138.50.23:3000/img/job/banker/forgot-password.png", this::openNewBankCard)
                .build();
        window.send(player);
    }

    public void openCreateBankAccount(final Player player) {
        final ModalWindow window = new ModalWindow.Builder("§7» §8Neues Bankkonto eröffnen", "Möchtest du wirklich ein neues Bankkonto eröffnen?\n\n§fVerwaltungsgebühr: §a$120\n§fBankkarte: §a$35\n\n§f§lZu zahlen: §r§a$155",
                "§8» §aErstellen", "§8» §cAbbrechen")
                .onYes(e -> {
                    this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                        if (money >= 155) {
                            this.serverCore.getEconomyAPI().reduceMoney(player.getName(), 155);
                            this.serverCore.getBankAPI().createBankAccount(player.getName(), "A", (password, account) -> {
                                player.sendMessage(Language.get("roleplay.bank.account.created", password));
                                this.giveBankCard(player, account);
                            });
                        } else {
                            player.sendMessage(Language.get("roleplay.bank.no.money"));
                        }
                    });
                })
                .onNo(this::openBankManager)
                .build();
        window.send(player);
    }

    public void openBankManagerService(final Player player) {
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.PAPER) {
            if (itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING)) {
                final String account = itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING);
                this.serverCore.getBankAPI().getAccount(account, bankAccount -> {
                    if (bankAccount != null) {
                        if (bankAccount.getOwner().equals(player.getName())) {
                            final SimpleWindow window = new SimpleWindow.Builder("§7» §8Service wählen", "Bitte suche dir eines der folgenden Aktionen aus, um fortzufahren.\n\n§8» §fKonto: §9" + bankAccount.getAccount() + "\n§8» §fName: §9" + bankAccount.getDisplayName())
                                    .addButton("§8» §9Kontoaktivität", "http://45.138.50.23:3000/img/job/banker/manage-account-log.png", e -> this.openBankManagerAccountLog(player, bankAccount))
                                    .addButton("§8» §9Bankkarte", "http://45.138.50.23:3000/img/job/banker/manage-account.png", e -> this.openBankManagerAccountNewCard(player, bankAccount))
                                    .addButton("§8» §9Einstellungen", "http://45.138.50.23:3000/img/job/banker/manage-account-settings.png", e -> this.openBankManagerAccountSettings(player, bankAccount))
                                    .build();
                            window.send(player);
                        } else {
                            player.sendMessage(Language.get("roleplay.bank.invalid.user"));
                        }
                    } else {
                        player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                    }
                });
            } else {
                player.sendMessage(Language.get("roleplay.bank.card.needed"));
            }
        } else {
            player.sendMessage(Language.get("roleplay.bank.card.needed"));
        }
    }

    public void openBankManagerAccountLog(final Player player, final BankAccount bankAccount) {
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Kontoaktivität", "§8» §fKonto: §9" + bankAccount.getAccount() + "\n§8» §fName: §9" + bankAccount.getDisplayName());

        bankAccount.getBankLogs().forEach(bankLog -> {
            window.addButton(bankLog.getTitle() + "\n§7" + bankLog.getDate(), e -> {
                final SimpleWindow logWindow = new SimpleWindow.Builder("§7» §8Kontoaktivität", "§fKonto: §9" + bankAccount.getAccount() + "\n§fName: §9" + bankAccount.getDisplayName() + "\n\n" +
                        "§fLogID: §1" + bankLog.getLogId() + "\n" + "§fAktion: §1" + bankLog.getTitle() + "\n" + "§fBeschreibung: §1" + bankLog.getDetails() + "\n" +
                        "§fDatum: §1" + bankLog.getDate() + "\n\n")
                        .addButton("§8» §cZurück", "http://45.138.50.23:3000/img/ui/back.png", h -> this.openBankManagerAccountLog(player, bankAccount))
                        .build();
                logWindow.send(player);
            });
        });

        window.addButton("§8» §cZurück", "http://45.138.50.23:3000/img/ui/back.png", this::openBankManagerService);
        window.build().send(player);
    }

    public void openBankManagerAccountNewCard(final Player player, final BankAccount bankAccount) {
        final ModalWindow window = new ModalWindow.Builder("§7» §8Neue Bankkarte", "§fMöchtest du für das Konto §9" + bankAccount.getAccount() + " §feine neue Bankkarte beanspruchen?" +
                "\n\n§fVerwaltungsgebühr: §a$20\n§fBankkarte: §a$35\n\n§f§lZu zahlen: §r§a$55", "§8» §aBeanspruchen", "§8» §cZurück")
                .onYes(e -> this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                    if (money >= 55) {
                        this.serverCore.getEconomyAPI().reduceMoney(player.getName(), 55);
                        this.giveBankCard(player, bankAccount.getAccount());
                        this.serverCore.getBankAPI().insertBankLog(bankAccount.getAccount(), "Neue Karte eingerichtet", "§7" + player.getName() + " hat für dieses Konto eine neue Karte einrichten lassen.");
                        player.sendMessage(Language.get("roleplay.bank.card.received"));
                    } else {
                        player.sendMessage(Language.get("roleplay.bank.no.money"));
                    }
                }))
                .onNo(this::openBankManagerService)
                .build();
        window.send(player);
    }

    public void openBankManagerAccountSettings(final Player player, final BankAccount bankAccount) {
        final CustomWindow window = new CustomWindow("§7» §8Kontoeinstellungen");
        window.form()
                .input("§8» §fAnzeigename des Kontos:", bankAccount.getAccount(), bankAccount.getDisplayName())
                .input("§8» §fPasswort des Kontos:", "0000", bankAccount.getPassword())
                .toggle("§8» §fMöchtest du das Passwort ändern? §cDas Ändern des Passworts wird sofort übernommen. Das Konto wird allerdings nicht aus aktiven Verknüpfungen getrennt.", false);

        window.onSubmit((g, h) -> {
            final String displayName = h.getInput(0);
            final String password = h.getInput(1);
            final boolean changePassword = h.getToggle(2);

            if (!bankAccount.getDisplayName().equals(displayName) && !displayName.isEmpty()) {
                this.serverCore.getBankAPI().changeDisplayName(bankAccount.getAccount(), displayName);
                this.serverCore.getBankAPI().insertBankLog(bankAccount.getAccount(), "Kontoname geändert", "§7" + player.getName() + " hat den Anzeigename des Kontos von " + bankAccount.getDisplayName() + " zu " + displayName + " geändert.");
            }

            if (changePassword) {
                if (!bankAccount.getPassword().equals(password)) {
                    try {
                        if (!(password.length() == 4)) throw new Exception("Password invalid");
                        final int check = Integer.parseInt(password);
                        this.serverCore.getBankAPI().changePassword(bankAccount.getAccount(), password);
                        this.serverCore.getBankAPI().insertBankLog(bankAccount.getAccount(), "Passwort geändert", "§7" + player.getName() + " hat das Passwort des Kontos von " + bankAccount.getPassword() + " zu " + password + " geändert.");
                    } catch (final Exception e) {
                        player.sendMessage(Language.get("roleplay.bank.settings.invalid.password"));
                        return;
                    }
                }
            }

            player.sendMessage(Language.get("roleplay.bank.settings.updated"));
        });
        window.send(player);
    }

    public void openNewBankCard(final Player player) {
        this.serverCore.getBankAPI().getBankAccountsByPlayer(player.getName(), bankAccounts -> {
            if (bankAccounts == null) {
                player.sendMessage(Language.get("roleplay.bank.no.accounts"));
                return;
            }

            final CustomWindow window = new CustomWindow("§7» §8Bankkarte verloren");
            window.form()
                    .dropdown("§8» §fBitte wähle eines deiner Konten aus, um eine neue Bankkarte zu beantragen.", bankAccounts.toArray(new String[0]));

            window.onSubmit((g, h) -> {
                final String account = bankAccounts.get(h.getDropdown(0));

                final ModalWindow acceptWindow = new ModalWindow.Builder("§7» §8Neue Bankkarte", "§fMöchtest du für das Konto §9" + account + " §feine neue Bankkarte beanspruchen?" +
                        "\n\n§fVerwaltungsgebühr: §a$20\n§fBankkarte: §a$35\n\n§f§lZu zahlen: §r§a$55", "§8» §aBeanspruchen", "§8» §cZurück")
                        .onYes(e -> {
                            this.serverCore.getEconomyAPI().getMoney(player.getName(), money -> {
                                if (money >= 55) {
                                    this.serverCore.getEconomyAPI().reduceMoney(player.getName(), 55);
                                    this.giveBankCard(player, account);
                                    this.serverCore.getBankAPI().insertBankLog(account, "Neue Karte eingerichtet", "§7" + player.getName() + " hat für dieses Konto eine neue Karte einrichten lassen.");
                                    player.sendMessage(Language.get("roleplay.bank.card.received"));
                                } else {
                                    player.sendMessage(Language.get("roleplay.bank.no.money"));
                                }
                            });
                        })
                        .onNo(this::openBankManager)
                        .build();
                acceptWindow.send(player);
            });
            window.send(player);
        });
    }

    public void giveBankCard(final Player player, final String account) {
        final ItemStack bankCard = new ItemStack(Material.PAPER, 1);
        final ItemMeta meta = bankCard.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "bank_card"), PersistentDataType.STRING, account);
        meta.displayName(Component.text("§8» §3Bankkarte"));
        meta.lore(List.of(Component.text("§7Diese Bankkarte kann an einem Bankautomaten "), Component.text("§7oder bei einem Bankmitarbeiter benutzt werden.")));
        bankCard.setItemMeta(meta);
        player.getInventory().addItem(bankCard);
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
