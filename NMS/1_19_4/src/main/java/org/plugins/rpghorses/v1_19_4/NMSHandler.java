package org.plugins.rpghorses.v1_19_4;

import com.google.common.collect.Sets;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
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
			// https://nms.screamingsandals.org/1.19.4/net/minecraft/world/entity/Mob.html
			Field targetSelector = EntityInsentient.class.getField("bO");
			Field goalSelector = EntityInsentient.class.getField("bN");

			targetSelector.setAccessible(true);
			goalSelector.setAccessible(true);

			Object bO = targetSelector.get(creature);
			Object bN = goalSelector.get(creature);

			selector.setAccessible(true);
			selector.set(bO, Sets.newLinkedHashSet());
			selector.set(bN, Sets.newLinkedHashSet());
		} catch (Exception | Error e) {
			logError(e);
		}
	}
}
