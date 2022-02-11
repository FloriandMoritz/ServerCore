package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NpcCommand extends Command {

    private final ServerCore serverCore;

    public NpcCommand(final ServerCore serverCore) {
        super("npc");
        this.serverCore = serverCore;
        this.setPermission("core.command.npc");
    }

    public static HashMap<String, Villager> selectedVillager = new HashMap<>();
    private final List<String> type = new ArrayList<>(List.of("DESERT", "JUNGLE", "PLAINS", "SAVANNA", "SNOW", "SWAMP", "TAIGA"));
    private final List<String> professions = new ArrayList<>(List.of("NONE", "NITWIT", "ARMORER", "BUTCHER", "CARTOGRAPHER", "CLERIC", "FARMER", "FISHERMAN",
            "FLETCHER", "LEATHERWORKER", "LIBRARIAN", "MASON", "SHEPHERD", "TOOLSMITH", "WEAPONSMITH"));

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player player) {
            if (args.length == 2) {
                if (args[0].equals("fnpc")) {
                    final ArmorStand armorStand = (ArmorStand) player.getLocation().getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
                    armorStand.getPersistentDataContainer().set(new NamespacedKey(ServerCore.getServerCore(), "fnpc.key"), PersistentDataType.STRING, args[1]);
                    armorStand.setGravity(false);
                    armorStand.setCanPickupItems(false);
                    armorStand.setCustomNameVisible(false);
                    armorStand.setVisible(false);
                    armorStand.setCanMove(false);
                    armorStand.setInvulnerable(true);
                    player.sendMessage("§8» §fCore §8| §7Der FNPC wurde erstellt.");
                }
            } else {
                final SimpleWindow mainWindow = new SimpleWindow.Builder("§7» §8NPC", "\n\n")
                        .addButton("§8» §fNPC erstellen", this::openCreateNpc)
                        .addButton("§8» §fNPC bearbeiten", this::openEditNpc)
                        .build();
                mainWindow.send(player);
            }
        }
        return true;
    }

    private void openCreateNpc(final Player player) {
        final CustomWindow createWindow = new CustomWindow("§7» §8NPC erstellen");
        createWindow.form()
                .input("§8» §fNPC-Key", "servercore:npc_name")
                .input("§8» §fAnzeigename", "Karl")
                .dropdown("§8» §fTyp", this.type.toArray(new String[0]))
                .dropdown("§8» §fBeruf", this.professions.toArray(new String[0]));

        createWindow.onSubmit((g, h) -> {
            final String key = h.getInput(0);
            final String displayName = h.getInput(1);
            final String type = this.type.get(h.getDropdown(2));
            final String profession = this.professions.get(h.getDropdown(3));

            if (key.isEmpty() || displayName.isEmpty()) {
                player.sendMessage("§8» §fCore §8| §7Fehlerhafte Angaben.");
                return;
            }

            final Villager villager = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
            villager.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING, key);
            villager.setCustomName(displayName);
            villager.setVillagerType(Villager.Type.valueOf(type));
            villager.setProfession(Villager.Profession.valueOf(profession));
            villager.setCanPickupItems(false);
            villager.setGravity(false);
            player.sendMessage("§8» §fCore §8| §7Der NPC wurde erstellt.");
        });
        createWindow.send(player);
    }

    private void openEditNpc(final Player player) {
        if (!selectedVillager.containsKey(player.getName()) || selectedVillager.get(player.getName()) == null) {
            player.sendMessage("§8» §fCore §8| §7Bitte wähle zunächst einen NPC mit einer Holzaxt aus, den du bearbeiten möchtest.");
        } else {
            final Villager villager = selectedVillager.get(player.getName());
            final CustomWindow editWindow = new CustomWindow("§7» §8NPC bearbeiten");
            editWindow.form()
                    .input("§8» §fNPC-Key", "servercore:npc_name", villager.getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING))
                    .input("§8» §fAnzeigename", "Karl", villager.getCustomName())
                    .dropdown("§8» §fTyp", this.type.toArray(new String[0]))
                    .dropdown("§8» §fBeruf", this.professions.toArray(new String[0]))
                    .toggle("§8» §fTyp und Beruf aktualisieren", false)
                    .toggle("§8» §cNPC löschen", false);

            editWindow.onSubmit((g, h) -> {
                final String key = h.getInput(0);
                final String displayName = h.getInput(1);
                final String type = this.type.get(h.getDropdown(2));
                final String profession = this.professions.get(h.getDropdown(3));
                final boolean updateTypeAndProfession = h.getToggle(4);
                final boolean delete = h.getToggle(5);

                if (!delete) {
                    if (!key.isEmpty() && !key.equals(villager.getPersistentDataContainer().get(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING))) {
                        villager.getPersistentDataContainer().set(new NamespacedKey(this.serverCore, "npc.key"), PersistentDataType.STRING, key);
                        player.sendMessage("§8» §fCore §8| §7Der Key wurde aktualisiert.");
                    }

                    if (!displayName.isEmpty() && !displayName.equals(villager.getCustomName())) {
                        villager.setCustomName(displayName);
                        player.sendMessage("§8» §fCore §8| §7Der Anzeigename wurde aktualisiert.");
                    }

                    if (updateTypeAndProfession) {
                        villager.setVillagerType(Villager.Type.valueOf(type));
                        villager.setProfession(Villager.Profession.valueOf(profession));
                        player.sendMessage("§8» §fCore §8| §7Der Typ und Beruf wurde aktualisiert.");
                    }
                } else {
                    villager.setHealth(0);
                    player.sendMessage("§8» §fCore §8| §7Der NPC wurde entfernt.");
                }
            });
            editWindow.send(player);
        }
    }

}
