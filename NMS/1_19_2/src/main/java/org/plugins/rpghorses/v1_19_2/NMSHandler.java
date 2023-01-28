package org.plugins.rpghorses.v1_19_2;

import com.google.common.collect.Sets;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Field;

public class NMSHandler extends NMS
{

	@Override
	public void removeBehaviour(LivingEntity entity)
	{
		EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());
		try {
			Field selector;

			try {
				selector = PathfinderGoalSelector.class.getDeclaredField("d");
			} catch (NoSuchFieldException ignored) {
				selector = PathfinderGoalSelector.class.getDeclaredField("availableGoals");
			}
			// https://nms.screamingsandals.org/1.19.2/net/minecraft/world/entity/Mob.html
			Field targetSelector = EntityInsentient.class.getField("bT");
			Field goalSelector = EntityInsentient.class.getField("bS");

			targetSelector.setAccessible(true);
			goalSelector.setAccessible(true);

			Object bT = targetSelector.get(creature);
			Object bS = goalSelector.get(creature);

			selector.setAccessible(true);
			selector.set(bT, Sets.newLinkedHashSet());
			selector.set(bS, Sets.newLinkedHashSet());
		} catch (Exception | Error e) {
			logError(e);
		}
	}
}
