package org.plugins.rpghorses.managers;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import roryslibrary.configs.PlayerConfigs;

import java.util.List;
import java.util.UUID;

public class MessageQueuer {
	
	private final PlayerConfigs playerConfigs;
	private final RPGMessagingUtil messagingUtil;
	
	public MessageQueuer(PlayerConfigs playerConfigs, RPGMessagingUtil messagingUtil) {
		this.playerConfigs = playerConfigs;
		this.messagingUtil = messagingUtil;
	}
	
	public void queueMessage(OfflinePlayer offlinePlayer, String message) {
		UUID uuid = offlinePlayer.getUniqueId();
		List<String> queuedMessages = this.playerConfigs.getConfig(uuid).getStringList("queued-messages");
		queuedMessages.add(message);
		this.playerConfigs.getConfig(uuid).set("queued-messages", queuedMessages);
		this.playerConfigs.saveConfig(uuid);
		this.playerConfigs.reloadConfig(uuid);
	}
	
	public void sendQueuedMessages(Player p) {
		List<String> queuedMessages = this.playerConfigs.getConfig(p).getStringList("queued-messages");
		for (String message : queuedMessages) {
			this.messagingUtil.sendMessage(p, message);
		}
		this.playerConfigs.getConfig(p).set("queued-messages", null);
		this.playerConfigs.saveConfig(p);
		this.playerConfigs.reloadConfig(p);
	}
	
}
