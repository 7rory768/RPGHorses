package org.plugins.rpghorses;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class NMS
{

	public int errorCount;

	public abstract void removeBehaviour(LivingEntity entity);

	public final void addGlow(ItemStack item)
	{
		ItemMeta meta = item.getItemMeta();
		addGlow(meta);
		item.setItemMeta(meta);
	}

	public void addGlow(ItemMeta meta)
	{
		meta.addEnchant(Enchantment.DURABILITY, 3, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
	}

	public final void removeGlow(ItemStack item)
	{
		ItemMeta meta = item.getItemMeta();
		removeGlow(meta);
		item.setItemMeta(meta);
	}

	public void removeGlow(ItemMeta meta)
	{
		meta.removeEnchant(Enchantment.DURABILITY);
		meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
	}

	public void logError(Throwable e)
	{
		if (++errorCount <= 10)
		{
			e.printStackTrace();
		} else
		{
			Bukkit.getLogger().warning("[RPGHorses] Failed to remove behaviour for horse, are you using a custom jar?");
		}
	}

}
