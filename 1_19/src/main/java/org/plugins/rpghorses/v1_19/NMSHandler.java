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
		EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());
		try
		{
			Field selector;

			try
			{
				selector = PathfinderGoalSelector.class.getDeclaredField("d");
			} catch (NoSuchFieldException ignored)
			{
				selector = PathfinderGoalSelector.class.getDeclaredField("availableGoals");
			}

			Field b0F = EntityInsentient.class.getField("b0");
			Field bPF = EntityInsentient.class.getField("bP");

			b0F.setAccessible(true);
			bPF.setAccessible(true);

			Object b0 = b0F.get(creature);
			Object bP = bPF.get(creature);

			selector.setAccessible(true);
			selector.set(b0, Sets.newLinkedHashSet());
			selector.set(bP, Sets.newLinkedHashSet());
		} catch (Exception | Error e)
		{
			logError(e);
		}

	}
}
