package org.plugins.rpghorses.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.plugins.rpghorses.horses.RPGHorse;

@Getter
@Setter
public class RPGHorsePostSpawnEvent extends PlayerEvent {

	private static final HandlerList HANDLERS = new HandlerList();

	private final RPGHorse     horse;
	private final LivingEntity entity;

	public RPGHorsePostSpawnEvent(Player player, RPGHorse horse) {
		super(player);
		this.horse = horse;
		this.entity = horse.getHorse();
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}
}
