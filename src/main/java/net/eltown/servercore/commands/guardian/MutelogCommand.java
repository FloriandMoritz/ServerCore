package net.eltown.servercore.commands.guardian;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.guardian.GuardianCalls;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.forms.simple.SimpleWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MutelogCommand extends Command {

    private final ServerCore serverCore;

    public MutelogCommand(final ServerCore serverCore) {
        super("mutelog");
        this.serverCore = serverCore;
        this.setDescription("Mutelog Command");
        this.setPermission("core.command.mutelog");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomWindow formWindowCustom = new CustomWindow("§7» §8Mutelog");
            formWindowCustom.form()
                    .input("§8» §7Nickname des Spielers", "Nickname", input);
            formWindowCustom.onSubmit((e, r) -> {
                final String target = r.getInput(0);

                if (target != null && target.isEmpty()) {
                    sender.sendMessage(Language.get("guardian.invalid.input"));
                    return;
                }

                this.serverCore.getTinyRabbit().sendAndReceive((delivery -> {
                    switch (GuardianCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_ENTRIES_NOT_FOUND -> sender.sendMessage(Language.get("mute.log.no.entries"));
                        case CALLBACK_MUTE_BEHAVIOR_ENTRIES_RECEIVED -> {
                            final List<String> list = Arrays.asList(delivery.getData());
                            final StringBuilder content = new StringBuilder("§fAuszug von: §7" + target + "\n§fDatensätze: §7" + (list.size() - 1) + "\n\n");
                            list.forEach(z -> {
                                if (!z.equals(delivery.getKey().toLowerCase())) {
                                    final String[] d = z.split(">>");
                                    content.append("§eMuteID: §7").append(d[1]).append("\n").append("§eLogID: §7").append(d[0]).append("\n").append("§eGrund: §7")
                                            .append(d[3]).append("\n").append("§eErsteller: §7").append(d[4]).append("\n").append("§eDatum: §7").append(d[5]).append("\n")
                                            .append("§eLänge: §7").append(this.serverCore.getRemainingTimeFuture((Long.parseLong(d[6]) - Long.parseLong(d[7])) + System.currentTimeMillis())).append("\n\n");
                                }
                            });
                            final SimpleWindow form = new SimpleWindow.Builder("§7» §8Mutelog", content.toString()).build();
                            form.send(e);
                        }
                    }
                }), Queue.GUARDIAN, GuardianCalls.REQUEST_GET_MUTE_BEHAVIOR_ENTRIES.name(), target);
            });
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
