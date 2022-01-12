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

public class CheckbanCommand extends Command {

    private final ServerCore serverCore;

    public CheckbanCommand(final ServerCore serverCore) {
        super("checkban");
        this.serverCore = serverCore;
        this.setDescription("Checkban Command");
        this.setPermission("core.command.checkban");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomWindow formWindowCustom = new CustomWindow("§7» §8Checkban");
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
                        case CALLBACK_BAN_IS_NOT_BANNED -> sender.sendMessage(Language.get("guardian.ban.is.not.banned", target));
                        case CALLBACK_ACTIVE_BAN_ENTRY_RECEIVED -> {
                            final List<String> list = Arrays.asList(delivery.getData());
                            final StringBuilder content = new StringBuilder("§fAuszug von: §7" + target + "\n\n");
                            list.forEach(z -> {
                                if (!z.equals(delivery.getKey().toLowerCase())) {
                                    final String[] d = z.split(">>");
                                    content.append("§eBanID: §7").append(d[0]).append("\n").append("§eGrund: §7")
                                            .append(d[2]).append("\n").append("§eErsteller: §7").append(d[3]).append("\n").append("§eDatum: §7").append(d[4]).append("\n")
                                            .append("§eVerbleibend: §7").append(this.serverCore.getRemainingTimeFuture(Long.parseLong(d[5]))).append("\n\n");
                                }
                            });
                            final SimpleWindow form = new SimpleWindow.Builder("§7» §8Checkban", content.toString()).build();
                            form.send(e);
                        }
                    }
                }), Queue.GUARDIAN, GuardianCalls.REQUEST_GET_BAN_ENTRY_TARGET.name(), target);
            });
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
