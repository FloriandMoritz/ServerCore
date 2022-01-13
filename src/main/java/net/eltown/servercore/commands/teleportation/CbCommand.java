package net.eltown.servercore.commands.teleportation;

import net.eltown.servercore.ServerCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CbCommand extends Command {

    private final ServerCore serverCore;

    public CbCommand(final ServerCore serverCore) {
        super("cb", "Teleportiere dich schnell zum CityBuild", "", List.of("citybuild", "plotwelt"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.serverCore.getTeleportationAPI().getWarps().stream().filter(warp -> warp.getName().equals("CityBuild")).findFirst().ifPresent(warp ->
                    this.serverCore.getTeleportationAPI().teleportToWarp(player, warp)
            );
        }
        return true;
    }
}
