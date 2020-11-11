package org.plugins.rpghorses.utils;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DebugUtil {
	
	@Setter
	private static JavaPlugin plugin;
	@Getter
	private static boolean debugRoree = true;
	
	public static void toggleDebugRoree() {
		debugRoree = !debugRoree;
	}
	
	public static void debug(String arg) {
		if (debugRoree) {
			Player p = Bukkit.getPlayer("Roree");
			if (p != null && p.isOnline()) {
				p.sendMessage(arg);
			}
		}
		
		if (plugin != null) {
		Bukkit.broadcast(arg, plugin.getName().toLowerCase() + ".debug");
		DebugUtil.plugin.getLogger().info(arg);
		}
	}
	
}
