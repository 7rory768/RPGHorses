package org.plugins.rpghorses.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;

public class PlayerTeleportListener implements Listener {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private RPGMessagingUtil messagingUtil;
	
	public PlayerTeleportListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.messagingUtil = messagingUtil;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onTeleport(PlayerTeleportEvent e) {
		Player p = e.getPlayer();
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
		if (horseOwner != null) {
			RPGHorse currentHorse = horseOwner.getCurrentHorse();

			if (p.hasMetadata("RPGHorses-Ignore-Next-Teleport")) {
				return;
			}

			p.removeMetadata("RPGHorses-Ignore-Next-Teleport", this.plugin);

			double maxDist = plugin.getHorseDespawner().getDespawnWhenOwnerPastDistance();
			if (maxDist < 0) return;
			
			if (currentHorse != null && (!e.getFrom().getWorld().equals(e.getTo().getWorld()) || e.getTo().distance(e.getFrom()) > maxDist)) {
				horseOwner.setCurrentHorse(null);
				this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-sent-to-stable").replace("{PLAYER}", "CONSOLE"), currentHorse);
			}
		}
	}
	
}
