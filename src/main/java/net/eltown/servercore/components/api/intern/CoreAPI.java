package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record CoreAPI(ServerCore serverCore) {

    public void proxyPlayerIsOnline(final String player, final Consumer<Boolean> isOnline) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (CoreCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL -> isOnline.accept(false);
                case CALLBACK_ONLINE -> isOnline.accept(true);
            }
        }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_IS_PLAYER_ONLINE.name(), player);
    }

    public void proxyGetOnlinePlayers(final Consumer<List<String>> list) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (CoreCalls.valueOf(delivery.getKey().toUpperCase()) == CoreCalls.CALLBACK_GET_ONLINE_PLAYERS) {
                if (delivery.getData()[1].isEmpty()) {
                    list.accept(new ArrayList<>());
                    return;
                }
                list.accept(Arrays.stream(delivery.getData()[1].split("#")).toList());
            }
        }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_GET_ONLINE_PLAYERS.name(), "null");
    }

    public void getPlayTime(final String player, final BiConsumer<Long, Long> consumer) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            consumer.accept(Long.parseLong(delivery.getData()[1]), Long.parseLong(delivery.getData()[2]));
        }, Queue.PROXY_PLAYTIME, "REQUEST_PLAYTIME", player);
    }

}
