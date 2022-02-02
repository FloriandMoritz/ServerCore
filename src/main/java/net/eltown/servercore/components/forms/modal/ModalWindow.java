package net.eltown.servercore.components.forms.modal;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.impl.ModalFormImpl;
import org.geysermc.cumulus.response.ModalFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.function.Consumer;

public class ModalWindow {

    private final String title, content, yes, no;
    private final Consumer<Player> onYes, onNo, closeCallback;

    public ModalWindow(Builder b) {
        this.title = b.title;
        this.content = b.content;
        this.yes = b.yes;
        this.no = b.no;
        this.onYes = b.onYes;
        this.onNo = b.onNo;
        this.closeCallback = b.closeCallback;
    }

    public void send(Player player) {
        final FloodgatePlayer session = FloodgateApi.getInstance().getPlayer(player.getUniqueId());

        if (session != null) {

            ModalFormImpl.Builder form = new ModalFormImpl.Builder();

            form.title(this.title);
            form.content(this.content);
            form.button1(yes);
            form.button2(no);

            form.responseHandler((mf, formData) -> {
                ModalFormResponse response = mf.parseResponse(formData);

                if (!response.isClosed() && response.isCorrect()) {
                    if (response.getResult()) this.setYes(player);
                    else this.setNo(player);
                } else this.setClosed(player);
            });

            session.sendForm(form.build());

        } else throw new IllegalArgumentException("Can't send SimpleForm to a non-bedrock player!");
    }

    public void setYes(Player player) {
        onYes.accept(player);
    }

    public void setNo(Player player) {
        onNo.accept(player);
    }

    public void setClosed(Player player) {
        if (closeCallback == null) return;
        closeCallback.accept(player);
    }

    public String getYes() {
        return yes;
    }

    public String getNo() {
        return no;
    }

    public static class Builder {

        private final String title, content, yes, no;
        private Consumer<Player> onYes, onNo, closeCallback;

        public Builder(String title, String content, String yes, String no) {
            this.title = title;
            this.content = content;
            this.yes = yes;
            this.no = no;
        }

        public Builder onYes(Consumer<Player> cb) {
            onYes = cb;
            return this;
        }

        public Builder onNo(Consumer<Player> cb) {
            onNo = cb;
            return this;
        }

        public Builder onClose(Consumer<Player> cb) {
            closeCallback = cb;
            return this;
        }

        public ModalWindow build() {
            return new ModalWindow(this);
        }
    }

}
