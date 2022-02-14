package net.eltown.servercore.components.enchantments.listeners;

import net.eltown.servercore.components.enchantments.CustomEnchantments;
import net.eltown.servercore.listeners.SpawnProtectionListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record VeinMiningListener(CustomEnchantments customEnchantments) implements Listener {

    static List<Material> veinBlocks = new ArrayList<>(List.of(
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_QUARTZ_ORE
    ));

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        final Block block = event.getBlock();
        if (this.customEnchantments.hasEnchantment(itemStack, CustomEnchantments.Enchantment.VEIN_MINING)) {
            if (veinBlocks.contains(block.getType())) {
                if (SpawnProtectionListener.isInRadius(block.getLocation().toVector())) {
                    event.setCancelled(true);
                    return;
                }
                final int level = this.customEnchantments.getLevel(itemStack, CustomEnchantments.Enchantment.VEIN_MINING);
                int maxOres = level == CustomEnchantments.Enchantment.VEIN_MINING.maxLevel() ? 100 : (level + 1);
                this.checkAndBreak(player, block, maxOres, 0);
            }
        }
    }

    private void checkAndBreak(final Player player, final Block block, final int maxOres, int oresBroken) {
        final Block north = block.getRelative(BlockFace.NORTH);
        final Block east = block.getRelative(BlockFace.EAST);
        final Block south = block.getRelative(BlockFace.SOUTH);
        final Block west = block.getRelative(BlockFace.WEST);
        final Block down = block.getRelative(BlockFace.DOWN);
        final Block up = block.getRelative(BlockFace.UP);

        if (oresBroken >= maxOres) return;
        if (block.getType() == Material.AIR) return;

        if (north.getType() == block.getType()) {
            oresBroken++;
            north.breakNaturally(player.getInventory().getItemInMainHand());
            this.checkAndBreak(player, north, maxOres, oresBroken);
        }
        if (east.getType() == block.getType()) {
            oresBroken++;
            east.breakNaturally(player.getInventory().getItemInMainHand());
            this.checkAndBreak(player, east, maxOres, oresBroken);
        }
        if (south.getType() == block.getType()) {
            oresBroken++;
            south.breakNaturally(player.getInventory().getItemInMainHand());
            this.checkAndBreak(player, south, maxOres, oresBroken);
        }
        if (west.getType() == block.getType()) {
            oresBroken++;
            west.breakNaturally(player.getInventory().getItemInMainHand());
            this.checkAndBreak(player, west, maxOres, oresBroken);
        }
        if (down.getType() == block.getType()) {
            oresBroken++;
            down.breakNaturally(player.getInventory().getItemInMainHand());
            this.checkAndBreak(player, down, maxOres, oresBroken);
        }
        if (up.getType() == block.getType()) {
            oresBroken++;
            up.breakNaturally(player.getInventory().getItemInMainHand());
            this.checkAndBreak(player, up, maxOres, oresBroken);
        }
    }

}
