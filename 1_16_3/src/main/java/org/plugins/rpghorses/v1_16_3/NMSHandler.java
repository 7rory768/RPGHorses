package org.plugins.rpghorses.v1_16_3;

import com.google.common.collect.Sets;
import net.minecraft.server.v1_16_R2.EntityCreature;
import net.minecraft.server.v1_16_R2.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Field;

public class NMSHandler implements NMS {
	
	@Override
	public void removeBehaviour(LivingEntity entity) {
		EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());
		try {
			Field d = PathfinderGoalSelector.class.getDeclaredField("d");
			d.setAccessible(true);
			d.set(creature.goalSelector, Sets.newLinkedHashSet());
			d.set(creature.targetSelector, Sets.newLinkedHashSet());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
