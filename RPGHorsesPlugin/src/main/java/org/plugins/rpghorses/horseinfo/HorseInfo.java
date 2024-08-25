package org.plugins.rpghorses.horseinfo;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.managers.RPGHorseManager;

import java.util.List;
import java.util.Random;

public class HorseInfo extends AbstractHorseInfo {

	public HorseInfo(EntityType entityType, Horse.Style style, Horse.Color color) {
		super(entityType, style, color);
	}

	@Override
	public AbstractHorseInfo populateNewRandomInfo() {
		Random random = new Random();

		EntityType type = entityType;
		if (type == null) {
			RPGHorseManager rpgHorseManager = RPGHorsesMain.getInstance().getRpgHorseManager();
			if (rpgHorseManager != null) {
				List<EntityType> entityTypes = rpgHorseManager.getValidEntityTypes();
				entityTypes.remove(EntityType.LLAMA);
				type = entityTypes.get(random.nextInt(entityTypes.size()));
			} else {
				type = EntityType.HORSE;
			}
		}

		Horse.Color color = this.color == null ? Horse.Color.values()[random.nextInt(Horse.Color.values().length)] : this.color;
		Horse.Style style = this.style == null ? Horse.Style.values()[random.nextInt(Horse.Style.values().length)] : this.style;

		return new HorseInfo(type, style, color);
	}
}
