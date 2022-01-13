package net.eltown.servercore.commands.teleportation;

import net.eltown.servercore.ServerCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NtCommand extends Command {

    private final ServerCore serverCore;

    public NtCommand(final ServerCore serverCore) {
        super("nt", "Teleportiere dich schnell in den Nether", "", List.of("nether", "hölle"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.serverCore.getTeleportationAPI().getWarps().stream().filter(w -> w.getName().equals("Nether")).findFirst().ifPresent(w ->
                    this.serverCore.getTeleportationAPI().teleportToWarp(player, w)
            );
        }
        return true;
    }
}
