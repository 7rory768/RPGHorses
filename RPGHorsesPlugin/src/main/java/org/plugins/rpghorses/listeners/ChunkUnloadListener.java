package org.plugins.rpghorses.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;

public class ChunkUnloadListener implements Listener {

	private final RPGHorsesMain plugin;
	private final RPGHorseManager rpgHorseManager;
	private final RPGMessagingUtil messagingUtil;

	public ChunkUnloadListener(RPGHorsesMain plugin) {
		this.plugin = plugin;
		this.rpgHorseManager = plugin.getRpgHorseManager();
		messagingUtil = plugin.getMessagingUtil();

		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (!rpgHorseManager.isRPGHorse(entity)) continue;

			RPGHorse horse = rpgHorseManager.getRPGHorse(entity);

			if (horse != null) {
				horse.despawnEntity();

				HorseOwner horseOwner = horse.getHorseOwner();

				if (horseOwner != null && horseOwner.getPlayer() != null) {
					horseOwner.setCurrentHorse(null);

					this.messagingUtil.sendMessage(horseOwner.getPlayer(), this.plugin.getConfig().getString("messages.horse-sent-to-stable").replace("{PLAYER}", "CONSOLE"), horse);
				}
			} else {
				entity.remove();
			}
		}
	}
}
