package org.plugins.rpghorses.v1_19_4;

import com.google.common.collect.Sets;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Field;

public class NMSHandler extends NMS {

	@Override
	public void removeBehaviour(LivingEntity entity) {
		try {
			EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());

			Field selector;

			try {
				selector = PathfinderGoalSelector.class.getDeclaredField("d");
			} catch (NoSuchFieldException ignored) {
				selector = PathfinderGoalSelector.class.getDeclaredField("availableGoals");
			}

			Field bNf = EntityInsentient.class.getField("bN");
			Field bOf = EntityInsentient.class.getField("bO");

			bNf.setAccessible(true);
			bOf.setAccessible(true);

			Object bN = bNf.get(creature);
			Object bO = bOf.get(creature);

			selector.setAccessible(true);
			selector.set(bN, Sets.newLinkedHashSet());
			selector.set(bO, Sets.newLinkedHashSet());
		} catch (Exception | Error e) {
			logError(e);
		}

	}
}
