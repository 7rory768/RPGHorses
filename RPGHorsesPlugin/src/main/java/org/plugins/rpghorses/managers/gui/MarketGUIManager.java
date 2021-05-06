package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.instances.MarketGUIPage;
import org.plugins.rpghorses.guis.instances.YourHorsesGUI;
import org.plugins.rpghorses.guis.instances.YourHorsesGUIPage;
import org.plugins.rpghorses.horses.MarketHorse;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.SQLManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.SkinValueUtil;
import roryslibrary.configs.CustomConfig;
import roryslibrary.util.DebugUtil;
import roryslibrary.util.ItemUtil;
import roryslibrary.util.MessagingUtil;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class MarketGUIManager {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final SQLManager sqlManager;
	private final CustomConfig marketConfig;
	private final ItemUtil itemUtil;
	
	private ItemStack marketFillItem, horseItem, yourHorsesItem, backItem, previousPageItem, nextPageItem;
	private int marketRows, yourHorsesSlot, backSlot, previousPageSlot, nextPageSlot;
	private List<MarketGUIPage> marketGUIPages = new ArrayList<>();
	private List<MarketHorse> marketHorses = new ArrayList<>();
	
	public MarketGUIManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, CustomConfig marketConfig, ItemUtil itemUtil) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.sqlManager = plugin.getSQLManager();
		this.marketConfig = marketConfig;
		this.itemUtil = itemUtil;
		
		this.loadMarketHorses();
		this.reload();
	}
	
	public int getPreviousPageSlot() {
		return previousPageSlot;
	}
	
	public int getNextPageSlot() {
		return nextPageSlot;
	}
	
	public int getYourHorsesSlot() {
		return this.yourHorsesSlot;
	}
	
	public int getBackSlot() {
		return this.backSlot;
	}
	
	public ItemStack getHorseItem(RPGHorse rpgHorse) {
		ItemStack item = horseItem.clone();
		return SkinValueUtil.applySkin(rpgHorse, item);
	}
	
	public void setupMarketGUI() {
		try {
		int rpgHorsesAdded = -1;
		String title = MessagingUtil.format(this.plugin.getConfig().getString("market-options.title"));
		Inventory gui = Bukkit.createInventory(null, this.marketRows * 9, title);
		int slot = 10, pageNum = 1;
		List<MarketGUIPage> marketGUIPages = new ArrayList<>();
		HashMap<Integer, MarketHorse> horseSlots = new HashMap<>();
		
		for (MarketHorse marketHorse : this.marketHorses) {
			rpgHorsesAdded++;
			RPGHorse rpgHorse = marketHorse.getRPGHorse();
			ItemStack item = getHorseItem(rpgHorse);
			item = fillPlaceholders(item, marketHorse);
			gui.setItem(slot, item);
			horseSlots.put(slot, marketHorse);
			if (slot++ == (this.marketRows - 1) * 9 - 2 || rpgHorsesAdded == this.marketHorses.size() - 1) {
				for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
					if (fillSlot == this.nextPageSlot && rpgHorsesAdded < this.marketHorses.size() - 1) {
						gui.setItem(fillSlot, this.nextPageItem);
					} else if (fillSlot == this.previousPageSlot && pageNum > 1) {
						gui.setItem(fillSlot, this.previousPageItem);
					} else if (fillSlot == this.yourHorsesSlot) {
						gui.setItem(fillSlot, this.yourHorsesItem);
					} else if (gui.getItem(fillSlot) == null) {
						gui.setItem(fillSlot, this.marketFillItem);
					}
				}
				marketGUIPages.add(new MarketGUIPage(pageNum, gui, horseSlots));
				slot = 10;
				horseSlots = new HashMap<>();
				gui = Bukkit.createInventory(null, this.marketRows * 9, title);
			}
		}
		
		if (rpgHorsesAdded == -1) {
			for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
				if (fillSlot == this.nextPageSlot && rpgHorsesAdded < this.marketHorses.size() - 1) {
					gui.setItem(fillSlot, this.nextPageItem);
				} else if (fillSlot == this.previousPageSlot && pageNum > 1) {
					gui.setItem(fillSlot, this.previousPageItem);
				} else if (fillSlot == this.yourHorsesSlot) {
					gui.setItem(fillSlot, this.yourHorsesItem);
				} else if (gui.getItem(fillSlot) == null) {
					gui.setItem(fillSlot, this.marketFillItem);
				}
			}
			marketGUIPages.add(new MarketGUIPage(pageNum++, gui, horseSlots));
		}
		this.marketGUIPages = marketGUIPages;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void reloadMarketGUI() {
		this.setupMarketGUI();
		
		mainloop:
		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners().values()) {
			if (horseOwner.isInGUI(GUILocation.MARKET_GUI)) {
				MarketGUIPage marketGUIPage = horseOwner.getCurrentMarketGUIPage();
				int pageNum = marketGUIPage.getPageNum();
				while (pageNum > 0) {
					MarketGUIPage newMarketGUIPage = this.getPage(pageNum--);
					if (newMarketGUIPage != null) {
						horseOwner.openMarketGUIPage(newMarketGUIPage);
						continue mainloop;
					}
				}
				if (horseOwner.getPlayer() != null) horseOwner.getPlayer().closeInventory();
			}
		}
	}
	
	public int getNextID() {
		return marketHorses.size();
	}
	
	public void loadMarketHorses() {
		if (sqlManager != null) {
			marketHorses = sqlManager.loadMarket();
		} else {
			this.marketHorses.clear();
			
			FileConfiguration config = this.marketConfig.getConfig();
			Set<String> ids = config.getConfigurationSection("market").getKeys(false);
			
			for (String id : ids) {
				RPGHorse rpgHorse = this.getRPGHorse(id);
				double price = config.getDouble("market." + id + ".price");
				this.marketHorses.add(new MarketHorse(Integer.valueOf(id), rpgHorse, price, rpgHorse.getHorseOwner().getHorseNumber(rpgHorse) - 1));
			}
		}
	}
	
	public void saveMarketHorses() {
		FileConfiguration config = marketConfig.getConfig();
		
		config.set("market", null);
		config.createSection("market");
		
		for (MarketHorse marketHorse : marketHorses) {
			String path = "market." + marketHorse.getId() + ".";
			config.set(path + "horse-owner", marketHorse.getRPGHorse().getHorseOwner().getUUID().toString());
			config.set(path + "price", marketHorse.getPrice());
			config.set(path + "index", marketHorse.getIndex());
		}
		
		this.marketConfig.saveConfig();
		this.marketConfig.reloadConfig();
	}
	
	public MarketHorse getMarketHorse(RPGHorse rpgHorse) {
		String owner = rpgHorse.getHorseOwner().getUUID().toString();
		int index = rpgHorse.getIndex();
		DebugUtil.debug("searching for " + index + ": " + owner);
		
		for (MarketHorse marketHorse : marketHorses) {
			DebugUtil.debug("checking " + marketHorse.getIndex() + ": " + marketHorse.getRPGHorse().getHorseOwner().getUUID().toString());
			if (marketHorse.getIndex() == index && marketHorse.getRPGHorse().getHorseOwner().getUUID().toString().equals(owner))
				return marketHorse;
		}
		
		return null;
	}
	
	public void registerDelete(RPGHorse rpgHorse) {
		UUID uuid = rpgHorse.getHorseOwner().getUUID();
        int index = rpgHorse.getIndex();
        
        for (MarketHorse marketHorse : marketHorses) {
        	if (marketHorse.getRPGHorse().getHorseOwner().getUUID().equals(uuid) && marketHorse.getIndex() > index) {
		        marketHorse.setIndex(marketHorse.getIndex() - 1);
	        }
        }
	}
	
	public MarketHorse addHorse(RPGHorse rpgHorse, double price, int index) {
		MarketHorse marketHorse = new MarketHorse(marketHorses.size(), rpgHorse, price, index);
		marketHorses.add(marketHorse);
		this.reloadMarketGUI();
		
		if (rpgHorse.getHorseOwner().getPlayer() != null && rpgHorse.getHorseOwner().getPlayer().isOnline()) {
			this.setupYourHorsesGUI(rpgHorse.getHorseOwner());
		}
		return marketHorse;
	}
	
	public void removeHorse(MarketHorse marketHorseToRemove, boolean horseRemoved) {
		try {
			RPGHorse rpgHorse = marketHorseToRemove.getRPGHorse();
			int id = marketHorseToRemove.getId();
			HorseOwner horseOwner = rpgHorse.getHorseOwner();
			int index = marketHorseToRemove.getIndex();
			UUID uuid = horseOwner.getUUID();
			
			marketHorses.remove(marketHorseToRemove);
			
			for (MarketHorse marketHorse : marketHorses) {
				if (marketHorse.getId() > id) {
					marketHorse.setId(marketHorse.getId() - 1);
				}
				
				if (horseRemoved && uuid.equals(marketHorse.getRPGHorse().getHorseOwner().getUUID()) && marketHorse.getIndex() > index) {
					marketHorse.setIndex(marketHorse.getIndex() - 1);
				}
			}
			
			this.reloadMarketGUI();
			if (rpgHorse.getHorseOwner().getPlayer() != null && rpgHorse.getHorseOwner().getPlayer().isOnline()) {
				this.setupYourHorsesGUI(rpgHorse.getHorseOwner());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public MarketGUIPage getPage(int page) {
		if (page >= 1 && this.marketGUIPages.size() >= page) {
			return this.marketGUIPages.get(page - 1);
		}
		return null;
	}
	
	public RPGHorse getRPGHorse(String id) {
		String path = "market." + id + ".";
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(Bukkit.getOfflinePlayer(UUID.fromString(this.marketConfig.getConfig().getString(path + "horse-owner"))));
		RPGHorse rpgHorse = horseOwner.getRPGHorse(this.marketConfig.getConfig().getInt(path + ".index"));
		if (Bukkit.getPlayer(horseOwner.getUUID()) == null) {
			this.horseOwnerManager.flushHorseOwner(horseOwner);
		}
		return rpgHorse;
	}
	
	public ItemStack fillPlaceholders(ItemStack item, MarketHorse marketHorse) {
		if (item != null && marketHorse != null) {
			ItemMeta itemMeta = item.getItemMeta();
			
			RPGHorse rpgHorse = marketHorse.getRPGHorse();
			
			NumberFormat formatter = new DecimalFormat("#0.0##");
			String name = MessagingUtil.format(rpgHorse.getName()), tier = "" + rpgHorse.getTier(), movementSpeed = formatter.format(rpgHorse.getMovementSpeed()), jumpStrength = formatter.format(rpgHorse.getJumpStrength()), health = formatter.format(rpgHorse.getHealth()), maxHealth = formatter.format(rpgHorse.getMaxHealth()), price = formatter.format(marketHorse.getPrice());
			
			String ownerName = "null";
			UUID uuid = rpgHorse.getHorseOwner().getUUID();
			OfflinePlayer p = Bukkit.getPlayer(uuid);
			if (p != null && p.isOnline()) {
				ownerName = p.getName();
			} else {
				p = Bukkit.getOfflinePlayer(uuid);
				if (p != null && p.hasPlayedBefore()) {
					ownerName = p.getName();
				}
			}
			
			if (itemMeta.hasDisplayName()) {
				itemMeta.setDisplayName(itemMeta.getDisplayName().replace("{HEALTH}", health).replace("{MAX-HEALTH}", maxHealth).replace("{TIER}", tier).replace("{HORSE-NAME}", name).replace("{MOVEMENT-SPEED}", movementSpeed).replace("{JUMP-STRENGTH}", jumpStrength).replace("{PRICE}", "" + price).replace("{HORSE-OWNER}", ownerName));
			}
			
			if (itemMeta.hasLore()) {
				List<String> newLore = new ArrayList<>();
				for (String line : itemMeta.getLore()) {
					newLore.add(line.replace("{HEALTH}", health).replace("{MAX-HEALTH}", maxHealth).replace("{TIER}", tier).replace("{HORSE-NAME}", name).replace("{MOVEMENT-SPEED}", movementSpeed).replace("{JUMP-STRENGTH}", jumpStrength).replace("{PRICE}", "" + price).replace("{HORSE-OWNER}", ownerName));
				}
				itemMeta.setLore(newLore);
			}
			
			item.setItemMeta(itemMeta);
		}
		return item;
	}
	
	public void setupYourHorsesGUIS() {
		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners().values()) {
			this.setupYourHorsesGUI(horseOwner);
		}
	}
	
	public void setupYourHorsesGUI(HorseOwner horseOwner) {
		Player p = horseOwner.getPlayer();
		
		if (p != null && p.isOnline()) {
			List<RPGHorse> rpgHorses = horseOwner.getRPGHorses();
			int horseCount = -1, slot = 10, pageNum = 1;
			String title = MessagingUtil.format(this.plugin.getConfig().getString("market-options.title"));
			Inventory gui = Bukkit.createInventory(null, this.marketRows * 9, title);
			List<YourHorsesGUIPage> yourHorsesGUIPages = new ArrayList<>();
			HashMap<Integer, MarketHorse> horseSlots = new HashMap<>();
			
			for (RPGHorse rpgHorse : rpgHorses) {
				horseCount++;
				if (rpgHorse.isInMarket()) {
					MarketHorse marketHorse = this.getMarketHorse(rpgHorse);
					ItemStack item = getHorseItem(rpgHorse);
					item = fillPlaceholders(item, marketHorse);
					gui.setItem(slot, item);
					horseSlots.put(slot, marketHorse);
				}
				if (slot++ == (this.marketRows - 1) * 9 - 2 || horseCount == rpgHorses.size() - 1) {
					for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
						if (fillSlot == (this.marketRows * 9) - 1 && horseCount < rpgHorses.size() - 1) {
							gui.setItem(fillSlot, this.nextPageItem);
						} else if (fillSlot == (this.marketRows * 9) - 9 && pageNum > 1) {
							gui.setItem(fillSlot, this.previousPageItem);
						} else if (fillSlot == this.getBackSlot()) {
							gui.setItem(fillSlot, this.backItem);
						} else if (gui.getItem(fillSlot) == null) {
							gui.setItem(fillSlot, this.marketFillItem);
						}
					}
					yourHorsesGUIPages.add(new YourHorsesGUIPage(pageNum++, gui, horseSlots));
					slot = 10;
					horseSlots = new HashMap<>();
					gui = Bukkit.createInventory(null, this.marketRows * 9, title);
				}
			}
			
			if (yourHorsesGUIPages.size() == 0) {
				for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
					if (fillSlot == this.getBackSlot()) {
						gui.setItem(fillSlot, this.backItem);
					} else if (gui.getItem(fillSlot) == null) {
						gui.setItem(fillSlot, this.marketFillItem);
					}
				}
				yourHorsesGUIPages.add(new YourHorsesGUIPage(pageNum++, gui, horseSlots));
			}
			
			YourHorsesGUI yourHorsesGUI = new YourHorsesGUI(horseOwner, yourHorsesGUIPages);
			horseOwner.setYourHorsesGUI(yourHorsesGUI);
		}
	}
	
	public boolean reload() {
		boolean reloadItems = false;
		reloadItems = this.reloadPreviousPageItem() || reloadItems;
		reloadItems = this.reloadNextPageItem() || reloadItems;
		reloadItems = this.reloadMarketFillItem() || reloadItems;
		reloadItems = this.reloadHorseItem() || reloadItems;
		reloadItems = this.reloadYourHorsesItem() || reloadItems;
		reloadItems = this.reloadBackItem() || reloadItems;
		reloadItems = this.reloadMarketNumbers() || reloadItems;
		
		if (reloadItems) {
			this.setupMarketGUI();
			this.setupYourHorsesGUIS();
		}
		
		return reloadItems;
	}
	
	private boolean reloadMarketFillItem() {
		ItemStack oldMarketFillItem = this.marketFillItem;
		this.marketFillItem = this.itemUtil.getItemStack("market-options.fill-item");
		return ItemUtil.isSimilar(oldMarketFillItem, marketFillItem);
	}
	
	private boolean reloadHorseItem() {
		ItemStack oldHorseItem = this.horseItem;
		this.horseItem = this.itemUtil.getItemStack("market-options.horse-item");
		return ItemUtil.isSimilar(oldHorseItem, horseItem);
	}
	
	private boolean reloadYourHorsesItem() {
		ItemStack oldYourHorsesItem = this.yourHorsesItem;
		this.yourHorsesItem = this.itemUtil.getItemStack("market-options.your-horses-item");
		return ItemUtil.isSimilar(oldYourHorsesItem, yourHorsesItem);
	}
	
	private boolean reloadBackItem() {
		ItemStack oldBackItem = this.backItem;
		this.backItem = this.itemUtil.getItemStack("market-options.back-item");
		return ItemUtil.isSimilar(oldBackItem, backItem);
	}
	
	private boolean reloadPreviousPageItem() {
		ItemStack oldPreviousPageItem = this.previousPageItem;
		this.previousPageItem = this.itemUtil.getItemStack("market-options.previous-page-item");
		return ItemUtil.isSimilar(oldPreviousPageItem, previousPageItem);
	}
	
	private boolean reloadNextPageItem() {
		ItemStack oldNextPageItem = this.nextPageItem;
		this.nextPageItem = this.itemUtil.getItemStack("market-options.next-page-item");
		return ItemUtil.isSimilar(oldNextPageItem, nextPageItem);
	}
	
	private boolean reloadMarketNumbers() {
		int oldMarketRows = this.marketRows;
		this.marketRows = this.plugin.getConfig().getInt("market-options.rows");
		
		int oldYourHorsesSlot = this.yourHorsesSlot;
		this.yourHorsesSlot = this.itemUtil.getSlot("market-options.your-horses-item");
		
		int oldBackSlot = this.backSlot;
		this.backSlot = this.itemUtil.getSlot("market-options.back-item");
		
		this.nextPageSlot = this.marketRows * 9 - 1;
		this.previousPageSlot = this.nextPageSlot - 8;
		
		return this.marketRows != oldMarketRows || this.yourHorsesSlot != oldYourHorsesSlot || this.backSlot != oldBackSlot;
	}
}
