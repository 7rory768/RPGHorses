package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.managers.gui.StableGUIManager;

public class EntityDamageByEntityListener implements Listener {
	
	private final RPGHorseManager rpgHorseManager;
	private final StableGUIManager stableGuiManager;
	
	public EntityDamageByEntityListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager) {
		this.rpgHorseManager = rpgHorseManager;
		this.stableGuiManager = stableGuiManager;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity victim = e.getEntity();
		Entity damager = e.getDamager();
		if (this.rpgHorseManager.isValidEntityType(victim.getType())) {
			EntityType damagerType = damager.getType(), victimType = victim.getType();
			Player damagerPlayer = null;
			
			if (damager.getType() == EntityType.PLAYER) {
				damagerPlayer = (Player) damager;
			} else if (damagerType == EntityType.ARROW || damagerType == EntityType.EGG || damagerType == EntityType.ENDER_PEARL || damagerType == EntityType.FISHING_HOOK || damagerType == EntityType.SPLASH_POTION || damagerType == EntityType.SNOWBALL) {
				Projectile projectile = (Projectile) e.getDamager();
				ProjectileSource projectileSource = projectile.getShooter();
				if (projectileSource instanceof Player) damagerPlayer = (Player) projectileSource;
			}
			
			if (damagerPlayer != null) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		Entity damaged = e.getEntity();
		if (this.rpgHorseManager.isValidEntityType(damaged.getType())) {
			RPGHorse rpgHorse = this.rpgHorseManager.getRPGHorse(damaged);
			if (rpgHorse != null) {
				rpgHorse.loadHealth();
				this.stableGuiManager.updateRPGHorse(rpgHorse);
				
			}
		}
	}
	
}
