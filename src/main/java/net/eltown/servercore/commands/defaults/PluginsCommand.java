package net.eltown.servercore.commands.defaults;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PluginsCommand extends Command {

    private final List<String> ignoredPlugins = List.of("FastAsyncWorldEdit", "floodgate");
    private final ServerCore serverCore;

    public PluginsCommand(final ServerCore serverCore) {
        super("plugins", "", "", List.of("pl", "plugin"));
        this.serverCore = serverCore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.openPlugins(player);
        }
        return true;
    }

    private void openPlugins(final Player player) {
        final List<Plugin> plugins = new ArrayList<>(Arrays.asList(this.serverCore.getServer().getPluginManager().getPlugins()));
        final SimpleWindow.Builder window = new SimpleWindow.Builder("§7» §8Plugins", "Hier sind alle Plugins dieses Servers aufgelistet. Klicke auf eines, um näheres zu erfahren.\n§aAnzahl: §f" + (plugins.size() - this.ignoredPlugins.size()));
        plugins.forEach(plugin -> {
            if (!this.ignoredPlugins.contains(plugin.getDescription().getName())) {
                window.addButton("§8» §a" + plugin.getDescription().getName() + "\n§f" + plugin.getDescription().getVersion(), e -> {
                    final SimpleWindow pluginWindow = new SimpleWindow.Builder("§7» §8" + plugin.getDescription().getName(), this.pluginDescription(plugin.getDescription()))
                            .addButton("§7» §cZurück", this::openPlugins)
                            .build();
                    pluginWindow.send(player);
                });
            }
        });
        window.build().send(player);
    }

    private String pluginDescription(final PluginDescriptionFile plugin) {
        if (plugin.getAuthors().size() != 0) {
            final StringBuilder authorList = new StringBuilder();
            plugin.getAuthors().forEach(e -> {
                authorList.append(e).append(",").append(" ");
            });
            final String authors = authorList.substring(0, authorList.length() - 2);

            if (plugin.getAuthors().size() == 1) {
                return "§fPlugin: §a" + plugin.getName() + "\n§fVersion: §a" + plugin.getVersion() + "\n§fAutor: §a" + authors + "\n§fBeschreibung: §a" + plugin.getDescription();
            } else {
                return "§fPlugin: §a" + plugin.getName() + "\n§fVersion: §a" + plugin.getVersion() + "\n§fAutoren: §a" + authors + "\n§fBeschreibung: §a" + plugin.getDescription();
            }
        } else {
            return "§fPlugin: §a" + plugin.getName() + "\n§fVersion: §a" + plugin.getVersion() + "\n§fBeschreibung: §a" + plugin.getDescription();
        }
    }

}
