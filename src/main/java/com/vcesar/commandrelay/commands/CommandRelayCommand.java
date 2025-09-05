package com.vcesar.commandrelay.commands;

import com.vcesar.commandrelay.CommandRelayPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandRelayCommand implements CommandExecutor {

    private final CommandRelayPlugin plugin;

    public CommandRelayCommand(CommandRelayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsa: /cr <reload|info|tcpstatus>");
            return true;
        }
        if (!sender.hasPermission("commandrelay.admin") && !sender.isOp()) {
            sender.sendMessage("❌  You do not have permission to use this command.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadPluginConfig(); // 🔹 Aquí se usa el método público
                sender.sendMessage("✅  Config reloaded.");
                return true;

            case "info":
                sender.sendMessage("ℹ CommandRelay Plugin v1.0 by vC3sar_");
                return true;

            case "tcpstatus":
                if (plugin.getServerSocket() != null && !plugin.getServerSocket().isClosed()) {
                    sender.sendMessage("✅  TCP Server listen in port " + plugin.getPort());
                } else {
                    sender.sendMessage("❌  TCP Server is not active.");
                }
                return true;

            default:
                sender.sendMessage("❌  unknown subcommand. Use: /cr <reload|info|tcpstatus>");
                return true;
        }
    }
}
