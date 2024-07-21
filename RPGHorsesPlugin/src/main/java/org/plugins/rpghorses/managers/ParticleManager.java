package org.plugins.rpghorses.managers;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;

import java.util.HashSet;

public class ParticleManager {

	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;

	private BukkitTask task;
	private int interval = 1, volume = 1;
	private double yOffset;
	@Getter
	private final HashSet<String> validParticles = new HashSet<>();
	@Getter
	private String particleList = "";

	public ParticleManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;

		this.reload();
		this.startTask();
	}

	public void startTask() {
		this.task = new BukkitRunnable() {
			@Override
			public void run() {
				for (HorseOwner horseOwner : horseOwnerManager.getHorseOwners().values()) {
					RPGHorse currentHorse = horseOwner.getCurrentHorse();
					if (currentHorse != null && currentHorse.getHorse().getPassenger() != null) {
						if (RPGHorsesMain.getVersion().getWeight() >= 9) {
							Particle particle = currentHorse.getParticle();
							if (particle != null) {
								Location loc = currentHorse.getHorse().getPassenger().getLocation();
								loc.getWorld().spawnParticle(particle, loc, volume, 0.1, yOffset, 0.1);
							}
						} else {
							Effect effect = ((LegacyHorseInfo) currentHorse.getHorseInfo()).getEffect();
							if (effect != null) {
								Location loc = currentHorse.getHorse().getLocation().add(0, yOffset, 0);
								loc.getWorld().playEffect(loc, effect, volume);
							}
						}
					}
				}
			}
		}.runTaskTimerAsynchronously(this.plugin, this.interval, this.interval);
	}

	public void reload() {
		this.reloadInterval();
		this.reloadVolume();
		this.reloadYOffset();
		this.setupValidParticles();
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

	public void setupValidParticles() {
		validParticles.clear();
		particleList = "";

		if (RPGHorsesMain.getVersion().getWeight() >= 9) {
			for (Particle particle : Particle.values()) {
				if (particle.getDataType() != Color.class && particle.getDataType() != Void.class)
					continue;

				validParticles.add(particle.name());
				particleList += particle.name() + ", ";
			}
		} else {
			for (Effect effect : Effect.values()) {
				if (effect != Effect.BLAZE_SHOOT && effect != Effect.BOW_FIRE && effect != Effect.CLICK1 && effect != Effect.CLICK2 && effect != Effect.DOOR_TOGGLE && effect != Effect.EXTINGUISH && effect != Effect.GHAST_SHOOT && effect != Effect.GHAST_SHRIEK && effect != Effect.RECORD_PLAY && effect != Effect.STEP_SOUND && effect != Effect.ZOMBIE_CHEW_IRON_DOOR && effect != Effect.ZOMBIE_CHEW_WOODEN_DOOR && effect != Effect.ZOMBIE_DESTROY_DOOR) {
					validParticles.add(effect.name());
					particleList += effect.name() + ", ";
				}
			}
		}

		particleList = particleList.substring(0, particleList.length() - 2);
	}

	public boolean isValidParticle(String arg) {
		return validParticles.contains(arg.toUpperCase());
	}

}
