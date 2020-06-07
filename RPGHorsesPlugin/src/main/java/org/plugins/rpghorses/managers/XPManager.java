package org.plugins.rpghorses.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class XPManager {
	
	private final RPGHorsesMain plugin;
	private final RPGHorseManager rpgHorseManager;
	private final RPGMessagingUtil messagingUtil;
	
	private HashSet<HorseOwner> horseOwners = new HashSet<>();
	private BukkitTask xpTask;
	
	public XPManager(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.rpgHorseManager = rpgHorseManager;
		this.messagingUtil = messagingUtil;
		
		start();
	}
	
	public void addHorseOwner(HorseOwner horseOwner) {
		RPGHorse rpgHorse = horseOwner.getCurrentHorse();
		if (rpgHorse != null && rpgHorse.getTier() < rpgHorseManager.getMaxTier() && rpgHorseManager.getXPNeededToUpgrade(rpgHorse) > rpgHorse.getXp()) {
			this.horseOwners.add(horseOwner);
		}
	}
	
	public void removeHorseOwner(HorseOwner horseOwner) {
		this.horseOwners.remove(horseOwner);
	}
	
	public void start() {
		cancel();
		
		xpTask = new BukkitRunnable() {
			@Override
			public void run() {
				List<HorseOwner> toRemove = new ArrayList<>();
				for (HorseOwner horseOwner : horseOwners) {
					Player p = horseOwner.getPlayer();
					Location newLoc = p.getLocation();
					Location oldLoc = horseOwner.getLastHorseLocation();
					RPGHorse rpgHorse = horseOwner.getCurrentHorse();
					if (rpgHorse == null) {
						toRemove.add(horseOwner);
						continue;
					}
					rpgHorse.increaseXp(newLoc.distance(oldLoc));
					
					if (rpgHorse.getXp() >= rpgHorseManager.getXPNeededToUpgrade(rpgHorse)) {
						messagingUtil.sendMessageAtPath(p, "messages.max-xp", rpgHorse);
						toRemove.add(horseOwner);
					}
					
					horseOwner.setLastHorseLocation(newLoc);
				}
				
				for (HorseOwner horseOwner : toRemove) {
					removeHorseOwner(horseOwner);
				}
			}
		}.runTaskTimerAsynchronously(plugin, 0L, 20L);
	}
	
	
	public void cancel() {
		if (xpTask != null) {
			xpTask.cancel();
			xpTask = null;
		}
	}
}
