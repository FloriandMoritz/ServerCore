package net.eltown.servercore.components.enchantments.listeners;

import net.eltown.servercore.components.enchantments.CustomEnchantments;
import net.eltown.servercore.listeners.SpawnProtectionListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public record LumberjackListener(CustomEnchantments customEnchantments) implements Listener {

    @EventHandler
    public void on(final BlockBreakEvent event) {
        if (this.customEnchantments.serverCore().getServerName().equals("server-1")) return;
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (this.customEnchantments.hasEnchantment(itemStack, CustomEnchantments.Enchantment.LUMBERJACK)) {
            if (SpawnProtectionListener.isInRadius(block.getLocation().toVector())) {
                event.setCancelled(true);
                return;
            }

            final int level = this.customEnchantments.getLevel(itemStack, CustomEnchantments.Enchantment.LUMBERJACK);

            if (woods.contains(block.getType())) {
                final LinkedList<Block> blocks = new LinkedList<>();

                int offset = 1;
                while (woods.contains(this.getOffset(block, offset).getType())) {
                    if (level == 1 && blocks.size() < 3) blocks.add(this.getOffset(block, offset));
                    else if (level == 2) blocks.add(this.getOffset(block, offset));
                    offset++;
                }

                if (blocks.size() > 1 && this.isTree(block.getWorld(), blocks.get(blocks.size() - 1).getLocation())) {
                    offset = -1;
                    while (isWood(this.getOffset(block, offset))) {
                        if (level == 1 && blocks.size() < 3) blocks.add(this.getOffset(block, offset));
                        else if (level == 2) blocks.add(this.getOffset(block, offset));
                        offset--;
                    }

                    blocks.forEach(e -> {
                        if (woods.contains(e.getType())) {
                            e.breakNaturally(itemStack);
                        }
                    });
                }
            }
        }
    }

    private static final List<Material> woods = new ArrayList<>(List.of(Material.OAK_LOG, Material.BIRCH_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG, Material.SPRUCE_LOG, Material.CRIMSON_STEM, Material.WARPED_STEM));
    private static final List<Material> leaves = new ArrayList<>(List.of(Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES, Material.SPRUCE_LEAVES, Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES));

    private Block getOffset(final Block block, final int yOffset) {
        return block.getWorld().getBlockAt(block.getX(), block.getY() + yOffset, block.getZ());
    }

    private boolean isLeaf(final Block block) {
        return leaves.contains(block.getType());
    }

    private boolean isWood(final Block block) {
        return woods.contains(block.getType());
    }

    private boolean isTree(final World world, final Location woodCrown) {
        if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX(), woodCrown.getBlockY() + 1, woodCrown.getBlockZ()))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX() + 1, woodCrown.getBlockY(), woodCrown.getBlockZ()))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX() + 1, woodCrown.getBlockY(), woodCrown.getBlockZ() - 1))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX() + 1, woodCrown.getBlockY(), woodCrown.getBlockZ() + 1))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX(), woodCrown.getBlockY(), woodCrown.getBlockZ()))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX(), woodCrown.getBlockY(), woodCrown.getBlockZ() + 1))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX(), woodCrown.getBlockY(), woodCrown.getBlockZ() - 1))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX() - 1, woodCrown.getBlockY(), woodCrown.getBlockZ() + 1))) return true;
        else if (this.isLeaf(world.getBlockAt(woodCrown.getBlockX() - 1, woodCrown.getBlockY(), woodCrown.getBlockZ() - 1))) return true;
        else return this.isLeaf(world.getBlockAt(woodCrown.getBlockX() - 1, woodCrown.getBlockY(), woodCrown.getBlockZ()));
    }

}
