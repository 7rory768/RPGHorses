package org.plugins.rpghorses.horseinfo;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.managers.RPGHorseManager;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class HorseInfo extends AbstractHorseInfo {

	public HorseInfo(EntityType entityType, Horse.Style style, Horse.Color color) {
		this(entityType, style, color, new HashSet<>());
	}

	public HorseInfo(EntityType entityType, Horse.Style style, Horse.Color color, HashSet<String> randomFields) {
		super(entityType, style, color, randomFields);
	}

	@Override
	public AbstractHorseInfo populateNewRandomInfo() {
		Random random = new Random();

		EntityType type = this.entityType;
		if (randomFields.contains("type")) {
			RPGHorseManager rpgHorseManager = RPGHorsesMain.getInstance().getRpgHorseManager();
			if (rpgHorseManager != null) {
				List<EntityType> entityTypes = rpgHorseManager.getValidEntityTypes();
				entityTypes.remove(EntityType.LLAMA);
				type = entityTypes.get(random.nextInt(entityTypes.size()));
			} else {
				type = EntityType.HORSE;
			}
		}

		Horse.Color color = randomFields.contains("color") ? Horse.Color.values()[random.nextInt(Horse.Color.values().length)] : this.color;
		Horse.Style style = randomFields.contains("style") ? Horse.Style.values()[random.nextInt(Horse.Style.values().length)] : this.style;

		return new HorseInfo(type, style, color, randomFields);
	}
}
