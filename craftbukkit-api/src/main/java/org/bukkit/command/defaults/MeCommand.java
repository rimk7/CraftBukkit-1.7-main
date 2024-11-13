package org.bukkit.command.defaults;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MeCommand extends VanillaCommand {
    public MeCommand() {
        super("me");
        this.description = "Performs the specified action in chat";
        this.usageMessage = "/me <action>";
        this.setPermission("bukkit.command.me");
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!sender.isOp())
        {
            sender.sendMessage(ChatColor.DARK_RED + "No permission.");
            return false;
        }
        if (args.length < 1)  {
            sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
            return false;
        }

        StringBuilder message = new StringBuilder();
        message.append((sender.isOp() ? ChatColor.RED : ChatColor.GREEN) + sender.getName());

        for (String arg : args) {
            message.append(" ");
            message.append(ChatColor.YELLOW + arg);
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "* " + message.toString());

        return true;
    }
}
