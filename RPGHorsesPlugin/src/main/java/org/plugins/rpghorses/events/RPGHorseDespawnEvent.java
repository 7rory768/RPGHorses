package org.plugins.rpghorses.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@Getter
@Setter
public class RPGHorseDespawnEvent extends PlayerEvent {

	private static final HandlerList HANDLERS = new HandlerList();
	private final        Entity      entity;

	public RPGHorseDespawnEvent(Player player, Entity entity) {
		super(player);
		this.entity = entity;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}
}
