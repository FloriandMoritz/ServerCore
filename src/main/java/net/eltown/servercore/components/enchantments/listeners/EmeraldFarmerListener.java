package net.eltown.servercore.components.enchantments.listeners;

import net.eltown.servercore.components.enchantments.CustomEnchantments;
import net.eltown.servercore.listeners.SpawnProtectionListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public record EmeraldFarmerListener(CustomEnchantments customEnchantments) implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (block.getType() == Material.EMERALD_ORE || block.getType() == Material.DEEPSLATE_EMERALD_ORE) {
            if (this.customEnchantments.hasEnchantment(itemStack, CustomEnchantments.Enchantment.EMERALD_FARMER)) {
                if (SpawnProtectionListener.isInRadius(block.getLocation().toVector())) {
                    event.setCancelled(true);
                    return;
                }
                if (!this.customEnchantments.serverCore().getServerName().equals("server-1")) {
                    this.customEnchantments.serverCore().getLevelAPI().addExperience(player, 100);
                }
            } else {
                player.sendActionBar("§8» §fFür das Abbauen wird die Verzauberung §9Smaragdfarmer §fbenötigt.");
                event.setCancelled(true);
            }
        }
    }

}
