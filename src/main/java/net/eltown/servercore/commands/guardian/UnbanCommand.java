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

public class UnbanCommand extends Command {

    private final ServerCore serverCore;

    public UnbanCommand(final ServerCore serverCore) {
        super("unban");
        this.serverCore = serverCore;
        this.setDescription("Unban Command");
        this.setPermission("core.command.unban");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission(Objects.requireNonNull(this.getPermission()))) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomWindow formWindowCustom = new CustomWindow("§7» §8Unban");
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
                        case CALLBACK_BAN_IS_NOT_BANNED -> sender.sendMessage(Language.get("guardian.ban.unban.not.banned", target));
                        case CALLBACK_BAN_CANCELLED -> sender.sendMessage(Language.get("guardian.ban.unban.executed", target, reason, delivery.getData()[1]));
                    }
                }), Queue.GUARDIAN, GuardianCalls.REQUEST_CANCEL_BAN.name(), target, reason, sender.getName());
            });
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
