package org.plugins.rpghorses.horseinfo;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.plugins.rpghorses.RPGHorsesMain;

import java.util.HashSet;
import java.util.logging.Level;

@Setter
@Getter
public abstract class AbstractHorseInfo {

	protected EntityType entityType;
	protected Horse.Style style;
	protected Horse.Color color;

	protected HashSet<String> randomFields;

	public AbstractHorseInfo(EntityType entityType, Horse.Style style, Horse.Color color, HashSet<String> randomFields) {
		this.entityType = entityType;
		this.style = style;
		this.color = color;
		this.randomFields = randomFields;
	}

	public static AbstractHorseInfo loadFromConfig(ConfigurationSection config) {
		return loadFromConfig(config, true);
	}

	public static AbstractHorseInfo loadFromConfig(ConfigurationSection config, boolean fillDefaults) {
		EntityType entityType = null;
		Horse.Variant variant = null;
		Horse.Color color = null;
		Horse.Style style = null;
		HashSet<String> randomFields = new HashSet<>();

		if (fillDefaults || config.isSet("color")) {
			try {
				color = Horse.Color.valueOf(config.getString("color", "BROWN"));
			} catch (IllegalArgumentException e) {
				if (!config.getString("color").equalsIgnoreCase("RANDOM")) {
					Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + config.getCurrentPath() + ".color ( " + config.getString("color") + " is not a valid color )");
				} else {
					randomFields.add("color");
				}
			}
		}

		if (fillDefaults || config.isSet("style")) {
			try {
				style = Horse.Style.valueOf(config.getString("style", "NONE"));
			} catch (IllegalArgumentException e) {
				if (!config.getString("style").equalsIgnoreCase("RANDOM")) {
					Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + config.getCurrentPath() + ".style ( " + config.getString("style") + " is not a valid style )");
				} else {
					randomFields.add("style");
				}
			}
		}

		AbstractHorseInfo horseInfo;

		if (RPGHorsesMain.getVersion().getWeight() < 11) {
			if (fillDefaults || config.isSet("type")) {
				try {
					variant = Horse.Variant.valueOf(config.getString("type", "HORSE"));
				} catch (IllegalArgumentException e) {
					if (!config.getString("type").equalsIgnoreCase("RANDOM")) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + config.getCurrentPath() + ".type ( " + config.getString("type") + " is not a valid variant )");
					} else {
						randomFields.add("variant");
					}
				}
			}
			horseInfo = new LegacyHorseInfo(style, color, variant, randomFields);
		} else {
			if (fillDefaults || config.isSet("type")) {
				try {
					entityType = EntityType.valueOf(config.getString("type", "HORSE"));
				} catch (IllegalArgumentException e) {
					if (!config.getString("type").equalsIgnoreCase("RANDOM")) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + config.getCurrentPath() + ".type ( " + config.getString("type") + " is not a valid entityType )");
					} else {
						randomFields.add("type");
					}
				}
			}
			horseInfo = new HorseInfo(entityType, style, color, randomFields);
		}

		return horseInfo;
	}

	public static AbstractHorseInfo getFromEntity(Entity entity) {
		if (entity.getType() == EntityType.HORSE) {
			Horse horse = (Horse) entity;
			Horse.Color color = horse.getColor();
			Horse.Style style = horse.getStyle();
			if (RPGHorsesMain.getVersion().getWeight() < 11) {
				return new LegacyHorseInfo(style, color, horse.getVariant(), new HashSet<>());
			} else {
				return new HorseInfo(EntityType.HORSE, style, color, new HashSet<>());
			}
		} else if (RPGHorsesMain.getVersion().getWeight() >= 11 && entity.getType() == EntityType.LLAMA) {
			Llama llama = (Llama) entity;
			Horse.Color color = Horse.Color.WHITE;
			try {
				color = Horse.Color.valueOf(llama.getColor().name());
			} catch (IllegalArgumentException e) {
			}

			return new HorseInfo(EntityType.LLAMA, Horse.Style.NONE, color, new HashSet<>());
		}

		return new HorseInfo(entity.getType(), Horse.Style.NONE, Horse.Color.BROWN, new HashSet<>());
	}

	public abstract AbstractHorseInfo populateNewRandomInfo();

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
