package net.eltown.servercore.commands.administrative;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.CustomEnchantments;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class CustomEnchantmentCommand extends Command {

    private final ServerCore serverCore;

    public CustomEnchantmentCommand(final ServerCore serverCore) {
        super("customenchantment");
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            final List<String> enchantments = new LinkedList<>(List.of(
                    "LUMBERJACK", "DRILL", "EMERALD_FARMER", "EXPERIENCE", "VEIN_MINING"
            ));
            final CustomWindow window = new CustomWindow("§7» §8Verzauberung hinzufügen");
            window.form()
                    .label("§8» §fItem in deiner Hand: §9" + player.getInventory().getItemInMainHand().getI18NDisplayName())
                    .dropdown("§8» §fWelche Verzauberung soll auf das Item gemacht werden?", enchantments.toArray(new String[0]))
                    .slider("§8» §fWelches Level soll die Verzauberung haben?", 1, 10, 1, 1);

            window.onSubmit((g, h) -> {
                final CustomEnchantments.Enchantment enchantment = CustomEnchantments.Enchantment.valueOf(enchantments.get(h.getDropdown(1)));
                final int level = (int) h.getSlider(2);

                this.serverCore.getCustomEnchantments().enchantItem(player, enchantment, level);
                player.sendMessage("§8» §fCore §8| §7Das Item in deiner Hand wurde mit §9" + enchantment.enchantment() + " Level " + level + " §7verzaubert.");
            });
            window.send(player);
        }
        return false;
    }
}
