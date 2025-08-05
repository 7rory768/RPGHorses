package org.plugins.rpghorses.v1_21_5;


import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;
import org.plugins.rpghorses.NMS;

/*
 * @author Rory Skipper (Roree) on 2025-03-17
 */
public class NMSHandler extends NMS {

	public NMSHandler() {
	}

	@Override
	public void removeBehaviour(LivingEntity entity) {
		try {
			Mob entityInsentient = (Mob) ((CraftEntity) entity).getHandle();
			removeAllGoals(entityInsentient);
		} catch (Exception | Error e) {
			logError(e);
		}
	}

	public void removeAllGoals(Mob mob) {
		try {
			for (WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
				mob.goalSelector.removeGoal(goal);
			}
			for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
				mob.targetSelector.removeGoal(goal);
			}
		} catch (Exception e) {
			logError(e);
		}
	}

	@Override
	public void addGlow(ItemMeta meta) {
		meta.setEnchantmentGlintOverride(true);
	}

	@Override
	public void removeGlow(ItemMeta meta) {
		if (meta.hasEnchantmentGlintOverride()) {
			meta.setEnchantmentGlintOverride(null);
		}
	}
}
