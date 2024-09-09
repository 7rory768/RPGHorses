package org.plugins.rpghorses.v1_19;

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
		try
		{
			EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());

			Field selector;

			try
			{
				selector = PathfinderGoalSelector.class.getDeclaredField("d");
			} catch (NoSuchFieldException ignored)
			{
				selector = PathfinderGoalSelector.class.getDeclaredField("availableGoals");
			}

			Field bSf = EntityInsentient.class.getField("bS");
			Field bTf = EntityInsentient.class.getField("bT");

			bSf.setAccessible(true);
			bTf.setAccessible(true);

			Object bS = bSf.get(creature);
			Object bT = bTf.get(creature);

			selector.setAccessible(true);
			selector.set(bS, Sets.newLinkedHashSet());
			selector.set(bT, Sets.newLinkedHashSet());
		} catch (Exception | Error e)
		{
			logError(e);
		}

	}
}
