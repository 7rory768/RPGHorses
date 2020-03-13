package org.plugins.rpghorses.managers;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;

public class ParticleManager {

    private final RPGHorsesMain plugin;
    private final HorseOwnerManager horseOwnerManager;

    private BukkitTask task;
    private int interval = 1, volume = 1;
    private double yOffset;

    public ParticleManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager) {
        this.plugin = plugin;
        this.horseOwnerManager = horseOwnerManager;

        this.reload();
        this.startTask();
    }

    public void reloadInterval() {
        this.interval = this.plugin.getConfig().getInt("horse-options.particles.interval");
    }

    public void reloadVolume() {
        this.volume = this.plugin.getConfig().getInt("horse-options.particles.volume");
    }

    public void reloadYOffset() {
        this.yOffset = this.plugin.getConfig().getDouble("horse-options.particles.y-offset");
    }

    public void reload() {
        this.reloadInterval();
        this.reloadVolume();
        this.reloadYOffset();
    }

    public void startTask() {
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                for (HorseOwner horseOwner : horseOwnerManager.getHorseOwners()) {
                    RPGHorse currentHorse = horseOwner.getCurrentHorse();
                    if (currentHorse != null) {
                        Particle particle = currentHorse.getParticle();
                        if (particle != null) {
                            Location loc = currentHorse.getHorse().getLocation().clone();
                            loc.getWorld().spawnParticle(particle, loc.add(0, yOffset, 0), volume, 0.1, 0, 0.1);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(this.plugin, this.interval, this.interval);
    }

    public String getParticleList() {
        String particleList = "";
        for (Particle particle : Particle.values()) {
            particleList += particle.name() + ", ";
        }

        return particleList.substring(0, particleList.length() - 2);
    }

}
