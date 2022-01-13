package net.eltown.servercore.components.api.intern;

import lombok.AllArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.function.Consumer;

@AllArgsConstructor
public class CoreAPI {

    private final ServerCore serverCore;

    public void proxyPlayerIsOnline(final String player, final Consumer<Boolean> isOnline) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (CoreCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL -> isOnline.accept(false);
                case CALLBACK_ONLINE -> isOnline.accept(true);
            }
        }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_IS_PLAYER_ONLINE.name(), player);
    }

}
