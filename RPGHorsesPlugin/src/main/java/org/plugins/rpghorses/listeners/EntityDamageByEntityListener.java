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
import org.plugins.rpghorses.utils.WorldGuardUtil;

public class EntityDamageByEntityListener implements Listener {
	
	private final RPGHorsesMain plugin;
	private final RPGHorseManager rpgHorseManager;
	private final StableGUIManager stableGuiManager;
	
	public EntityDamageByEntityListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager) {
		this.plugin = plugin;
		this.rpgHorseManager = rpgHorseManager;
		this.stableGuiManager = stableGuiManager;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Entity victim = e.getEntity();
		Entity damager = e.getDamager();
		RPGHorse rpgHorse = this.rpgHorseManager.getRPGHorse(victim);
		if (rpgHorse != null) {
			EntityType damagerType = damager.getType(), victimType = victim.getType();
			Player damagerPlayer = null;
			
			if (damager.getType() == EntityType.PLAYER) {
				damagerPlayer = (Player) damager;
			} else if (damager instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				ProjectileSource projectileSource = projectile.getShooter();
				if (projectileSource instanceof Player) damagerPlayer = (Player) projectileSource;
			}

			// Not PVP
			if (damagerPlayer == null) return;
			if (damagerPlayer.getUniqueId().equals(rpgHorse.getHorseOwner().getUUID())) {
				e.setCancelled(true);
				return;
			}
			
			boolean horsePVP = plugin.getConfig().getBoolean("horse-options.horse-pvp", false) && WorldGuardUtil.isHorsePVPAllowed(null, victim.getLocation()) && WorldGuardUtil.isHorsePVPAllowed(null, damagerPlayer.getLocation());
			
			if (!horsePVP) {
				plugin.getMessagingUtil().sendMessageAtPath(damagerPlayer, "messages.no-horse-pvp");
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
