package net.eltown.servercore.components.forms.simple;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.impl.SimpleFormImpl;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.cumulus.util.impl.FormImageImpl;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SimpleWindow {

    private final LinkedHashMap<ElementButton, Consumer<Player>> buttons;
    private final String title, content;
    private final Consumer<Player> closeCallback;

    public SimpleWindow(Builder b) {
        this.buttons = b.buttons;
        this.title = b.title;
        this.content = b.content;
        this.closeCallback = b.closeCallback;
    }

    public void send(Player player) {
        final FloodgatePlayer session = FloodgateApi.getInstance().getPlayer(player.getUniqueId());

        if (session != null) {
            final SimpleFormImpl.Builder form = new SimpleFormImpl.Builder();

            form.title(this.title);
            form.content(this.content);

            buttons.keySet().forEach((button) -> {
                if (button.image.isEmpty()) form.button(button.text);
                else form.button(button.text, new FormImageImpl(FormImage.Type.URL, button.image));
            });

            form.responseHandler((f, data) -> {
                SimpleFormResponse response = f.parseResponse(data);
                if (!response.isClosed() && response.isCorrect()) {
                    int i = 0;

                    for (Map.Entry<ElementButton, Consumer<Player>> entry : this.buttons.entrySet()) {
                        if (i != response.getClickedButtonId()) {
                            i++;
                            continue;
                        }
                        entry.getValue().accept(player);
                        return;
                    }
                } else this.setClosed(player);

            });

            session.sendForm(form.build());
        } else throw new IllegalArgumentException("Can't send SimpleForm to a non-bedrock player!");
    }

    public void setClosed(Player player) {
        if (closeCallback == null) return;
        closeCallback.accept(player);
    }

    public HashMap<ElementButton, Consumer<Player>> getButtons() {
        return buttons;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public static class Builder {

        private final LinkedHashMap<ElementButton, Consumer<Player>> buttons = new LinkedHashMap<>();
        private final String title, content;
        private Consumer<Player> closeCallback;


        public Builder(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public Builder onClose(Consumer<Player> close) {
            closeCallback = close;
            return this;
        }

        public Builder addButton(String text, Consumer<Player> callback) {
            buttons.put(new ElementButton(text, ""), callback);
            return this;
        }

        public Builder addButton(String text, String image, Consumer<Player> callback) {
            buttons.put(new ElementButton(text, image), callback);
            return this;
        }

        public SimpleWindow build() {
            return new SimpleWindow(this);
        }

    }

    private record ElementButton(String text, String image) { }

}
