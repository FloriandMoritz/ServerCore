package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PrintItemCommand extends Command {

    private final ServerCore serverCore;

    public PrintItemCommand(final ServerCore serverCore) {
        super("printitem");
        this.serverCore = serverCore;
        this.setDescription("Lasse das Item in deiner Hand dir ausdrucken");
        this.setPermission("core.command.printitem");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player player) {
            this.serverCore.getLogger().info("[PRINT-ITEM] Printed ItemStack by " + player.getName() + ": " + SyncAPI.ItemAPI.itemStackToBase64(player.getInventory().getItemInMainHand()));
            player.sendMessage("Item ausgedruckt. Du kannst die Daten in der Konsole einsehen.");
        }
        return true;
    }
}
