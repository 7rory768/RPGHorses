package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.managers.XPManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;

public class VehicleListener implements Listener {

	private final RPGHorsesMain plugin;
	private final RPGHorseManager rpgHorseManager;
	private final XPManager xpManager;
	private final RPGMessagingUtil messagingUtil;

	public VehicleListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, XPManager xpManager, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.rpgHorseManager = rpgHorseManager;
		this.xpManager = xpManager;
		this.messagingUtil = messagingUtil;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onVehicleEnter(VehicleEnterEvent e) {
		RPGHorse rpgHorse = this.rpgHorseManager.getRPGHorse(e.getVehicle());

		if (rpgHorse != null) {
			HorseOwner horseOwner = rpgHorse.getHorseOwner();
			Entity entered = e.getEntered();

			if (entered.getType() == EntityType.PLAYER) {
				Player p = (Player) entered;

				if (!horseOwner.getUUID().equals(p.getUniqueId())) {
					this.messagingUtil.sendMessageAtPath(p, "messages.not-your-horse");
					e.setCancelled(true);
					return;
				}

				p.setMetadata("RPGHorses-Ignore-Next-Teleport", new FixedMetadataValue(plugin, true));
			}
		}
	}

	@EventHandler
	public void onVehicleExit(VehicleExitEvent e) {
		RPGHorse rpgHorse = this.rpgHorseManager.getRPGHorse(e.getVehicle());
		if (rpgHorse != null) {
			e.getExited().setMetadata("RPGHorses-Ignore-Next-Teleport", new FixedMetadataValue(plugin, true));
		}
	}

}
