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

public class UnmuteCommand extends Command {

    private final ServerCore serverCore;

    public UnmuteCommand(final ServerCore serverCore) {
        super("unmute");
        this.serverCore = serverCore;
        this.setDescription("Unmute Command");
        this.setPermission("core.command.unmute");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomWindow formWindowCustom = new CustomWindow("§7» §8Unmute");
            formWindowCustom.form()
                    .input("§8» §7Nickname des Spielers", "Nickname", input)
                    .input("§8» §7Grund der Aufhebung", "Grund");

            formWindowCustom.onSubmit((e, r) -> {
                final String target = r.getInput(0);
                final String reason = r.getInput(1);

                if (reason != null && target != null && (target.isEmpty() || reason.isEmpty())) {
                    sender.sendMessage(Language.get("guardian.invalid.input"));
                    return;
                }

                this.serverCore.getTinyRabbit().sendAndReceive((delivery -> {
                    switch (GuardianCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_MUTE_IS_NOT_MUTED -> sender.sendMessage(Language.get("guardian.mute.unmute.not.muted", target));
                        case CALLBACK_MUTE_CANCELLED -> sender.sendMessage(Language.get("guardian.mute.unmute.executed", target, reason, delivery.getData()[1]));
                    }
                }), Queue.GUARDIAN, GuardianCalls.REQUEST_CANCEL_MUTE.name(), target, reason, sender.getName());
            });
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
