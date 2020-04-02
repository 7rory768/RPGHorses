package org.plugins.rpghorses.v1_11_2;

import com.google.common.collect.Sets;
import net.minecraft.server.v1_11_R1.EntityCreature;
import net.minecraft.server.v1_11_R1.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Field;

public class NMSHandler implements NMS {

	@Override
	public void removeBehaviour(LivingEntity entity) {
		EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());
		try {
			Field b = PathfinderGoalSelector.class.getDeclaredField("b");
			b.setAccessible(true);
			b.set(creature.goalSelector, Sets.newLinkedHashSet());
			b.set(creature.targetSelector, Sets.newLinkedHashSet());

			Field c = PathfinderGoalSelector.class.getDeclaredField("c");
			c.setAccessible(true);
			c.set(creature.goalSelector, Sets.newLinkedHashSet());
			c.set(creature.targetSelector, Sets.newLinkedHashSet());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}