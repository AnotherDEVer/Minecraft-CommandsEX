package com.github.zathrus_writer.commandsex.helpers;

import static com.github.zathrus_writer.commandsex.Language._;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.github.zathrus_writer.commandsex.CommandsEX;
import com.github.zathrus_writer.commandsex.SQLManager;

/***
 * Contains set of functions and event listeners used to handle the freeze command.
 * @author zathrus-writer
 *
 */
public class Common implements Listener {
	
	public static List<String> frozenPlayers = new ArrayList<String>();
	public static Common plugin = null;
	public static Boolean ignoreTpEvent = false;

	/***
	 * simply tell Bukkit we want to listen
	 */
	public Common() {
		plugin = this;
	}
	
	/***
	 * FREEZE - freezes a player, making him unable to perform physical actions on the server
	 * @param sender
	 * @param args
	 * @param command
	 * @param alias
	 * @return
	 */
	public static Boolean freeze(CommandSender sender, String[] args, String command, String alias, Boolean... omitMessage) {
		// create Freeze class instance, if not instantiated yet to allow for events listening
		if (plugin == null) {
			new Common();
		}

		Boolean showMessages = (omitMessage.length == 0);
		// check if requested player is online
		Player p = Bukkit.getServer().getPlayer(args[0]);
		String pName = "";
		if (p != null) {
			pName = p.getName();
		} else {
			pName = args[0];
		}
				
		// check if requested player is frozen
		if ((frozenPlayers.size() > 0) && frozenPlayers.contains(pName)) {
			// unfreeze player
			frozenPlayers.remove(pName);
			// inform the command sender and the player
			if (showMessages) {
				LogHelper.showInfo("[" + pName + " #####freezePlayerUnfrozen", sender);
				if (p != null) {
					LogHelper.showInfo("freezeYouAreUnfrozen", p);
				}
			}
			
			// if there are no more frozen players left, we don't need our event listeners, so unregister them
			HandlerList.unregisterAll(Common.plugin);
			
			return true;
		}

		// we are trying to freeze a player
		if (p == null) {
			// requested player not found
			LogHelper.showWarning("invalidPlayer", sender);
			return true;
		}

		// insert player's name into frozen players' list
		frozenPlayers.add(pName);
		// if we have only this player, add activate event listeners, since they're not active yet
		if (frozenPlayers.size() == 1) {
			CommandsEX.plugin.getServer().getPluginManager().registerEvents(Common.plugin, CommandsEX.plugin);
		}
		
		if (showMessages) {
			// inform both players
			LogHelper.showInfo("[" + pName + " #####freezePlayerFrozen", sender);
			LogHelper.showInfo("freezeYouAreFrozen1", p);
			LogHelper.showInfo("freezeYouAreFrozen2", p);
		}
		
		return true;
	}
	
	/***
	 * KICK - kicks a player out from the server, optionally providing a custom reason
	 * @param sender
	 * @param args
	 * @param command
	 * @param alias
	 * @return
	 */
	public static Boolean kick(CommandSender sender, String[] args, String command, String alias) {
		// check if requested player is online
		Player p = Bukkit.getServer().getPlayer(args[0]);
		String pName = "";
		String leaveReason = "";
		if (p == null) {
			// requested player not found
			LogHelper.showWarning("invalidPlayer", sender);
			return true;
		} else {
			pName = p.getName();
		}
		
		// if we have more than 1 argument, build up the leave string
		if (args.length > 1) {
			for (Integer i = 1; i < args.length; i++) {
				leaveReason = leaveReason + " " + args[i];
			}
		}
		
		// kick player and tell everyone if set up in the config file
		p.kickPlayer(leaveReason.equals("") ? _("kickGenericReason", "") : leaveReason);
		if (!CommandsEX.getConf().getBoolean("silentKicks")) {
			CommandsEX.plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + (!leaveReason.equals("") ? (pName + " " + _("kickBeingKickedForMessage", "") + leaveReason) : pName + " " + _("kickBeingKickedMessage", "")));
		}
		
