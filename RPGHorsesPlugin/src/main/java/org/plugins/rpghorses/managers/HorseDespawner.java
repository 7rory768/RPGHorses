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
	private int despawnWhenOwnerPastDistance = -1;
	private BukkitTask despawnTask;

	public HorseDespawner(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.rpgHorseManager = rpgHorseManager;

		if (plugin.getConfig().getBoolean("horse-options.prevent-regular-horses")) {
			this.despawnAllHorses();
		} else {
			this.despawnAllRPGHorses();
		}
		this.reload();
	}

	public void reload() {
		int oldIdleTime = this.idleTime;
		this.idleTime = this.plugin.getConfig().getInt("horse-options.idle-time");
		this.despawnWhenOwnerPastDistance = this.plugin.getConfig().getInt("horse-options.despawn-when-owner-past-distance");

		if (this.idleTime != oldIdleTime) {
			this.startDespawnTask();
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

	public void startDespawnTask() {
		this.cancelDespawnTask();

		final HorseDespawner horseDespawner = this;
		this.despawnTask = new BukkitRunnable() {
			@Override
			public void run() {
				for (HorseOwner horseOwner : horseOwnerManager.getHorseOwners().values()) {
					RPGHorse currentHorse = horseOwner.getCurrentHorse();
					if (currentHorse == null) return;

					if (currentHorse.getHorse() == null || !currentHorse.getHorse().isValid()) {
						horseOwner.setCurrentHorse(null);
						continue;
					}

					Location currentLocation = currentHorse.getHorse().getLocation();
					Location lastLocation = currentHorse.getLastLocation();

					boolean isRidingHorse = horseOwner.isRidingHorse();

					if (!isRidingHorse && despawnWhenOwnerPastDistance >= 0) {
						Location ownerLocation = horseOwner.getPlayer().getLocation();
						if (!ownerLocation.getWorld().getUID().equals(currentLocation.getWorld().getUID()) || ownerLocation.distance(currentLocation) > despawnWhenOwnerPastDistance) {
							Bukkit.getScheduler().runTask(plugin, () -> {
								currentHorse.despawnEntity();
								horseOwner.setCurrentHorse(null);
							});
							continue;
						}
					}

					if (lastLocation == null || !lastLocation.getWorld().getName().equalsIgnoreCase(currentLocation.getWorld().getName()) || lastLocation.distanceSquared(currentLocation) > 1) {
						currentHorse.setLastMoveTime(System.currentTimeMillis());
						currentHorse.setLastLocation(currentLocation);
					} else if (System.currentTimeMillis() - currentHorse.getLastMoveTime() >= (idleTime * 1000L)) {
						Bukkit.getScheduler().runTask(plugin, () -> {
							currentHorse.despawnEntity();
							horseOwner.setCurrentHorse(null);
						});
						continue;
					}
				}
			}
		}.runTaskTimerAsynchronously(this.plugin, 0, 20L);
	}

	public void cancelDespawnTask() {
		if (this.despawnTask != null) {
			this.despawnTask.cancel();
			this.despawnTask = null;
		}
	}

}
