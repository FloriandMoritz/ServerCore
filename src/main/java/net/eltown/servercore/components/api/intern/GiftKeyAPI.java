package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.Giftkey;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.List;
import java.util.function.Consumer;

public record GiftKeyAPI(ServerCore serverCore) {

    public void createKey(final String key, final int maxUses, final String rewards, final String marks, final long duration, final Consumer<String> keyCallback) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL -> keyCallback.accept(delivery.getData()[1]);
                case CALLBACK_GIFTKEY_ALREADY_EXISTS -> keyCallback.accept(null);
            }
        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_CREATE_KEY.name(), key, String.valueOf(maxUses), rewards, marks, String.valueOf(duration));
    }

    public void getKey(final String key, final Consumer<Giftkey> giftkeyCallback) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL -> giftkeyCallback.accept(null);
                case CALLBACK_KEY -> {
                    final String[] d = delivery.getData()[1].split(">>");
                    final Giftkey giftkey = new Giftkey(d[0], Integer.parseInt(d[1]), List.of(d[3].split(">:<")), List.of(d[4].split(">:<")), List.of(d[5].split(">:<")), Long.parseLong(d[2]));
                    giftkeyCallback.accept(giftkey);
                }
            }
        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_GET_KEY.name(), key);
    }

    public void redeemKey(final Giftkey giftkey, final String player, final Consumer<GiftkeyCalls> callback) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            callback.accept(GiftkeyCalls.valueOf(delivery.getKey().toUpperCase()));
        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_REDEEM_KEY.name(), giftkey.getKey(), player);
    }

    public void deleteKey(final String key) {
        this.serverCore.getTinyRabbit().send(Queue.GIFTKEYS_RECEIVE, GiftkeyCalls.REQUEST_DELETE_KEY.name(), key);
    }

}
