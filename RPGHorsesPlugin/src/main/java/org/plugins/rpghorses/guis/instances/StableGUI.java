package org.plugins.rpghorses.guis.instances;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.ItemUtil;

import java.util.HashMap;
import java.util.List;

public class StableGUI {

	private HorseOwner horseOwner;

	private List<StableGUIPage> stableGUIPages;

	public StableGUI(HorseOwner horseOwner, List<StableGUIPage> stableGUIPages) {
		this.horseOwner = horseOwner;
		this.stableGUIPages = stableGUIPages;
	}

	public HorseOwner getHorseOwner() {
		return horseOwner;
	}

	public List<StableGUIPage> getStableGUIPages() {
		return stableGUIPages;
	}

	public void setStableGUIPages(List<StableGUIPage> stableGUIPages) {
		this.stableGUIPages = stableGUIPages;
	}

	public StableGUIPage getPage(int page) {
		if (page >= 1 && this.stableGUIPages.size() >= page) {
			return this.stableGUIPages.get(page - 1);
		}
		return null;
	}

	public void addGlow(RPGHorse rpgHorse) {
		RPGHorse oldHorse = this.horseOwner.getCurrentHorse();
		if (oldHorse != rpgHorse) {
			boolean foundOldHorse = oldHorse == null;
			boolean foundNewHorse = rpgHorse == null;
			for (StableGUIPage stableGUIPage : this.stableGUIPages) {
				HashMap<Integer, RPGHorse> rpgHorses = stableGUIPage.getHorseSlots();
				for (Integer slot : rpgHorses.keySet()) {
					RPGHorse loopHorse = rpgHorses.get(slot);
					if (loopHorse == oldHorse) {
						ItemStack item = stableGUIPage.getGUI().getItem(slot);
						ItemUtil.removeDurabilityGlow(item);
						foundOldHorse = true;
						if (foundNewHorse) {
							return;
						}
					} else if (loopHorse == rpgHorse) {
						ItemStack item = stableGUIPage.getGUI().getItem(slot);
						ItemUtil.addDurabilityGlow(item);
						if (this.horseOwner.isInGUI(GUILocation.STABLE_GUI) && this.horseOwner.getCurrentStableGUIPage() == stableGUIPage) {
							this.horseOwner.openStableGUIPage(stableGUIPage.getPageNum());
						}
						foundNewHorse = true;
						if (foundOldHorse) {
							return;
						}
					}
				}
			}
		}
	}
}
