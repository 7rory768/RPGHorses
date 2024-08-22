package org.plugins.rpghorses.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.plugins.rpghorses.horses.RPGHorse;

@Getter
@Setter
public class RPGHorsePreSpawnEvent extends PlayerEvent implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();

	private final RPGHorse horse;
	private final Location location;

	private boolean cancelled = false;

	public RPGHorsePreSpawnEvent(Player player, RPGHorse horse, Location location) {
		super(player);
		this.horse = horse;
		this.location = location;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}
}
