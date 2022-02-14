package net.eltown.servercore.components.enchantments.listeners;

import net.eltown.servercore.components.enchantments.CustomEnchantments;
import net.eltown.servercore.listeners.SpawnProtectionListener;
import org.bukkit.Location;
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

public record DrillListener(CustomEnchantments customEnchantments) implements Listener {

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (this.customEnchantments.hasEnchantment(itemStack, CustomEnchantments.Enchantment.DRILL)) {
            if (SpawnProtectionListener.isInRadius(block.getLocation().toVector())) {
                event.setCancelled(true);
                return;
            }
            final int level = this.customEnchantments.getLevel(itemStack, CustomEnchantments.Enchantment.DRILL);
            BlockFace blockFace = player.getFacing();
            if (player.getLocation().getPitch() <= -40) blockFace = BlockFace.UP;
            if (player.getLocation().getPitch() >= 40) blockFace = BlockFace.DOWN;

            if ((block.getType() == Material.STONE && level >= 1) || (block.getType() == Material.DEEPSLATE && level >= 2) || (block.getType() == Material.NETHERRACK && level >= 3)) {
                this.breakBlocks(player, itemStack, this.getBlocks(block, blockFace.name().toUpperCase()));
            }
        }
    }

    private void breakBlocks(final Player player, final ItemStack itemStack, final List<Block> blocks) {
        final int level = this.customEnchantments.getLevel(itemStack, CustomEnchantments.Enchantment.DRILL);
        blocks.forEach(e -> {
            if (e.getType() == Material.STONE && level >= 1) {
                e.breakNaturally(itemStack);
            } else if (e.getType() == Material.DEEPSLATE && level >= 2) {
                e.breakNaturally(itemStack);
            } else if (e.getType() == Material.NETHERRACK && level >= 3) {
                e.breakNaturally(itemStack);
            }
        });
    }

    private List<Block> getBlocks(final Block block, final String face) {
        final List<Block> blocks = new ArrayList<>();
        final Location location = block.getLocation();
        switch (face) {
            case "UP", "DOWN" -> {
                blocks.add(block);
                blocks.add(location.clone().add(0.0D, 0.0D, 1.0D).getBlock());
                blocks.add(location.clone().add(0.0D, 0.0D, -1.0D).getBlock());
                blocks.add(location.clone().add(1.0D, 0.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(1.0D, 0.0D, 1.0D).getBlock());
                blocks.add(location.clone().add(1.0D, 0.0D, -1.0D).getBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, 1.0D).getBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, -1.0D).getBlock());
            }
            case "EAST", "WEST" -> {
                blocks.add(block);
                blocks.add(location.clone().add(0.0D, 0.0D, 1.0D).getBlock());
                blocks.add(location.clone().add(0.0D, 0.0D, -1.0D).getBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, 1.0D).getBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, -1.0D).getBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, 1.0D).getBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, -1.0D).getBlock());
            }
            case "NORTH", "SOUTH" -> {
                blocks.add(block);
                blocks.add(location.clone().add(1.0D, 0.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(1.0D, 1.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(-1.0D, 1.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(1.0D, -1.0D, 0.0D).getBlock());
                blocks.add(location.clone().add(-1.0D, -1.0D, 0.0D).getBlock());
            }
        }
        return blocks;
    }

}
