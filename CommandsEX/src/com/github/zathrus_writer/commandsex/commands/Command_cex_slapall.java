package com.github.zathrus_writer.commandsex.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.zathrus_writer.commandsex.helpers.Common;
import com.github.zathrus_writer.commandsex.helpers.LogHelper;
import com.github.zathrus_writer.commandsex.helpers.Utils;

public class Command_cex_slapall extends Common {

	public static Boolean run(CommandSender sender, String alias, String[] args){
		if (!(sender instanceof Player) || (!Utils.checkCommandSpam((Player) sender, "slap"))) {
			if (args.length > 1){
				// Incorrect args
				return true;
			}
			
			String slapHeight = null;
			if (args.length == 1){
				slapHeight = args[0];
			}
			
			for (Player p : Bukkit.getOnlinePlayers()){
				// Don't launch the sender
				if (!sender.getName().equalsIgnoreCase(p.getName())){
					String[] newargs = {p.getName(), (slapHeight != null ? slapHeight : "")};
					slap(sender, newargs, "slapall", alias);
				}
			}
			
			LogHelper.showInfo("playerSlappedAll", sender, ChatColor.AQUA);
		}
		return true;
	}
	
}
