package org.plugins.rpghorses.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.plugins.rpghorses.horses.RPGHorse;
import rorys.library.util.MessagingUtil;

public class RPGMessagingUtil extends MessagingUtil {

	public RPGMessagingUtil(JavaPlugin plugin) {
		super(plugin);
	}

	public void sendMessage(CommandSender sender, String msg, RPGHorse rpgHorse) {
		super.sendMessage(sender, msg.replace("{HORSE-NAME}", rpgHorse.getName()));
	}

	public void sendMessageAtPath(CommandSender sender, String path, RPGHorse rpgHorse) {
		this.sendMessage(sender, getPlugin().getConfig().getString(path), rpgHorse);
	}

	public String placeholders(String arg, RPGHorse rpgHorse) {
		return super.placeholders(arg).replace("{HORSE-NAME}", rpgHorse.getName());
	}

	public static String format(String arg, RPGHorse rpgHorse) {
		return MessagingUtil.format(arg.replace("{HORSE-NAME}", rpgHorse.getName()));
	}
}
