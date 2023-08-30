package co.purevanilla.mcplugins.taxes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

public class CMD implements CommandExecutor {

    API api;

    CMD(API api){
        this.api=api;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(commandSender instanceof Player){
            this.api.taxes.getServer().getScheduler().runTaskAsynchronously(this.api.taxes, () -> {
                try {
                    this.api.showTop((Player) commandSender);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return true;
    }
}
