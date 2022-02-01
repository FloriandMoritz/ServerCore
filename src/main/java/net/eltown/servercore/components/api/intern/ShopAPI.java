package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.shops.ShopCalls;

import java.util.function.BiConsumer;

public record ShopAPI(ServerCore serverCore) {

    public void getItemPrice(final String namespaceId, final int amount, final BiConsumer<Double, Double> callback) {
        this.serverCore.getTinyRabbit().sendAndReceive((delivery) -> {
            final double buy = Double.parseDouble(delivery.getData()[1]);
            final double sell = Double.parseDouble(delivery.getData()[2]);
            callback.accept(buy * amount, sell * amount);
        }, "api.shops.callback", ShopCalls.REQUEST_ITEM_PRICE.name(), namespaceId);
    }

    public void getMinBuySell(final String namespaceId, final BiConsumer<Double, Double> callback) {
        this.serverCore.getTinyRabbit().sendAndReceive((delivery) -> {
            callback.accept(Double.parseDouble(delivery.getData()[1]), Double.parseDouble(delivery.getData()[2]));
        }, "api.shops.callback", ShopCalls.REQUEST_MIN_BUY_SELL.name(), namespaceId);
    }

    public void sendBought(final String namespaceId, final int amount) {
        this.serverCore.getTinyRabbit().send("api.shops.receive", ShopCalls.UPDATE_ITEM_BOUGHT.name(), namespaceId, amount + "");
    }

    public void sendSold(final String namespaceId, final int amount) {
        this.serverCore.getTinyRabbit().send("api.shops.receive", ShopCalls.UPDATE_ITEM_SOLD.name(), namespaceId, amount + "");
    }

    public void setPrice(final String namespaceId, final double price) {
        this.serverCore.getTinyRabbit().send("api.shops.receive", ShopCalls.UPDATE_ITEM_PRICE.name(), namespaceId, price + "");
    }

}
