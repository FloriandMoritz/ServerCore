package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpeedCommand extends Command {

    private final ServerCore serverCore;

    public SpeedCommand(final ServerCore serverCore) {
        super("speed");
        this.serverCore = serverCore;
        this.setPermission("core.command.speed");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length <= 1) {
            if (sender instanceof Player player) {
                if (args.length == 0) {
                    sender.sendMessage(Language.get("speed.usage"));
                    return true;
                }

                try {
                    final float speed = Float.parseFloat(args[0]);
                    if (speed > 0) {
                        player.setFlySpeed(speed / 10);
                        player.setWalkSpeed(speed / 10);
                        player.sendMessage(Language.get("speed.adjusted", speed));
                    } else sender.sendMessage(Language.get("speed.invalid.number"));
                } catch (Exception e) {
                    sender.sendMessage(Language.get("speed.invalid.number"));
                }
            }
        } else {
            if (sender.hasPermission("core.command.speed.other")) {
                try {
                    final float speed = Float.parseFloat(args[0]);
                    if (speed > 0) {
                        final Player player = this.serverCore.getServer().getPlayer(args[1]);

                        if (player != null) {
                            player.setFlySpeed(speed / 10);
                            player.setWalkSpeed(speed / 10);
                            player.sendMessage(Language.get("speed.adjusted", speed));
                            sender.sendMessage(Language.get("speed.set", player.getName(), speed));
                        } else sender.sendMessage(Language.get("speed.pnf"));

                    } else sender.sendMessage(Language.get("speed.invalid.number"));
                } catch (Exception e) {
                    sender.sendMessage(Language.get("speed.invalid.number"));
                }
            }
        }
        return false;
    }
}
