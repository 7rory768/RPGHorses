package org.plugins.rpghorses.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RPGHorseClaimEvent extends Event implements Cancellable {
	
	private static final HandlerList HANDLERS = new HandlerList();
	boolean cancelled = false;
	private final Player player;
	private final Entity entity;
	
	public RPGHorseClaimEvent(Player player, Entity entity) {
		this.player = player;
		this.entity = entity;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Entity getEntity() {
		return entity;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}
	
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
