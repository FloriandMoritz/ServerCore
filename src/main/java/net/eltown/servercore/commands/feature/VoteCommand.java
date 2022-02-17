package net.eltown.servercore.commands.feature;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.utils.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class VoteCommand extends Command {

    private final ServerCore serverCore;

    public VoteCommand(final ServerCore serverCore) {
        super("vote", "", "", List.of("abstimmen", "belohnung"));
        this.serverCore = serverCore;
    }

    final Cooldown cooldown = new Cooldown(TimeUnit.SECONDS.toMillis(5));

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (!this.cooldown.hasCooldown(player.getName())) {
                this.serverCore.getVote(player.getName(), code -> {
                    switch (code) {
                        case "0" -> {
                            player.sendMessage(Language.get("vote.not.voted"));
                            Sound.NOTE_BASS.playSound(player);
                        }
                        case "1" -> this.serverCore.getGiftKeyAPI().createKey(this.serverCore.createId(6), 1, "levelxp;250>:<money;50>:<crate;common;2>:<crate;uncommon;1", player.getName(), (72 * 3600000L) + System.currentTimeMillis(), key -> {
                            player.sendMessage(Language.get("vote.successful.voted", key));
                            Sound.RANDOM_LEVELUP.playSound(player, 1, 2);
                            this.serverCore.setVoted(player.getName());
                        });
                        case "2" -> {
                            player.sendMessage(Language.get("vote.already.voted"));
                            Sound.NOTE_BASS.playSound(player);
                        }
                    }
                });
            } else {
                player.sendMessage(Language.get("vote.do.not.spam"));
                Sound.NOTE_BASS.playSound(player);
            }
        }
        return true;
    }
}
