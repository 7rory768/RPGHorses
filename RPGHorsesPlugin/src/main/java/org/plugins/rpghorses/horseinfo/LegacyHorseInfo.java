package org.plugins.rpghorses.horseinfo;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Effect;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;

import java.util.Random;

@Setter
@Getter
public class LegacyHorseInfo extends AbstractHorseInfo {

	private Horse.Variant variant;
	private Effect effect;

	public LegacyHorseInfo(Horse.Style style, Horse.Color color, Horse.Variant variant) {
		super(EntityType.HORSE, style, color);
		this.variant = variant;
	}

	@Override
	public AbstractHorseInfo populateNewRandomInfo() {
		Random random = new Random();

		Horse.Color color = this.color == null ? Horse.Color.values()[random.nextInt(Horse.Color.values().length)] : this.color;
		Horse.Style style = this.style == null ? Horse.Style.values()[random.nextInt(Horse.Style.values().length)] : this.style;
		Horse.Variant variant = this.variant == null ? Horse.Variant.values()[random.nextInt(Horse.Variant.values().length)] : this.variant;

		return new LegacyHorseInfo(style, color, this.variant);
	}
}
