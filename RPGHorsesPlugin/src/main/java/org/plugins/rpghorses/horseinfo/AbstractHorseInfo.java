package org.plugins.rpghorses.horseinfo;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.plugins.rpghorses.RPGHorsesMain;

public abstract class AbstractHorseInfo {

	private EntityType  entityType;
	private Horse.Style style;
	private Horse.Color color;

	public AbstractHorseInfo(EntityType entityType, Horse.Style style, Horse.Color color) {
		this.entityType = entityType;
		this.style = style;
		this.color = color;
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
