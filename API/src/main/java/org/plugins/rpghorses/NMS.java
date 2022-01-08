package org.plugins.rpghorses;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public abstract class NMS {

	public int errorCount;

	public abstract void removeBehaviour(LivingEntity entity);

	public void logError(Exception e) {
		if (++errorCount <= 10) {
			e.printStackTrace();
		} else {
			Bukkit.getLogger().warning("[RPGHorses] Failed to remove behaviour for horse, are you using a custom jar?");
		}
	}

}
