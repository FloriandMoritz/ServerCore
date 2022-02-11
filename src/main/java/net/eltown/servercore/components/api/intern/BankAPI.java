package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.bank.BankAccount;
import net.eltown.servercore.components.data.bank.BankCalls;
import net.eltown.servercore.components.data.bank.BankLog;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record BankAPI(ServerCore serverCore) {

    public void createBankAccount(final String owner, final String prefix, final BiConsumer<String, String> callbackData) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (BankCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_CREATE_ACCOUNT -> callbackData.accept(delivery.getData()[1], delivery.getData()[2]);
            }
        }, Queue.BANK_CALLBACK, BankCalls.REQUEST_CREATE_ACCOUNT.name(), owner, prefix);
    }

    public void insertBankLog(final String account, final String title, final String details) {
        this.serverCore.getTinyRabbit().send(Queue.BANK_RECEIVE, BankCalls.REQUEST_INSERT_LOG.name(), account, title, details);
    }

    public void getAccount(final String account, final Consumer<BankAccount> bankAccountConsumer) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (BankCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_GET_BANK_ACCOUNT -> {
                    final String rawLogs = delivery.getData()[6];
                    final String[] rawFullLog = rawLogs.split("--");
                    final List<BankLog> logs = new ArrayList<>();
                    for (final String s : rawFullLog) {
                        final String[] log = s.split(";");
                        logs.add(new BankLog(log[0], log[1], log[2], log[3]));
                    }
                    bankAccountConsumer.accept(new BankAccount(delivery.getData()[1], delivery.getData()[2], delivery.getData()[3], delivery.getData()[4], Double.parseDouble(delivery.getData()[5]), logs));
                }
                case CALLBACK_NULL -> bankAccountConsumer.accept(null);
            }
        }, Queue.BANK_CALLBACK, BankCalls.REQUEST_GET_BANK_ACCOUNT.name(), account);
    }

    public void withdrawMoney(final String account, final double amount) {
        this.serverCore.getTinyRabbit().send(Queue.BANK_RECEIVE, BankCalls.REQUEST_WITHDRAW_MONEY.name(), account, String.valueOf(amount));
    }

    public void depositMoney(final String account, final double amount) {
        this.serverCore.getTinyRabbit().send(Queue.BANK_RECEIVE, BankCalls.REQUEST_DEPOSIT_MONEY.name(), account, String.valueOf(amount));
    }

    public void setMoney(final String account, final double amount) {
        this.serverCore.getTinyRabbit().send(Queue.BANK_RECEIVE, BankCalls.REQUEST_SET_MONEY.name(), account, String.valueOf(amount));
    }

    public void changePassword(final String account, final String password) {
        this.serverCore.getTinyRabbit().send(Queue.BANK_RECEIVE, BankCalls.REQUEST_CHANGE_PASSWORD.name(), account, password);
    }

    public void changeDisplayName(final String account, final String displayName) {
        this.serverCore.getTinyRabbit().send(Queue.BANK_RECEIVE, BankCalls.REQUEST_CHANGE_DISPLAY_NAME.name(), account, displayName);
    }

    public void getBankAccountsByPlayer(final String player, final Consumer<List<String>> consumer) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (delivery.getData()[1].equals("null")) consumer.accept(null);
            else consumer.accept(Arrays.asList(delivery.getData()[1].split("#")));
        }, Queue.BANK_CALLBACK, BankCalls.REQUEST_BANKACCOUNTS_BY_PLAYER.name(), player);
    }

}
