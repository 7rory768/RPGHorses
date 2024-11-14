package org.plugins.rpghorses.players;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.instances.*;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.SQLManager;
import org.plugins.rpghorses.tiers.Tier;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.utils.WorldGuardUtil;
import roryslibrary.util.MessagingUtil;
import roryslibrary.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class HorseOwner {

	GUILocation guiLocation = GUILocation.NONE;
	private UUID uuid;
	private List<RPGHorse> rpgHorses;
	private RPGHorse currentHorse;
	private boolean spawningHorse, receivedDefaultHorse, autoMount;
	private Location lastHorseLocation;
	private StableGUI stableGUI;
	private StableGUIPage stableGUIPage;
	private MarketGUIPage marketGUIPage;
	private YourHorsesGUI yourHorsesGUI;
	private YourHorsesGUIPage yourHorsesGUIPage;
	private HorseGUI horseGUI;
	private TrailsGUI trailsGUI;
	private TrailsGUIPage trailsGUIPage;
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

	public boolean setCurrentHorse(RPGHorse rpgHorse) {
		if (this.currentHorse != rpgHorse) {
			if (this.currentHorse != null) {
				if (rpgHorse != null) {
					LivingEntity horse = this.currentHorse.getHorse();
					if (horse != null) {
						getPlayer().setMetadata("RPGHorses-Ignore-Next-Teleport", new FixedMetadataValue(RPGHorsesMain.getInstance(), true));
					}
				}

				this.currentHorse.despawnEntity();
			}

			this.currentHorse = null;

			if (rpgHorse != null) {
				Player player = getPlayer();
				if (player == null) return false;

				if (!WorldGuardUtil.areRPGHorsesAllowed(player, player.getLocation()))
					return false;

				this.stableGUI.addGlow(rpgHorse);

				if (rpgHorse.spawnEntity()) {
					this.currentHorse = rpgHorse;
					return true;
				}

				return false;
			}
		}

		return true;
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

	public void openTrailsGUIPage(TrailsGUIPage trailsGUIPage) {
		if (trailsGUIPage != null) {
			getPlayer().openInventory(trailsGUIPage.getInventory());
			guiLocation = GUILocation.TRAILS_GUI;
			this.trailsGUIPage = trailsGUIPage;
		}
	}

	public void setTrailsGUI(TrailsGUI trailsGUI) {
		this.trailsGUI = trailsGUI;
		if (guiLocation == GUILocation.TRAILS_GUI) {
			int pageNum = this.trailsGUIPage.getPageNum();
			while (pageNum > 0) {
				TrailsGUIPage trailsGUIPage = trailsGUI.getPage(pageNum--);
				if (trailsGUIPage != null) {
					this.openTrailsGUIPage(trailsGUIPage);
					break;
				}
			}
		}
	}

	public void openSellGUI(SellGUI sellGUI) {
		this.sellGUI = sellGUI;
		getPlayer().openInventory(sellGUI.getInventory());
		setGUILocation(GUILocation.SELL_GUI);
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

	public boolean isRidingHorse() {
		if (currentHorse == null) return false;

		LivingEntity horse = currentHorse.getHorse();
		if (horse == null || !horse.isValid() || horse.isDead()) return false;

		Player p = getPlayer();
		if (p == null) return false;

		if (RPGHorsesMain.getVersion().getWeight() < 11) {
			return p.equals(horse.getPassenger());
		} else {
			return horse.getPassengers().contains(p);
		}
	}

	public void toggleRPGHorse(RPGHorse rpgHorse) {
		toggleRPGHorse(rpgHorse, true);
	}

	public void toggleRPGHorse(RPGHorse rpgHorse, boolean updateGUI) {
		RPGHorsesMain plugin = RPGHorsesMain.getInstance();
		RPGMessagingUtil messagingUtil = plugin.getMessagingUtil();

		Player p = getPlayer();
		if (p == null) return;

		if (getCurrentHorse() == rpgHorse) {
			setCurrentHorse(null);
			for (String cmd : plugin.getConfig().getStringList("command-options.on-despawn")) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
			}
		} else if (rpgHorse.isDead()) {
			String timeLeft = TimeUtil.formatTime((long) Math.ceil(plugin.getStableGuiManager().getDeathDifferent(rpgHorse) / 1000D));
			messagingUtil.sendMessageAtPath(p, "messages.horse-is-dead", rpgHorse, "TIME-LEFT", timeLeft);
		} else if (rpgHorse.isInMarket()) {
			messagingUtil.sendMessageAtPath(p, "messages.horse-is-in-market", rpgHorse);
		} else {
			if (getCurrentHorse() != null) {
				for (String cmd : plugin.getConfig().getStringList("command-options.on-despawn")) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
				}
			}

			RPGHorse currentHorse = getCurrentHorse();
			LivingEntity horse = currentHorse != null ? currentHorse.getHorse() : null;
			boolean isRidingHorse = isRidingHorse();

			boolean onGround = p.isOnGround() || (isRidingHorse && horse.isOnGround());

			if (!onGround) {
				messagingUtil.sendMessageAtPath(p, "messages.not-on-ground");
				return;
			}

			if (setCurrentHorse(rpgHorse)) {
				for (String cmd : plugin.getConfig().getStringList("command-options.on-spawn")) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
				}

				Tier nextTier = plugin.getRpgHorseManager().getNextTier(rpgHorse);

				if (nextTier != null && nextTier.getExpCost() <= rpgHorse.getXp())
					messagingUtil.sendMessageAtPath(p, "messages.max-xp", rpgHorse);

			} else {
				messagingUtil.sendMessageAtPath(p, "messages.cant-spawn-horse-here", rpgHorse);
			}
		}

		if (updateGUI) {
			if (getGUILocation() == GUILocation.HORSE_GUI) {
				openHorseGUI(getHorseGUI());
			} else if (getGUILocation() == GUILocation.STABLE_GUI) {
				openStableGUIPage(getCurrentStableGUIPage());
			}
		}
	}
}
