package org.plugins.rpghorses.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.tiers.Tier;
import org.plugins.rpghorses.utils.RPGMessagingUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class XPManager {

	private final RPGHorsesMain plugin;
	private final RPGHorseManager rpgHorseManager;
	private final RPGMessagingUtil messagingUtil;

	private BukkitTask xpTask;

	public XPManager(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.rpgHorseManager = rpgHorseManager;
		this.messagingUtil = messagingUtil;

		start();
	}

	public void start() {
		cancel();

		xpTask = new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					HorseOwner horseOwner = rpgHorseManager.getHorseOwnerManager().getHorseOwner(player);
					if (horseOwner == null) continue;

					RPGHorse rpgHorse = horseOwner.getCurrentHorse();
					if (rpgHorse == null) continue;

					Tier tier = rpgHorseManager.getNextTier(rpgHorse);
					if (tier == null) return;

					// Prevent message spam
					if (rpgHorse.getXp() >= tier.getExpCost()) continue;

					Player p = horseOwner.getPlayer();
					Location newLoc = p.getLocation();
					Location oldLoc = horseOwner.getLastHorseLocation();
					newLoc.setY(oldLoc.getY());
					rpgHorse.increaseXp(newLoc.distance(oldLoc));

					if (rpgHorse.getXp() >= tier.getExpCost()) {
						messagingUtil.sendMessageAtPath(p, "messages.max-xp", rpgHorse);
						continue;
					}

					horseOwner.setLastHorseLocation(newLoc);
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
