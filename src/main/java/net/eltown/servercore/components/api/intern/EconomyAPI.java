package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.economy.EconomyCalls;
import net.eltown.servercore.components.event.MoneyChangeEvent;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public record EconomyAPI(ServerCore serverCore) {

    public void hasAccount(final String id, final Consumer<Boolean> callback) {
        this.serverCore.getTinyRabbit().sendAndReceive((delivery -> {
            callback.accept(delivery.getData()[1].equalsIgnoreCase("true"));
        }), Queue.ECONOMY_CALLBACK, EconomyCalls.REQUEST_ACCOUNTEXISTS.name(), id);
    }

    public void createAccount(final String player) {
        this.serverCore.getTinyRabbit().send(Queue.ECONOMY_RECEIVE, EconomyCalls.REQUEST_CREATEACCOUNT.name(), player, "0");
    }

    public void getMoney(final String player, final Consumer<Double> callback) {
        CompletableFuture.runAsync(() -> {
            this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                callback.accept(Double.parseDouble(delivery.getData()[1]));
            }, Queue.ECONOMY_CALLBACK, EconomyCalls.REQUEST_GETMONEY.name(), player);
        });
    }

    public void setMoney(final String player, final double set) {
        CompletableFuture.runAsync(() -> {
            this.serverCore.getTinyRabbit().send(Queue.ECONOMY_RECEIVE, EconomyCalls.REQUEST_SETMONEY.name(), player, String.valueOf(set));
            this.callMoneyChangeEvent(player, set);
        });
    }

    public void addMoney(final String player, final double add) {
        CompletableFuture.runAsync(() -> {
            this.getMoney(player, money -> {
                final double set = money + add;
                this.serverCore.getTinyRabbit().send(Queue.ECONOMY_RECEIVE, EconomyCalls.REQUEST_SETMONEY.name(), player, String.valueOf(set));
                this.callMoneyChangeEvent(player, set);
            });
        });
    }

    public void reduceMoney(final String player, final double reduce) {
        CompletableFuture.runAsync(() -> {
            this.getMoney(player, money -> {
                final double set = money - reduce;
                this.serverCore.getTinyRabbit().send(Queue.ECONOMY_RECEIVE, EconomyCalls.REQUEST_SETMONEY.name(), player, String.valueOf(set));
                this.callMoneyChangeEvent(player, set);
            });
        });
    }

    public void getAll(final Consumer<Map<String, Double>> callback) {
        final Map<String, Double> map = new HashMap<>();
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            final List<String> list = Arrays.asList(delivery.getData());
            list.forEach((string) -> {
                if (!string.equals(delivery.getKey().toLowerCase())) {
                    map.put(string.split(":")[0], Double.parseDouble(string.split(":")[1]));
                }
            });
            callback.accept(map);
        }, Queue.ECONOMY_CALLBACK, EconomyCalls.REQUEST_GETALL.name());
    }

    private void callMoneyChangeEvent(final String player, final double change) {
        final Player online = this.serverCore.getServer().getPlayer(player);
        if (online != null) this.serverCore.getServer().getPluginManager().callEvent(new MoneyChangeEvent(online, change));
    }

}