		return true;
	}
	
	/***
	 * PLAYTIME - tells a player or admin how long they played on the server
	 * @param sender
	 * @param args
	 * @param command
	 * @param alias
	 * @return
	 */
	public static Boolean playtime(CommandSender sender, String[] args, String command, String alias) {
		if (!CommandsEX.sqlEnabled) {
			LogHelper.showWarning("playTimeNoSQL", sender);
			return false;
		}
		
		// check who we want to see the playtime for
		String pName;
		Boolean online = false;
		if (args.length > 0) {
			pName = args[0];
			if (Bukkit.getServer().getPlayer(pName) != null) {
				online = true;
			}
		} else {
			pName = sender.getName();
		}
		
		// load playtime from database if the player is not online, otherwise load it from stored values in main class
		Integer playtime = 0;
		if (online && CommandsEX.playTimes.containsKey(pName) && (CommandsEX.playTimes.get(pName) > -1)) {
			playtime = CommandsEX.playTimes.get(pName);
			Map<String, Integer> m = Utils.parseTimeStamp(playtime);
			LogHelper.showInfo("playTimeIs#####[" + (m.get("days") + " #####days#####[, ") + (m.get("hours") + " #####hours#####[, ") + (m.get("minutes") + " #####minutes#####[, ") + (m.get("seconds") + " #####seconds"), sender);
			return true;
		} else {
			String playerForSQL = pName.toLowerCase();
			String foundPlayerName = "";
			Integer numPlayers = 0;
			ResultSet res = SQLManager.query_res("SELECT * FROM " + SQLManager.prefix + "playtime WHERE (player_name = ? OR player_name LIKE ?)", playerForSQL, playerForSQL + "%");
			
			try {
				while (res.next()) {
					numPlayers++;
					foundPlayerName = res.getString("player_name");
					playtime = res.getInt("seconds_played");
	
					// if the name matches exactly what we've been looking for, just exit the loop
					if (foundPlayerName.toLowerCase().equals(playerForSQL)) {
						numPlayers = 1;
						playtime = res.getInt("seconds_played");
						break;
					}
				}
				res.close();
				
				if (numPlayers > 1) {
					// too many players match the selection
					numPlayers = 0;
					foundPlayerName = pName;
				} else if (numPlayers == 1) {
					// show playtime
					Map<String, Integer> m = Utils.parseTimeStamp(playtime);
					LogHelper.showInfo("playTimeIs#####[" + (m.get("days") + " #####days#####[, ") + (m.get("hours") + " #####hours#####[, ") + (m.get("minutes") + " #####minutes#####[, ") + (m.get("seconds") + " #####seconds"), sender);
					return true;
				}
				
				// if player's playtime was not found, show message
				if (numPlayers == 0) {
					LogHelper.showInfo("playTimeNotFound", sender, ChatColor.YELLOW);
				}
			} catch (Throwable e) {
				// unable to load players' playtime
				LogHelper.showWarning("internalError", sender);
				LogHelper.logSevere("[CommandsEX] " + _("dbReadError", ""));
				LogHelper.logDebug("Message: " + e.getMessage() + ", cause: " + e.getCause());
				return false;
			}
		}
		
		return true;
	}
	
	/***
	 * Functions below listen to all events that should be prevented when a player is frozen.
	 * @param e
	 * @return
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(BlockBreakEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(BlockDamageEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(BlockIgniteEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(BlockPlaceEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(PlayerBucketEmptyEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(PlayerDropItemEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(PlayerInteractEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(PlayerJoinEvent e) {
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			// if the player is frozen, remind them
			Player p = e.getPlayer();
			LogHelper.showInfo("freezeYouAreFrozen1", p);
			LogHelper.showInfo("freezeYouAreFrozen2", p);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(InventoryOpenEvent e) {
		if (e.isCancelled()) return;
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(PlayerMoveEvent e) {
		if (e.isCancelled()) return;
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			Common.ignoreTpEvent = true;
			e.setTo(e.getFrom());
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkFrozenState(PlayerTeleportEvent e) {
		if (e.isCancelled()) return;
		if (Common.ignoreTpEvent) {
			Common.ignoreTpEvent = false;
			return;
		}
		
		if ((Common.frozenPlayers.size() > 0) && Common.frozenPlayers.contains(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
}