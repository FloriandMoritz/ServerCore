package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SpawnProtectionCommand extends Command {

    private final ServerCore serverCore;

    public static int spawnProtection;

    public SpawnProtectionCommand(final ServerCore serverCore) {
        super("spawnprotection");
        this.serverCore = serverCore;
        this.setPermission("core.command.spawnprotection");

        spawnProtection = this.serverCore.getConfig().getInt("spawnProtection", 0);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length > 0) {
            try {
                final int radius = Integer.parseInt(args[0]);
                this.serverCore.getConfig().set("spawnProtection", radius);
                this.serverCore.saveConfig();
                spawnProtection = radius;
                sender.sendMessage("Der Spawnradius wurde auf " + radius + " geändert.");
            } catch (Exception ex) {
                sender.sendMessage("Bitte gebe einen gültigen Radius an.");
            }
        } else sender.sendMessage("/spawnprotection <radius>");
        return false;
    }
}
