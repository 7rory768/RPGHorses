package org.plugins.rpghorses.horseinfo;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.plugins.rpghorses.RPGHorsesMain;

public abstract class AbstractHorseInfo {
	
	private EntityType entityType;
	private Horse.Style style;
	private Horse.Color color;
	
	public AbstractHorseInfo(EntityType entityType, Horse.Style style, Horse.Color color) {
		this.entityType = entityType;
		this.style = style;
		this.color = color;
	}
	
	public static AbstractHorseInfo getFromEntity(Entity entity) {
		if (entity.getType() == EntityType.HORSE) {
			Horse horse = (Horse) entity;
			Horse.Color color = horse.getColor();
			Horse.Style style = horse.getStyle();
			if (RPGHorsesMain.getVersion().getWeight() < 11) {
				return new LegacyHorseInfo(style, color, horse.getVariant());
			} else {
				return new HorseInfo(EntityType.HORSE, style, color);
			}
		} else if (RPGHorsesMain.getVersion().getWeight() >= 11 && entity.getType() == EntityType.LLAMA) {
			Llama llama = (Llama) entity;
			Horse.Color color = Horse.Color.WHITE;
			try {
				color = Horse.Color.valueOf(llama.getColor().name());
			} catch (IllegalArgumentException e) {}
			
			return new HorseInfo(EntityType.LLAMA, Horse.Style.NONE, color);
		}
		
		return new HorseInfo(entity.getType(), Horse.Style.NONE, Horse.Color.BROWN);
	}
	
	public EntityType getEntityType() {
		return entityType;
	}
	
	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}
	
	public Horse.Style getStyle() {
		return style;
	}
	
	public void setStyle(Horse.Style style) {
		this.style = style;
	}
	
	public Horse.Color getColor() {
		return color;
	}
	
	public void setColor(Horse.Color color) {
		this.color = color;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbstractHorseInfo)) return false;
		AbstractHorseInfo that = (AbstractHorseInfo) o;
		if (RPGHorsesMain.getVersion().getWeight() < 11 && ((LegacyHorseInfo) this).getVariant() != ((LegacyHorseInfo) that).getVariant()) {
			return false;
		}
		return entityType == that.entityType &&
				style == that.style &&
				color == that.color;
	}
}
