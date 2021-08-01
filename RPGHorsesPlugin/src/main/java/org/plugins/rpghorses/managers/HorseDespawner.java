package org.plugins.rpghorses.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;

public class HorseDespawner {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final RPGHorseManager rpgHorseManager;
	
	private int idleTime = 5;
	private BukkitTask idleHorseTask;
	
	public HorseDespawner(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.rpgHorseManager = rpgHorseManager;
		
		if (plugin.getConfig().getBoolean("horse-options.prevent-regular-horses")) {
			this.despawnAllHorses();
		} else {
			this.despawnAllRPGHorses();
		}
		this.reloadIdleTime();
	}
	
	public void reloadIdleTime() {
		int oldIdleTime = this.idleTime;
		this.idleTime = this.plugin.getConfig().getInt("horse-options.idle-time");
		
		if (this.idleTime != oldIdleTime) {
			this.startIdleHorseTask();
		}
	}
	
	public void despawnAllRPGHorses() {
		int horsesDespawned = 0;
		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners().values()) {
			RPGHorse oldCurrentHorse = horseOwner.getCurrentHorse();
			horseOwner.setCurrentHorse(null);
			if (oldCurrentHorse != null) {
				horsesDespawned++;
			}
		}
		Bukkit.getLogger().info("[RPGHorses] Despawned " + horsesDespawned + " horses");
	}
	
	public void despawnAllHorses() {
		int horsesDespawned = 0;
		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (this.rpgHorseManager.isValidEntityType(entity.getType())) {
					horsesDespawned++;
					entity.remove();
				}
			}
		}
		Bukkit.getLogger().info("[RPGHorses] Despawned " + horsesDespawned + " horses");
	}
	
	public void startIdleHorseTask() {
		this.cancelIdleHorseTask();
		
		final HorseDespawner horseDespawner = this;
		this.idleHorseTask = new BukkitRunnable() {
			@Override
			public void run() {
				for (HorseOwner horseOwner : horseDespawner.horseOwnerManager.getHorseOwners().values()) {
					RPGHorse currentHorse = horseOwner.getCurrentHorse();
					if (currentHorse != null) {
						Location currentLocation = currentHorse.getHorse().getLocation();
						Location lastLocation = currentHorse.getLastLocation();
						
						if (lastLocation == null || lastLocation.distanceSquared(currentLocation) > 1) {
							currentHorse.setLastMoveTime(System.currentTimeMillis());
							currentHorse.setLastLocation(currentLocation);
						} else if (System.currentTimeMillis() - currentHorse.getLastMoveTime() >= (idleTime * 1000L)) {
							Bukkit.getScheduler().runTask(plugin, () -> {
								currentHorse.despawnEntity();
								horseOwner.setCurrentHorse(null);
							});
						}
					}
				}
			}
		}.runTaskTimerAsynchronously(this.plugin, 0, 20L);
	}
	
	public void cancelIdleHorseTask() {
		if (this.idleHorseTask != null) {
			this.idleHorseTask.cancel();
			this.idleHorseTask = null;
		}
	}
	
}
