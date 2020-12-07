package org.plugins.rpghorses.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.plugins.rpghorses.horses.RPGHorse;
import roryslibrary.util.MessagingUtil;
import roryslibrary.util.PluginMessagingUtil;

public class RPGMessagingUtil extends PluginMessagingUtil {
	
	public RPGMessagingUtil(JavaPlugin plugin) {
		super(plugin);
	}
	
	public void sendMessage(CommandSender sender, String msg, RPGHorse rpgHorse, String... placeholders) {
		super.sendMessage(sender, msg.replace("{HORSE-NAME}", rpgHorse.getName()), placeholders);
	}
	
	public void sendMessageAtPath(CommandSender sender, String path, RPGHorse rpgHorse, String... placeholders) {
		this.sendMessage(sender, getConfig().getString(path), rpgHorse, placeholders);
	}
	
	public String placeholders(String arg, RPGHorse rpgHorse, String... placeholders) {
		return super.placeholders(arg, placeholders).replace("{HORSE-NAME}", rpgHorse.getName());
	}
	
	public static String format(String arg, RPGHorse rpgHorse, String... placeholders) {
		return MessagingUtil.format(arg.replace("{HORSE-NAME}", rpgHorse.getName()), placeholders);
	}
}
