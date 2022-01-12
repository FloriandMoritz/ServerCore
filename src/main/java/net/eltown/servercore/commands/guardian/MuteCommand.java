package net.eltown.servercore.commands.guardian;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.guardian.GuardianCalls;
import net.eltown.servercore.components.forms.custom.CustomWindow;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MuteCommand extends Command {

    private final ServerCore serverCore;

    public MuteCommand(final ServerCore serverCore) {
        super("mute");
        this.serverCore = serverCore;
        this.setDescription("Mute Command");
        this.setPermission("core.command.mute");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomWindow formWindowCustom = new CustomWindow("§7» §8Muten");
            formWindowCustom.form()
                    .input("§8» §7Nickname des Spielers", "Nickname", input)
                    .input("§8» §7Grund der Bestrafung", "Grund")
                    .dropdown("§8» §7Auswahl der Zeiteinheit", 2, "m", "h", "d", "M", "Permanent")
                    .slider("§8» §7Auswahl der Zeit", 1, 100, 1, 3);

            formWindowCustom.onSubmit((e, r) -> {
                final String target = r.getInput(0);
                final String reason = r.getInput(1);
                final long duration = this.serverCore.getDuration(this.timeFormat(r.getDropdown(2)), (int) r.getSlider(3));

                if (target.isEmpty() || reason.isEmpty()) {
                    sender.sendMessage(Language.get("guardian.invalid.input"));
                    return;
                }

                this.serverCore.getTinyRabbit().sendAndReceive((delivery -> {
                    switch (GuardianCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_MUTE_IS_MUTED:
                            sender.sendMessage(Language.get("guardian.mute.is.muted", target));
                            break;
                        case CALLBACK_MUTE_EXECUTED:
                            sender.sendMessage(Language.get("guardian.mute.executed", target, reason, delivery.getData()[1]));
                            break;
                    }
                }), Queue.GUARDIAN, GuardianCalls.REQUEST_INITIATE_MUTE.name(), target, reason, sender.getName(), String.valueOf(duration));
            });
            formWindowCustom.send((Player) sender);
        }
        return true;
    }

    private String timeFormat(final int dropdown) {
        return switch (dropdown) {
            case 0 -> "m";
            case 1 -> "h";
            case 2 -> "d";
            case 3 -> "M";
            case 4 -> "P";
            default -> "null";
        };
    }
}
