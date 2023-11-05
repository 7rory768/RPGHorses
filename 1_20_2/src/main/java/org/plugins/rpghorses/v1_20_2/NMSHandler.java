package org.plugins.rpghorses.v1_20_2;

import com.google.common.collect.Sets;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Field;

/*
 * @author Rory Skipper (Roree) on 2023-10-27
 */
public class NMSHandler extends NMS {

	@Override
	public void removeBehaviour(LivingEntity entity) {
		try {
			EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());

			Field goalsField;

			try {
				goalsField = PathfinderGoalSelector.class.getDeclaredField("d");
			} catch (NoSuchFieldException ignored) {
				goalsField = PathfinderGoalSelector.class.getDeclaredField("availableGoals");
			}

			Field selectorField1 = EntityInsentient.class.getField("bO");
			Field selectorField2 = EntityInsentient.class.getField("bP");

			selectorField1.setAccessible(true);
			selectorField2.setAccessible(true);

			Object selector1 = selectorField1.get(creature);
			Object selector2 = selectorField2.get(creature);

			goalsField.setAccessible(true);
			goalsField.set(selector1, Sets.newLinkedHashSet());
			goalsField.set(selector2, Sets.newLinkedHashSet());
		} catch (Exception | Error e) {
			logError(e);
		}
	}
}
