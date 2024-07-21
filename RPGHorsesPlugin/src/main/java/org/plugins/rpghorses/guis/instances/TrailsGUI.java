package org.plugins.rpghorses.guis.instances;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.TrailGUIItem;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;

import java.util.List;

@Getter
@Setter
public class TrailsGUI {

	private final RPGHorse rpgHorse;
	private final List<TrailsGUIPage> pages;

	public TrailsGUI(RPGHorse rpgHorse, List<TrailsGUIPage> pages, TrailGUIItem currentTrail) {
		this.rpgHorse = rpgHorse;
		this.pages = pages;
	}

	public TrailsGUIPage getPage(int page) {
		page = Math.min(pages.size(), Math.max(1, page));
		if (page >= 1 && this.pages.size() >= page) {
			return this.pages.get(page - 1);
		}
		return null;
	}

	public boolean applyTrail(TrailsGUIPage page, int slot) {
		TrailGUIItem trailGUIItem = page.getTrailGUIItem(slot);

		if (trailGUIItem == null || page.getUnknownTrails().contains(trailGUIItem)) return false;

		String trailName = trailGUIItem.getTrailName();

		if (RPGHorsesMain.getVersion().getWeight() < 9) {
			((LegacyHorseInfo) rpgHorse.getHorseInfo()).setEffect(Effect.valueOf(trailName.toUpperCase()));
		} else {
			rpgHorse.setParticle(Particle.valueOf(trailName.toUpperCase()));
		}

		for (TrailsGUIPage page1 : pages) {
			page1.setCurrentTrail(trailGUIItem);
		}

		return true;
	}

	public boolean removeTrail() {
		boolean hadTrail;

		for (TrailsGUIPage page : pages) {
			page.setCurrentTrail(null);
		}

		if (RPGHorsesMain.getVersion().getWeight() < 9) {
			hadTrail = ((LegacyHorseInfo) rpgHorse.getHorseInfo()).getEffect() != null;
			((LegacyHorseInfo) rpgHorse.getHorseInfo()).setEffect(null);
		} else {
			hadTrail = rpgHorse.getParticle() != null;
			rpgHorse.setParticle(null);
		}

		return hadTrail;
	}
}
