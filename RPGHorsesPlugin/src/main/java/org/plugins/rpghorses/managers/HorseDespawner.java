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

import java.util.HashMap;

public class HorseDespawner {

    private final RPGHorsesMain plugin;
    private final HorseOwnerManager horseOwnerManager;
    private final RPGHorseManager rpgHorseManager;

    private int idleTime = 5;
    private BukkitTask idleHorseTask;
    private HashMap<RPGHorse, Location> lastLocations = new HashMap<>();

    public HorseDespawner(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager) {
        this.plugin = plugin;
        this.horseOwnerManager = horseOwnerManager;
        this.rpgHorseManager = rpgHorseManager;

        // TODO: Make auto horse despawning optional
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
        for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners()) {
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

    public Location getLastLocation(RPGHorse rpgHorse) {
        return this.lastLocations.get(rpgHorse);
    }

    public void setLastLocations(HashMap<RPGHorse, Location> lastLocations) {
        this.lastLocations = lastLocations;
    }

    public void startIdleHorseTask() {
        this.cancelIdleHorseTask();

        // TODO: Edit how this works so it knows when each horse was last moved
        final HorseDespawner horseDespawner = this;
        this.idleHorseTask = new BukkitRunnable() {
            @Override
            public void run() {
                HashMap<RPGHorse, Location> lastLocations = new HashMap<>();
                for (HorseOwner horseOwner : horseDespawner.horseOwnerManager.getHorseOwners()) {
                    RPGHorse currentHorse = horseOwner.getCurrentHorse();
                    if (currentHorse != null) {
                        Location currentLocation = horseOwner.getLastHorseLocation();
                        Location lastLocation = horseDespawner.getLastLocation(currentHorse);
                        if (lastLocation != null && lastLocation.equals(currentLocation)) {
                            horseOwner.setCurrentHorse(null);
                        } else {
                            lastLocations.put(currentHorse, currentLocation);
                        }
                    }
                }
                horseDespawner.setLastLocations(lastLocations);
            }
        }.runTaskTimerAsynchronously(this.plugin, 0, this.idleTime * 20L);
    }

    public void cancelIdleHorseTask() {
        if (this.idleHorseTask != null) {
            this.idleHorseTask.cancel();
            this.idleHorseTask = null;
        }
    }

}
