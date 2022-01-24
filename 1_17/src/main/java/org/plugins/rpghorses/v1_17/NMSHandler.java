package org.plugins.rpghorses.v1_17;

import com.google.common.collect.Sets;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Field;

public class NMSHandler extends NMS {

	@Override
	public void removeBehaviour(LivingEntity entity) {
		EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());
		try {
			Field d = PathfinderGoalSelector.class.getDeclaredField("d");
			d.setAccessible(true);

			Field bP = EntityInsentient.class.getDeclaredField("bP");
			bP.setAccessible(true);

			Field bQ = EntityInsentient.class.getDeclaredField("bQ");
			bQ.setAccessible(true);

			d.set(creature.bO, Sets.newLinkedHashSet());
			d.set(creature.bP, Sets.newLinkedHashSet());
			//d.set(creature.bQ, Sets.newLinkedHashSet());
		} catch (Exception | Error e) {
			logError(e);
		}
	}
}
