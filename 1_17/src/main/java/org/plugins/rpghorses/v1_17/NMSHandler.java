package org.plugins.rpghorses.v1_17;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.plugins.rpghorses.NMS;

public class NMSHandler implements NMS {
	
	@Override
	public void removeBehaviour(LivingEntity entity) {
		try {
			((Mob) entity).setAware(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
