package net.eltown.servercore.listeners;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class LevelListener implements Listener {

    private final ServerCore serverCore;
    private final boolean allowXp;
    private final HashMap<Material, Double> blockXp = new HashMap<>();
    private final HashMap<EntityType, Double> entityXp = new HashMap<>();

    public LevelListener(final ServerCore serverCore, final boolean allowXp) {
        this.serverCore = serverCore;
        this.allowXp = allowXp;

        this.blockXp.put(Material.OAK_LOG, 1.5);
        this.blockXp.put(Material.BIRCH_LOG, 1.5);
        this.blockXp.put(Material.SPRUCE_LOG, 1.5);
        this.blockXp.put(Material.DARK_OAK_LOG, 1.5);
        this.blockXp.put(Material.ACACIA_LOG, 1.5);
        this.blockXp.put(Material.JUNGLE_LOG, 1.5);
        this.blockXp.put(Material.CRIMSON_STEM, 1.75);
        this.blockXp.put(Material.WARPED_STEM, 1.75);
        this.blockXp.put(Material.COAL_ORE, 1.7);
        this.blockXp.put(Material.DEEPSLATE_COAL_ORE, 1.9);
        this.blockXp.put(Material.IRON_ORE, 3.5);
        this.blockXp.put(Material.DEEPSLATE_IRON_ORE, 3.9);
        this.blockXp.put(Material.GOLD_ORE, 4.5);
        this.blockXp.put(Material.DEEPSLATE_GOLD_ORE, 5.1);
        this.blockXp.put(Material.REDSTONE_ORE, 3.2);
        this.blockXp.put(Material.DEEPSLATE_REDSTONE_ORE, 3.5);
        this.blockXp.put(Material.LAPIS_ORE, 3.3);
        this.blockXp.put(Material.DEEPSLATE_LAPIS_ORE, 3.7);
        this.blockXp.put(Material.DIAMOND_ORE, 8.5);
        this.blockXp.put(Material.COPPER_ORE, 3.1);
        this.blockXp.put(Material.DEEPSLATE_COPPER_ORE, 3.4);
        this.blockXp.put(Material.EMERALD_ORE, 25.0);
        this.blockXp.put(Material.DEEPSLATE_EMERALD_ORE, 50.0);
        this.blockXp.put(Material.DEEPSLATE_DIAMOND_ORE, 9.75);
        this.blockXp.put(Material.ANCIENT_DEBRIS, 21.8);
        this.blockXp.put(Material.GLOWSTONE, 2.5);
        this.blockXp.put(Material.NETHER_GOLD_ORE, 4.8);
        this.blockXp.put(Material.NETHER_QUARTZ_ORE, 3.25);
        this.blockXp.put(Material.SMALL_AMETHYST_BUD, 6.5);
        this.blockXp.put(Material.MEDIUM_AMETHYST_BUD, 9.5);
        this.blockXp.put(Material.LARGE_AMETHYST_BUD, 12.5);
        this.blockXp.put(Material.AMETHYST_CLUSTER, 15.5);
        this.blockXp.put(Material.ROOTED_DIRT, 2.0);

        this.entityXp.put(EntityType.CHICKEN, 3.0);
        this.entityXp.put(EntityType.BEE, 4.5);
        this.entityXp.put(EntityType.COW, 3.0);
        this.entityXp.put(EntityType.PIG, 3.0);
        this.entityXp.put(EntityType.SHEEP, 3.5);
        this.entityXp.put(EntityType.WOLF, 5.0);
        this.entityXp.put(EntityType.POLAR_BEAR, 12.5);
        this.entityXp.put(EntityType.OCELOT, 5.0);
        this.entityXp.put(EntityType.CAT, 5.0);
        this.entityXp.put(EntityType.MUSHROOM_COW, 7.75);
        this.entityXp.put(EntityType.PARROT, 10.0);
        this.entityXp.put(EntityType.RABBIT, 13.0);
        this.entityXp.put(EntityType.LLAMA, -10.0);
        this.entityXp.put(EntityType.HORSE, 8.5);
        this.entityXp.put(EntityType.DONKEY, 8.5);
        this.entityXp.put(EntityType.MULE, 8.5);
        this.entityXp.put(EntityType.SKELETON_HORSE, 50.0);
        this.entityXp.put(EntityType.ZOMBIE_HORSE, 50.0);
        this.entityXp.put(EntityType.TROPICAL_FISH, 8.0);
        this.entityXp.put(EntityType.COD, 8.0);
        this.entityXp.put(EntityType.PUFFERFISH, 20.0);
        this.entityXp.put(EntityType.SALMON, 8.0);
        this.entityXp.put(EntityType.DOLPHIN, 18.5);
        this.entityXp.put(EntityType.TURTLE, 18.5);
        this.entityXp.put(EntityType.PANDA, 18.5);
        this.entityXp.put(EntityType.FOX, 14.5);
        this.entityXp.put(EntityType.CREEPER, 5.5);
        this.entityXp.put(EntityType.ENDERMAN, 12.5);
        this.entityXp.put(EntityType.SILVERFISH, 5.5);
        this.entityXp.put(EntityType.SKELETON, 7.5);
        this.entityXp.put(EntityType.WITHER_SKELETON, 22.5);
        this.entityXp.put(EntityType.STRAY, 7.5);
        this.entityXp.put(EntityType.SLIME, 10.5);
        this.entityXp.put(EntityType.SPIDER, 5.5);
        this.entityXp.put(EntityType.ZOMBIE, 5.5);
        this.entityXp.put(EntityType.ZOGLIN, 8.5);
        this.entityXp.put(EntityType.HUSK, 5.5);
        this.entityXp.put(EntityType.DROWNED, 12.5);
        this.entityXp.put(EntityType.SQUID, 7.5);
        this.entityXp.put(EntityType.GLOW_SQUID, 10.5);
        this.entityXp.put(EntityType.CAVE_SPIDER, 6.5);
        this.entityXp.put(EntityType.WITCH, 17.5);
        this.entityXp.put(EntityType.GUARDIAN, 30.0);
        this.entityXp.put(EntityType.ELDER_GUARDIAN, 120.0);
        this.entityXp.put(EntityType.ENDERMITE, 4.5);
        this.entityXp.put(EntityType.MAGMA_CUBE, 9.5);
        this.entityXp.put(EntityType.STRIDER, 12.5);
        this.entityXp.put(EntityType.HOGLIN, 20.5);
        this.entityXp.put(EntityType.PIGLIN, 8.5);
        this.entityXp.put(EntityType.PIGLIN_BRUTE, 10.5);
        this.entityXp.put(EntityType.GOAT, 8.5);
        this.entityXp.put(EntityType.AXOLOTL, 25.5);
        this.entityXp.put(EntityType.GHAST, 15.5);
        this.entityXp.put(EntityType.BLAZE, 7.5);
        this.entityXp.put(EntityType.SHULKER, 25.5);
        this.entityXp.put(EntityType.VINDICATOR, 12.5);
        this.entityXp.put(EntityType.EVOKER, 29.5);
        this.entityXp.put(EntityType.VEX, 18.5);
        this.entityXp.put(EntityType.VILLAGER, 7.5);
        this.entityXp.put(EntityType.WANDERING_TRADER, 9.5);
        this.entityXp.put(EntityType.ZOMBIE_VILLAGER, 6.5);
        this.entityXp.put(EntityType.PHANTOM, 13.5);
        this.entityXp.put(EntityType.PILLAGER, 15.5);
        this.entityXp.put(EntityType.ILLUSIONER, 35.5);
        this.entityXp.put(EntityType.IRON_GOLEM, 40.0);
        this.entityXp.put(EntityType.SNOWMAN, 6.5);
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        if (this.allowXp) {
            final Player player = event.getPlayer();
            final ItemStack item = player.getInventory().getItemInMainHand();
            final Block block = event.getBlock();
            if (this.blockXp.containsKey(block.getType())) {
                this.serverCore.getLevelAPI().addExperience(player, this.blockXp.get(block.getType()));
            }
        }
    }

    @EventHandler
    public void on(final EntityDeathEvent event) {
        if (this.allowXp) {
            final LivingEntity entity = event.getEntity();
            final Player player = event.getEntity().getKiller();
            if (player != null) {
                final ItemStack item = player.getInventory().getItemInMainHand();
                if (this.entityXp.containsKey(entity.getType())) {
                    this.serverCore.getLevelAPI().addExperience(player, this.entityXp.get(entity.getType()));
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerFishEvent event) {
        if (this.allowXp) {
            final Player player = event.getPlayer();
            if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                this.serverCore.getLevelAPI().addExperience(player, 5.5);
            }
        }
    }

    @EventHandler
    public void on(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        final Level level = this.serverCore.getLevelAPI().getLevel(player.getName());
        this.serverCore.getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_TO_DATABASE.name(),
                player.getName(), String.valueOf(level.getLevel()), String.valueOf(level.getExperience()));
    }

}
