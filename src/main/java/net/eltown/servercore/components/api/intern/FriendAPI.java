package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record FriendAPI(ServerCore serverCore) {

    public void getFriendData(final String player, final BiConsumer<List<String>, List<String>> data) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (FriendCalls.valueOf(delivery.getKey()) == FriendCalls.CALLBACK_FRIEND_DATA) {
                List<String> friends = new ArrayList<>();
                List<String> requests = new ArrayList<>();
                if (!delivery.getData()[1].equals("null")) friends = Arrays.asList(delivery.getData()[1].split(":"));
                if (!delivery.getData()[2].equals("null")) requests = Arrays.asList(delivery.getData()[2].split(":"));

                data.accept(friends, requests);
            } else if (FriendCalls.valueOf(delivery.getKey()) == FriendCalls.CALLBACK_NULL) {
                data.accept(null, null);
            }
        }, Queue.FRIEND_CALLBACK, FriendCalls.REQUEST_FRIEND_DATA.name(), player);
    }

    public void areFriends(final String player, final String target, final Consumer<Boolean> are) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            if (FriendCalls.valueOf(delivery.getKey().toUpperCase()) == FriendCalls.CALLBACK_ARE_FRIENDS) {
                are.accept(Boolean.parseBoolean(delivery.getData()[1]));
            }
        }, Queue.FRIEND_CALLBACK, FriendCalls.REQUEST_CHECK_ARE_FRIENDS.name(), player, target);
    }

}
