package com.cobelpvp.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class GCCommand extends Command {

    public GCCommand(String name) {
        super(name);
        this.usageMessage = "/" + name;
        this.description = "Execute garbage collector";
        this.setPermission("cobelpvp.gc");
    }

    @Override
    public boolean execute(CommandSender sender, String arg1, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        System.gc();
        sender.sendMessage(ChatColor.BLUE + "Garbage Cleaned");

        return false;
    }
}
