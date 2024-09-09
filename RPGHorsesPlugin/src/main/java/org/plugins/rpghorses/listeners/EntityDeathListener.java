package org.plugins.rpghorses.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.managers.XPManager;
import org.plugins.rpghorses.managers.gui.StableGUIManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import roryslibrary.util.TimeUtil;

public class EntityDeathListener implements Listener {
	
	private RPGHorsesMain plugin;
	private final RPGHorseManager rpgHorseManager;
	private final StableGUIManager stableGuiManager;
	private final XPManager xpManager;
	private final RPGMessagingUtil messagingUtil;
	
	public EntityDeathListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager, XPManager xpManager, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.rpgHorseManager = rpgHorseManager;
		this.stableGuiManager = stableGuiManager;
		this.xpManager = xpManager;
		this.messagingUtil = messagingUtil;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent e) {
		EntityType entityType = e.getEntityType();
		if (this.rpgHorseManager.isValidEntityType(entityType)) {
			RPGHorse rpgHorse = this.rpgHorseManager.getRPGHorse(e.getEntity());
			if (rpgHorse != null) {
				HorseOwner horseOwner = rpgHorse.getHorseOwner();
				rpgHorse.loadItems();
				horseOwner.setCurrentHorse(null);
				rpgHorse.setDead(true);
				e.getDrops().clear();
				e.setDroppedExp(0);
				
				this.stableGuiManager.updateRPGHorse(rpgHorse);
				
				messagingUtil.sendMessage(horseOwner.getPlayer(), plugin.getConfig().getString("messages.horse-died").replace("{TIME}", TimeUtil.formatTime((long) Math.ceil(stableGuiManager.getDeathDifferent(rpgHorse) / 1000D))), rpgHorse);
			}
		}
	}
	
}
