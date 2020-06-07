package org.plugins.rpghorses.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.XPManager;
import org.plugins.rpghorses.players.HorseOwner;

public class PlayerQuitListener implements Listener {
	
	private final HorseOwnerManager horseOwnerManager;
	private final XPManager xpManager;
	
	public PlayerQuitListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, XPManager xpManager) {
		this.horseOwnerManager = horseOwnerManager;
		this.xpManager = xpManager;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent e) {
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(e.getPlayer());
		this.xpManager.removeHorseOwner(horseOwner);
		this.horseOwnerManager.flushHorseOwner(horseOwner);
	}
	
}
