package org.plugins.rpghorses.v1_21;

import com.google.common.collect.Sets;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.plugins.rpghorses.NMS;

import java.lang.reflect.Field;
import java.util.EnumMap;

/*
 * @author Rory Skipper (Roree) on 2023-10-27
 */
public class NMSHandler extends NMS {

	@Override
	public void removeBehaviour(LivingEntity entity) {
		try {
			CraftEntity craftEntity = (CraftEntity) entity;
			EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());

			Field goalsField;
			Field goalsMapField;

			try {
				goalsField = PathfinderGoalSelector.class.getDeclaredField("c");
				goalsMapField = PathfinderGoalSelector.class.getDeclaredField("b");
			} catch (NoSuchFieldException ignored) {
				return;
			}

			Field selectorField1 = EntityInsentient.class.getField("bW");
			Field selectorField2 = EntityInsentient.class.getField("bX");

			selectorField1.setAccessible(true);
			selectorField2.setAccessible(true);

			Object selector1 = selectorField1.get(creature);
			Object selector2 = selectorField2.get(creature);

			goalsField.setAccessible(true);
			goalsField.set(selector1, Sets.newLinkedHashSet());
			goalsField.set(selector2, Sets.newLinkedHashSet());

			goalsMapField.setAccessible(true);
			goalsMapField.set(selector1, new EnumMap<>(PathfinderGoal.Type.class));
			goalsMapField.set(selector2, new EnumMap<>(PathfinderGoal.Type.class));
		} catch (Exception | Error e) {
			logError(e);
		}
	}

	@Override
	public void removeDurabilityEnchant(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setEnchantmentGlintOverride(null);
			item.setItemMeta(meta);
		}
	}
}
