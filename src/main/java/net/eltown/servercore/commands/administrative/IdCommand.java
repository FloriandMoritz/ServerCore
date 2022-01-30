package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class IdCommand extends Command {

    private final ServerCore serverCore;

    public IdCommand(final ServerCore serverCore) {
        super("id");
        this.serverCore = serverCore;
        this.setPermission("core.command.id");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            final ItemStack itemStack = player.getInventory().getItemInMainHand();

            if (itemStack.getType() == Material.AIR) {
                player.sendMessage("§8» §fCore §8| §7Bitte halte ein Item in deiner Hand.");
                return true;
            }

            player.sendMessage("§8» §fCore §8| §7Das Item in deiner Hand heißt: §a" + itemStack.getType().name());
        }
        return true;
    }
}
