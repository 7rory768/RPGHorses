package org.plugins.rpghorses.v1_21;


import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/*
 * @author Rory Skipper (Roree) on 2023-10-27
 */
public class NMSHandler extends NMS {

	protected Method mobGoalsMethod;

	public NMSHandler() {
		try {
			mobGoalsMethod = GoalSelector.class.getDeclaredMethod("b");
			mobGoalsMethod.setAccessible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void removeBehaviour(LivingEntity entity) {
		try {
			Mob entityInsentient = (Mob) ((CraftEntity) entity).getHandle();
			removeAllGoals(entityInsentient.goalSelector);
			removeAllGoals(entityInsentient.targetSelector);
		} catch (Exception | Error e) {
			logError(e);
		}
	}

	public void removeAllGoals(GoalSelector selector) {
		try {
			Set<WrappedGoal> goals = (Set<WrappedGoal>) mobGoalsMethod.invoke(selector);
			Iterator<WrappedGoal> goalIterator = goals.iterator();

			while (goalIterator.hasNext()) {
				WrappedGoal pathGoal = goalIterator.next();
				if (pathGoal.isRunning()) pathGoal.stop();

				goalIterator.remove();
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
