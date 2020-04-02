package org.plugins.rpghorses.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;

public class EntitySpawnListener implements Listener {

	private final RPGHorsesMain     plugin;
	private final RPGHorseManager   rpgHorseManager;
	private final HorseOwnerManager horseOwnerManager;

	public EntitySpawnListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, HorseOwnerManager horseOwnerManager) {
		this.plugin = plugin;
		this.rpgHorseManager = rpgHorseManager;
		this.horseOwnerManager = horseOwnerManager;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntitySpawn(CreatureSpawnEvent e) {
		if (plugin.getConfig().getBoolean("horse-options.prevent-regular-horses")) {
			EntityType entityType = e.getEntityType();
			if (this.rpgHorseManager.isValidEntityType(entityType)) {
				CreatureSpawnEvent.SpawnReason spawnReason = e.getSpawnReason();
				// TODO: Make auto horse despawning optional
				for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners()) {
					if (horseOwner.isSpawningHorse() && spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
						horseOwner.setSpawningHorse(false);
						return;
					}
				}
				if (spawnReason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG && spawnReason != CreatureSpawnEvent.SpawnReason.CUSTOM) {
					e.setCancelled(true);
				}
			}
		}
	}

}
