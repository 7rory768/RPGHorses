package org.plugins.rpghorses.players;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.instances.*;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.SQLManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HorseOwner {
	
	GUILocation guiLocation = GUILocation.NONE;
	private UUID uuid;
	private List<RPGHorse> rpgHorses;
	private RPGHorse currentHorse;
	private boolean spawningHorse, mountingHorse, deMountingHorse, changingHorse, receivedDefaultHorse, autoMount = true;
	private Location lastHorseLocation;
	private StableGUI stableGUI;
	private StableGUIPage stableGUIPage;
	private MarketGUIPage marketGUIPage;
	private YourHorsesGUI yourHorsesGUI;
	private YourHorsesGUIPage yourHorsesGUIPage;
	private HorseGUI horseGUI;
	private TrailsGUI trailsGUI;
	private SellGUI sellGUI;
	
	public HorseOwner(Player p) {
		this(p.getUniqueId());
	}
	
	public HorseOwner(UUID uuid) {
		this(uuid, new ArrayList<>());
	}
	
	public HorseOwner(UUID uuid, List<RPGHorse> rpgHorses) {
		this.uuid = uuid;
		this.rpgHorses = rpgHorses;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public List<RPGHorse> getRPGHorses() {
		return this.rpgHorses;
	}
	
	public RPGHorse getRPGHorse(int index) {
		if (index >= 0 && index < this.rpgHorses.size()) {
			return this.rpgHorses.get(index);
		}
		return null;
	}
	
	public void addRPGHorse(RPGHorse rpgHorse) {
		addRPGHorse(rpgHorse, true);
	}
	
	public void addRPGHorse(RPGHorse rpgHorse, boolean sql) {
		this.rpgHorses.add(rpgHorse);
		rpgHorse.setIndex(rpgHorses.size() - 1);
		rpgHorse.setHorseOwner(this);
		
		SQLManager sqlManager = RPGHorsesMain.getInstance().getSQLManager();
		if (sqlManager != null && sql) sqlManager.addHorse(rpgHorse);
	}
	
	public RPGHorse removeRPGHorse(int index) {
		if (index >= 0 && index < this.rpgHorses.size()) {
			RPGHorse rpgHorse = this.rpgHorses.get(index);
			this.removeRPGHorse(rpgHorse);
			return rpgHorse;
		}
		return null;
	}
	
	public RPGHorse removeRPGHorse(int index, boolean sqlRemove) {
		if (index >= 0 && index < this.rpgHorses.size()) {
			RPGHorse rpgHorse = this.rpgHorses.get(index);
			this.removeRPGHorse(rpgHorse, sqlRemove);
			return rpgHorse;
		}
		return null;
	}
	
	public void removeRPGHorse(RPGHorse rpgHorse) {
		removeRPGHorse(rpgHorse, true);
	}
	
	
	public void removeRPGHorse(RPGHorse rpgHorse, boolean sqlRemove) {
		if (rpgHorse == this.currentHorse) {
			this.setCurrentHorse(null);
		}
		this.rpgHorses.remove(rpgHorse);
		
		int index = rpgHorse.getIndex();
		for (RPGHorse otherHorse : rpgHorses) {
			if (otherHorse.getIndex() > index) {
				otherHorse.setIndex(otherHorse.getIndex() - 1);
			}
		}
		
		if (sqlRemove) {
			SQLManager sqlManager = RPGHorsesMain.getInstance().getSQLManager();
			if (sqlManager != null) sqlManager.removeHorse(rpgHorse);
		}
	}
	
	public RPGHorse getCurrentHorse() {
		return currentHorse;
	}
	
	public void setCurrentHorse(RPGHorse rpgHorse) {
		if (this.currentHorse != rpgHorse) {
			if (this.currentHorse != null) {
				if (rpgHorse != null) {
					LivingEntity horse = this.currentHorse.getHorse();
					if (horse != null) {
						if (RPGHorsesMain.getVersion().getWeight() < 11) {
							if (horse.getPassenger() != null && horse.getPassenger().getUniqueId() == this.uuid) {
								this.setChangingHorse(true);
							}
						} else {
							for (Entity entity : horse.getPassengers()) {
								if (entity.getUniqueId() == this.uuid) {
									this.setChangingHorse(true);
									break;
								}
							}
						}
					}
				}
				this.currentHorse.despawnEntity();
			}
			
			if (rpgHorse != null) {
				this.stableGUI.addGlow(rpgHorse);
			}
			
			this.currentHorse = rpgHorse;
			
			if (rpgHorse != null) {
				this.currentHorse.spawnEntity();
			}
		}
		
	}
	
	public int getHorseNumber(RPGHorse rpgHorse) {
		for (int i = 0; i < this.rpgHorses.size(); i++) {
			if (this.rpgHorses.get(i) == rpgHorse) {
				return i + 1;
			}
		}
		return -1;
	}
	
	public void openStableGUIPage(int pageNum) {
		StableGUIPage stableGUIPage = this.stableGUI.getPage(pageNum);
		this.openStableGUIPage(stableGUIPage);
	}
	
	public void openStableGUIPage(StableGUIPage stableGUIPage) {
		if (stableGUIPage != null) {
			getPlayer().openInventory(stableGUIPage.getGUI());
			guiLocation = GUILocation.STABLE_GUI;
			this.stableGUIPage = stableGUIPage;
		}
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(uuid);
	}
	
	public String getPlayerName() {
		OfflinePlayer player = getPlayer();
		if (player != null) {
			return player.getName();
		} else {
			player = Bukkit.getOfflinePlayer(uuid);
			if (player != null && player.hasPlayedBefore()) {
				return player.getName();
			}
		}
		return "";
	}
	
	public StableGUI getStableGUI() {
		return stableGUI;
	}
	
	public void setStableGUI(StableGUI stableGUI) {
		this.stableGUI = stableGUI;
		if (guiLocation == GUILocation.STABLE_GUI && this.stableGUIPage != null) {
			int pageNum = this.stableGUIPage.getPageNum();
			while (pageNum > 0) {
				StableGUIPage stableGUIPage = stableGUI.getPage(pageNum--);
				if (stableGUIPage != null) {
					this.openStableGUIPage(stableGUIPage);
					break;
				}
			}
		}
	}
	
	public StableGUIPage getCurrentStableGUIPage() {
		return this.stableGUIPage;
	}
	
	public void openYourHorsesGUIPage(int pageNum) {
		YourHorsesGUIPage yourHorsesGUIPage = this.yourHorsesGUI.getPage(pageNum);
		this.openYourHorsesGUIPage(yourHorsesGUIPage);
	}
	
	public void openYourHorsesGUIPage(YourHorsesGUIPage yourHorsesGUIPage) {
		if (yourHorsesGUIPage != null) {
			getPlayer().openInventory(yourHorsesGUIPage.getGUI());
			guiLocation = guiLocation.YOUR_HORSES_GUI;
			this.yourHorsesGUIPage = yourHorsesGUIPage;
		}
	}
	
	public YourHorsesGUI getYourHorsesGUI() {
		return yourHorsesGUI;
	}
	
	public void setYourHorsesGUI(YourHorsesGUI yourHorsesGUI) {
		this.yourHorsesGUI = yourHorsesGUI;
		if (guiLocation == GUILocation.YOUR_HORSES_GUI) {
			int pageNum = this.yourHorsesGUIPage.getPageNum();
			while (pageNum > 0) {
				YourHorsesGUIPage yourHorsesGUIPage = yourHorsesGUI.getPage(pageNum--);
				if (yourHorsesGUIPage != null) {
					this.openYourHorsesGUIPage(yourHorsesGUIPage);
					break;
				}
			}
		}
	}
	
	public YourHorsesGUIPage getCurrentYourHorsesGUIPage() {
		return this.yourHorsesGUIPage;
	}
	
	public void openMarketGUIPage(MarketGUIPage marketGUIPage) {
		if (marketGUIPage != null) {
			new BukkitRunnable() {
				@Override
				public void run() {
					getPlayer().openInventory(marketGUIPage.getGUI());
					guiLocation = GUILocation.MARKET_GUI;
				}
			}.runTask(RPGHorsesMain.getInstance());
			this.marketGUIPage = marketGUIPage;
		}
	}
	
	public MarketGUIPage getCurrentMarketGUIPage() {
		return marketGUIPage;
	}
	
	public void openHorseGUI(HorseGUI horseGUI) {
		this.horseGUI = horseGUI;
		getPlayer().openInventory(horseGUI.getInventory());
		setGUILocation(GUILocation.HORSE_GUI);
	}
	
	public HorseGUI getHorseGUI() {
		return horseGUI;
	}
	
	public void openTrailsGUI(TrailsGUI trailsGUI) {
		this.trailsGUI = trailsGUI;
		getPlayer().openInventory(trailsGUI.getInventory());
		setGUILocation(GUILocation.TRAILS_GUI);
	}
	
	public TrailsGUI getTrailsGUI() {
		return trailsGUI;
	}
	
	public void openSellGUI(SellGUI sellGUI) {
		this.sellGUI = sellGUI;
		getPlayer().openInventory(sellGUI.getInventory());
		setGUILocation(GUILocation.SELL_GUI);
	}
	
	public SellGUI getSellGUI() {
		return sellGUI;
	}
	
	public boolean isSpawningHorse() {
		return this.spawningHorse;
	}
	
	public void setSpawningHorse(boolean spawningHorse) {
		this.spawningHorse = spawningHorse;
	}
	
	public boolean isMountingHorse() {
		return mountingHorse;
	}
	
	public void setMountingHorse(boolean mountingHorse) {
		this.mountingHorse = mountingHorse;
	}
	
	public boolean isDeMountingHorse() {
		return deMountingHorse;
	}
	
	public void setDeMountingHorse(boolean deMountingHorse) {
		this.deMountingHorse = deMountingHorse;
	}
	
	public boolean isChangingHorse() {
		return changingHorse;
	}
	
	public void setChangingHorse(boolean changingHorse) {
		this.changingHorse = changingHorse;
	}
	
	public boolean hasReceivedDefaultHorse() {
		return receivedDefaultHorse;
	}
	
	public void setReceivedDefaultHorse(boolean receivedDefaultHorse) {
		this.receivedDefaultHorse = receivedDefaultHorse;
	}
	
	public Location getLastHorseLocation() {
		return lastHorseLocation;
	}
	
	public void setLastHorseLocation(Location lastHorseLocation) {
		this.lastHorseLocation = lastHorseLocation;
	}
	
	public GUILocation getGUILocation() {
		return guiLocation;
	}
	
	public void setGUILocation(GUILocation guiLocation) {
		this.guiLocation = guiLocation;
	}
	
	public boolean isInGUI(GUILocation guiLocation) {
		return guiLocation == this.guiLocation;
	}
	
	public boolean autoMountOn() {
		return autoMount;
	}
	
	public void setAutoMount(boolean autoMount) {
		this.autoMount = autoMount;
	}
}
