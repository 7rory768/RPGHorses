package org.plugins.rpghorses.horseinfo;

import org.bukkit.Effect;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;

public class LegacyHorseInfo extends AbstractHorseInfo {
	
	private Horse.Variant variant;
	private Effect effect;
	
	public LegacyHorseInfo(Horse.Style style, Horse.Color color, Horse.Variant variant) {
		super(EntityType.HORSE, style, color);
		this.variant = variant;
	}
	
	public LegacyHorseInfo(Horse.Style style, Horse.Color color, Horse.Variant variant, Effect effect) {
		super(EntityType.HORSE, style, color);
		this.variant = variant;
		this.effect = effect;
	}
	
	public Horse.Variant getVariant() {
		return variant;
	}
	
	public void setVariant(Horse.Variant variant) {
		this.variant = variant;
	}
	
	public Effect getEffect() {
		return effect;
	}
	
	public void setEffect(Effect effect) {
		this.effect = effect;
	}
}
