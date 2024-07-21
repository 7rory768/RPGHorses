package org.plugins.rpghorses;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class NMS {

	public int errorCount;

	public abstract void removeBehaviour(LivingEntity entity);

	public void removeDurabilityEnchant(ItemStack item) {
		item.removeEnchantment(Enchantment.DURABILITY);
		ItemMeta meta = item.getItemMeta();
		meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
	}

	public void logError(Throwable e) {
		if (++errorCount <= 10) {
			e.printStackTrace();
		} else {
			Bukkit.getLogger().warning("[RPGHorses] Failed to remove behaviour for horse, are you using a custom jar?");
		}
	}

}
