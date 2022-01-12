package net.eltown.servercore.components.forms.custom;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.impl.CustomFormImpl;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CustomWindow {


    private final CustomFormImpl.Builder builder;
    private Consumer<Player> closeCallback;
    private BiConsumer<Player, CustomFormResponse> submitCallback;


    public CustomWindow(String title) {
        this.builder = new CustomFormImpl.Builder();
        this.builder.title(title);
    }

    public CustomFormImpl.Builder form() {
        return this.builder;
    }

    public void onClose(Consumer<Player> closeCallback) {
        this.closeCallback = closeCallback;
    }

    public void onSubmit(BiConsumer<Player, CustomFormResponse> submitCallback) {
        this.submitCallback = submitCallback;
    }

    public void send(Player player) {
        final FloodgatePlayer session = FloodgateApi.getInstance().getPlayer(player.getUniqueId());

        if (session != null) {

            this.builder.responseHandler((cf, formData) -> {
                final CustomFormResponse response = cf.parseResponse(formData);

                if (!response.isClosed() && response.isCorrect()) {
                    this.setSubmitted(player, response);
                } else this.setClosed(player);
            });

            session.sendForm(this.builder.build());

        } else throw new IllegalArgumentException("Can't send SimpleForm to a non-bedrock player!");
    }

    public void setClosed(Player player) {
        if (closeCallback == null) return;
        closeCallback.accept(player);
    }

    public void setSubmitted(Player player, CustomFormResponse form) {
        submitCallback.accept(player, form);
    }

}
