package org.plugins.rpghorses.players;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;

public class RemoveHorseConfirmation {
	
	private final RPGHorsesMain plugin;
	private final RPGHorseManager rpgHorseManager;
	
	private HorseOwner horseOwner;
	private RPGHorse rpgHorse;
	private BukkitTask timeoutTask;
	
	public RemoveHorseConfirmation(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, HorseOwner horseOwner, RPGHorse rpgHorse) {
		this.plugin = plugin;
		this.rpgHorseManager = rpgHorseManager;
		this.horseOwner = horseOwner;
		this.rpgHorse = rpgHorse;
		
		this.startTask();
	}
	
	public HorseOwner getHorseOwner() {
		return horseOwner;
	}
	
	public RPGHorse getRPGHorse() {
		return this.rpgHorse;
	}
	
	public void startTask() {
		this.endTask();
		
		this.timeoutTask = new BukkitRunnable() {
			@Override
			public void run() {
				rpgHorseManager.removeRemoveConfirmation(horseOwner.getUUID());
			}
		}.runTaskLater(this.plugin, 20L * 20);
	}
	
	public void endTask() {
		if (this.timeoutTask != null) {
			this.timeoutTask.cancel();
			this.timeoutTask = null;
		}
	}
}
