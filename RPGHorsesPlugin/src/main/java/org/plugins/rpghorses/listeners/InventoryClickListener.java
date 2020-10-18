package org.plugins.rpghorses.listeners;

import net.milkbowl.vault.economy.Economy;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUIItem;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.PriceGUIItem;
import org.plugins.rpghorses.guis.instances.*;
import org.plugins.rpghorses.horses.MarketHorse;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.MessageQueuer;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.managers.SQLManager;
import org.plugins.rpghorses.managers.gui.*;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.tiers.Tier;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import rorys.library.util.ItemUtil;
import rorys.library.util.MessagingUtil;
import rorys.library.util.SoundUtil;

import java.util.Map;

public class InventoryClickListener implements Listener {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final RPGHorseManager rpgHorseManager;
	private final StableGUIManager stableGUIManager;
	private final MarketGUIManager marketGUIManager;
	private final HorseGUIManager horseGUIManager;
	private final TrailGUIManager trailGUIManager;
	private final SellGUIManager sellGUIManager;
	private final SQLManager sqlManager;
	private final Economy economy;
	private final MessageQueuer messageQueuer;
	private final RPGMessagingUtil messagingUtil;
	
	public InventoryClickListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager, StableGUIManager stableGUIManager, MarketGUIManager marketGUIManager, HorseGUIManager horseGUIManager, TrailGUIManager trailGUIManager, SellGUIManager sellGUIManager, Economy economy, MessageQueuer messageQueuer, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.rpgHorseManager = rpgHorseManager;
		this.stableGUIManager = stableGUIManager;
		this.marketGUIManager = marketGUIManager;
		this.horseGUIManager = horseGUIManager;
		this.trailGUIManager = trailGUIManager;
		this.sellGUIManager = sellGUIManager;
		this.sqlManager = plugin.getSQLManager();
		this.economy = economy;
		this.messageQueuer = messageQueuer;
		this.messagingUtil = messagingUtil;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
		int slot = e.getSlot();
		
		if (horseOwner.getGUILocation() != GUILocation.NONE) {
			e.setCancelled(true);
		}
		
		if (horseOwner.isInGUI(GUILocation.STABLE_GUI)) {
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				StableGUIPage stableGUIPage = horseOwner.getCurrentStableGUIPage();
				if (stableGUIPage == null) {
					horseOwner.setGUILocation(GUILocation.NONE);
					return;
				}
				
				Inventory inv = p.getOpenInventory().getTopInventory();
				
				if (slot == inv.getSize() - 1) {
					horseOwner.openStableGUIPage(stableGUIPage.getPageNum() + 1);
				} else if (slot == inv.getSize() - 9) {
					horseOwner.openStableGUIPage(stableGUIPage.getPageNum() - 1);
				} else {
					RPGHorse rpgHorse = stableGUIPage.getRPGHorse(slot);
					if (rpgHorse != null) {
						if (e.isLeftClick()) {
							clickRPGHorse(p, horseOwner, rpgHorse);
						} else if (e.isRightClick()) {
							horseOwner.openHorseGUI(horseGUIManager.getHorseGUI(rpgHorse));
						}
					}
				}
			}
		} else if (horseOwner.isInGUI(GUILocation.HORSE_GUI)) {
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				HorseGUI horseGUI = horseOwner.getHorseGUI();
				RPGHorse rpgHorse = horseGUI.getRpgHorse();
				int horseNumber = horseOwner.getHorseNumber(rpgHorse);
				ItemPurpose itemPurpose = horseGUIManager.getItemPurpose(slot);
				if (itemPurpose == ItemPurpose.BACK) {
					horseOwner.openStableGUIPage(horseOwner.getCurrentStableGUIPage());
				} else if (itemPurpose == ItemPurpose.UPGRADE) {
					Tier tier = rpgHorseManager.getTier(rpgHorse.getTier());
					if (tier == null) {
						SoundUtil.playSound(p, plugin.getConfig(), "upgrade-options.success-sound");
						this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.max-tier-horse").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
					} else if (rpgHorse.getXp() >= tier.getExpCost()) {
						if (economy == null || tier.getCost() <= this.economy.getBalance(p)) {
							Map<ItemStack, Integer> itemsMissing = rpgHorseManager.getMissingItems(p, tier);
							if (itemsMissing.isEmpty()) {
								String tierStr = "" + (rpgHorse.getTier() + 1);
								if (this.rpgHorseManager.upgradeHorse(p, rpgHorse)) {
									this.stableGUIManager.updateRPGHorse(rpgHorse);
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.upgrade-horse-success").replace("{HORSE-NUMBER}", "" + horseNumber).replace("{TIER}", tierStr), rpgHorse);
									SoundUtil.playSound(p, plugin.getConfig(), "upgrade-options.success-sound");
									for (String cmd : this.plugin.getConfig().getStringList("command-options.on-upgrade-success")) {
										Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
									}
								} else {
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.upgrade-horse-failure").replace("{HORSE-NUMBER}", "" + horseNumber).replace("{TIER}", tierStr), rpgHorse);
									SoundUtil.playSound(p, plugin.getConfig(), "upgrade-options.failure-sound");
									for (String cmd : this.plugin.getConfig().getStringList("command-options.on-upgrade-fail")) {
										Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
									}
								}
							} else {
								SoundUtil.playSound(p, plugin.getConfig(), "upgrade-options.failure-sound");
								
								String items = "";
								int totalMissing = itemsMissing.size(), count = 0;
								for (ItemStack item : itemsMissing.keySet()) {
									int amount = itemsMissing.get(item);
									
									String name = "";
									if (item.getItemMeta().hasDisplayName()) {
										name = item.getItemMeta().getDisplayName();
									} else {
										name = "&6";
										String typeName = item.getType().name().toLowerCase().replace("_", "");
										for (String word : typeName.split("\\s")) {
											name += word.substring(0, 1).toUpperCase() + word.substring(1) + " ";
										}
										name = name.trim();
									}
									
									items += ChatColor.GRAY + "" + amount + "x " + name + ChatColor.GRAY;
									
									if (++count < totalMissing) {
										if (count == totalMissing - 1) {
											items += " and ";
										} else {
											items += ", ";
										}
									}
								}
								
								items = MessagingUtil.format(items);
								
								this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.missing-items-upgrade").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse, "ITEMS", items);
								p.closeInventory();
							}
						} else {
							SoundUtil.playSound(p, plugin.getConfig(), "upgrade-options.failure-sound");
							this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.cant-afford-upgrade").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
							p.closeInventory();
						}
					} else {
						SoundUtil.playSound(p, plugin.getConfig(), "upgrade-options.failure-sound");
						messagingUtil.sendMessageAtPath(p, "messages.not-enough-xp", rpgHorse);
						p.closeInventory();
					}
				} else if (itemPurpose == ItemPurpose.RENAME) {
					AnvilGUI.Builder builder = new AnvilGUI.Builder().plugin(plugin);
					builder.onClose(player -> new BukkitRunnable() {
						@Override
						public void run() {
							horseOwner.openHorseGUI(horseOwner.getHorseGUI());
						}
					}.runTaskLater(plugin, 1L)).onComplete((player, text) -> {
						String oldName = rpgHorse.getName(), name = text;
						if (!plugin.getConfig().getBoolean("horse-options.names.allow-spaces")) {
							name = name.replace(" ", "");
						}
						
						int length = ChatColor.stripColor(MessagingUtil.format(name)).length(), minLength = plugin.getConfig().getInt("horse-options.names.min-length"), maxLength = plugin.getConfig().getInt("horse-options.names.max-length");
						if (length < minLength) {
							messagingUtil.sendMessageAtPath(p, "messages.short-name", "HORSE-NAME", name, "MIN-LENGTH", "" + minLength, "MAX-LENGTH", "" + maxLength);
							p.closeInventory();
						} else if (length > maxLength) {
							messagingUtil.sendMessageAtPath(p, "messages.long-name", "HORSE-NAME", name, "MIN-LENGTH", "" + minLength, "MAX-LENGTH", "" + maxLength);
							p.closeInventory();
						} else {
							rpgHorse.setName(RPGMessagingUtil.format(name));
							this.stableGUIManager.updateRPGHorse(rpgHorse);
							this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-renamed").replace("{OLD-HORSE-NAME}", oldName), rpgHorse);
							horseOwner.openHorseGUI(horseGUIManager.getHorseGUI(rpgHorse));
						}
						return AnvilGUI.Response.close();
					});
					builder.title(RPGMessagingUtil.format("&6Type the new name")).item(horseGUI.getInventory().getItem(ItemUtil.getSlot(plugin.getConfig(), "horse-gui-options.horse-item")).clone()).text(rpgHorse.getName()).open(p);
				} else if (itemPurpose == ItemPurpose.TOGGLE_AUTOMOUNT_OFF || itemPurpose == ItemPurpose.TOGGLE_AUTOMOUNT_ON) {
					horseGUIManager.toggleAutoMount(horseGUI);
				} else if (itemPurpose == ItemPurpose.TRAILS) {
					horseOwner.openTrailsGUI(trailGUIManager.setupTrailsGUI(horseGUI));
				} else if (itemPurpose == ItemPurpose.SELL) {
					if (rpgHorse.isInMarket()) {
						this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-already-in-market").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
						return;
					} else if (rpgHorse.isDead() || horseOwner.getCurrentHorse() == rpgHorse) {
						this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-market-fail").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
						return;
					}
					horseOwner.openSellGUI(sellGUIManager.createSellGUI(horseGUI));
				} else if (itemPurpose == ItemPurpose.DELETE) {
					p.closeInventory();
					this.rpgHorseManager.addRemoveConfirmation(p, rpgHorse);
					this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.confirm-remove-horse").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
				} else if (slot == ItemUtil.getSlot(plugin.getConfig(), "horse-gui-options.horse-item")) {
					clickRPGHorse(p, horseOwner, rpgHorse);
				}
			}
		} else if (horseOwner.isInGUI(GUILocation.TRAILS_GUI)) {
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				TrailsGUI trailsGUI = horseOwner.getTrailsGUI();
				ItemPurpose itemPurpose = trailGUIManager.getItemPurpose(slot, trailsGUI);
				if (itemPurpose == ItemPurpose.TRAIL) {
					if (trailsGUI.applyTrail(slot)) {
						RPGHorse rpgHorse = trailsGUI.getRpgHorse();
						String trailName = trailsGUI.getTrailName(slot);
						this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.particle-set").replace("{HORSE-NUMBER}", "" + horseOwner.getHorseNumber(rpgHorse)).replace("{PARTICLE}", trailName.toUpperCase()), rpgHorse);
						horseOwner.openHorseGUI(horseOwner.getHorseGUI());
					}
				} else if (itemPurpose == ItemPurpose.BACK) {
					horseOwner.openHorseGUI(horseOwner.getHorseGUI());
				}
			}
		} else if (horseOwner.isInGUI(GUILocation.SELL_GUI)) {
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				SellGUI sellGUI = horseOwner.getSellGUI();
				GUIItem guiItem = sellGUIManager.getGUIItem(slot);
				if (guiItem != null) {
					ItemPurpose itemPurpose = guiItem.getItemPurpose();
					if (itemPurpose == ItemPurpose.CHANGE_PRICE) {
						PriceGUIItem priceGUIItem = (PriceGUIItem) guiItem;
						p.playSound(p.getLocation(), priceGUIItem.getSound(), priceGUIItem.getVolume(), priceGUIItem.getPitch());
						sellGUIManager.performUpdate(sellGUI, guiItem);
					} else if (itemPurpose == ItemPurpose.CONFIRM) {
						int price = sellGUI.getPrice();
						if (price <= 0) {
							SoundUtil.playSound(p, plugin.getConfig(), "sell-gui-options.failure-sound");
							return;
						}
						
						RPGHorse rpgHorse = sellGUI.getRpgHorse();
						int horseNumber = rpgHorse.getHorseOwner().getHorseNumber(rpgHorse);
						
						rpgHorse.setInMarket(true);
						MarketHorse marketHorse = this.marketGUIManager.addHorse(rpgHorse, price, horseNumber - 1);
						
						if (sqlManager != null) {
							sqlManager.addMarketHorse(marketHorse);
						}
						
						this.stableGUIManager.updateRPGHorse(rpgHorse);
						SoundUtil.playSound(p, plugin.getConfig(), "sell-gui-options.success-sound");
						this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-added-to-market").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
						p.closeInventory();
					} else if (itemPurpose == ItemPurpose.BACK) {
						horseOwner.openHorseGUI(horseOwner.getHorseGUI());
					}
				}
			}
		} else if (horseOwner.isInGUI(GUILocation.MARKET_GUI)) {
			
			e.setCancelled(true);
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				
				MarketGUIPage marketGUIPage = horseOwner.getCurrentMarketGUIPage();
				if (marketGUIPage == null) {
					horseOwner.setGUILocation(GUILocation.NONE);
					return;
				}
				
				if (slot == this.marketGUIManager.getNextPageSlot()) {
					horseOwner.openMarketGUIPage(this.marketGUIManager.getPage(marketGUIPage.getPageNum() + 1));
				} else if (slot == this.marketGUIManager.getPreviousPageSlot()) {
					horseOwner.openMarketGUIPage(this.marketGUIManager.getPage(marketGUIPage.getPageNum() - 1));
				} else if (slot == this.marketGUIManager.getYourHorsesSlot()) {
					
					if (horseOwner.getYourHorsesGUI() == null || horseOwner.getYourHorsesGUI().getPage(1) == null) {
						this.marketGUIManager.setupYourHorsesGUI(horseOwner);
					}
					horseOwner.openYourHorsesGUIPage(1);
				} else {
					MarketHorse marketHorse = marketGUIPage.getHorse(slot);
					if (marketHorse != null) {
						RPGHorse rpgHorse = marketHorse.getRPGHorse();
						if (rpgHorse.getHorseOwner().getUUID().equals(p.getUniqueId())) {
							this.messagingUtil.sendMessageAtPath(p.getPlayer(), "messages.market-buy-own-horse", rpgHorse);
						} else {
							
							if (this.horseOwnerManager.getHorseCount(p) >= this.horseOwnerManager.getHorseLimit(p)) {
								this.messagingUtil.sendMessageAtPath(p.getPlayer(), "messages.market-horse-limit", rpgHorse);
							} else {
								
								double price = marketHorse.getPrice();
								if (this.economy.getBalance(p) < price) {
									this.messagingUtil.sendMessageAtPath(p, "messages.cant-afford-market-horse", rpgHorse);
								} else {
									Player ownerP = rpgHorse.getHorseOwner().getPlayer();
									if (ownerP != null && ownerP.isOnline()) {
										rpgHorse = horseOwnerManager.getHorseOwner(ownerP).getRPGHorse(rpgHorse.getIndex());
									}
									
									this.economy.withdrawPlayer(p, price);
									rpgHorse.setInMarket(false);
									this.marketGUIManager.removeHorse(marketHorse, true);
									
									if (sqlManager != null) {
										sqlManager.removeMarketHorse(marketHorse, true);
									}
									
									String oldPlayerName = "null";
									if (rpgHorse.getHorseOwner().getPlayer() != null) {
										HorseOwner oldHorseOwner = rpgHorse.getHorseOwner();
										oldHorseOwner.removeRPGHorse(rpgHorse);
										Player oldOwner = Bukkit.getPlayer(oldHorseOwner.getUUID());
										if (oldOwner != null) {
											oldPlayerName = oldOwner.getName();
											this.stableGUIManager.setupStableGUI(oldHorseOwner);
											this.messagingUtil.sendMessage(oldOwner, this.plugin.getConfig().getString("messages.market-horse-sold").replace("{PRICE}", "" + price).replace("{PLAYER}", p.getName()), rpgHorse);
										} else {
											OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(oldHorseOwner.getUUID());
											if (offlinePlayer.hasPlayedBefore()) {
												oldPlayerName = offlinePlayer.getName();
												this.messageQueuer.queueMessage(offlinePlayer, this.messagingUtil.placeholders(this.plugin.getConfig().getString("messages.market-horse-sold").replace("{PRICE}", "" + price).replace("{PLAYER}", p.getName()), rpgHorse));
												this.horseOwnerManager.flushHorseOwner(oldHorseOwner);
											}
										}
									}
									horseOwner.addRPGHorse(rpgHorse);
									this.stableGUIManager.setupStableGUI(horseOwner);
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.market-horse-bought").replace("{PRICE}", "" + price).replace("{PLAYER}", oldPlayerName), rpgHorse);
								}
							}
						}
						p.closeInventory();
					}
				}
			}
		} else if (horseOwner.isInGUI(GUILocation.YOUR_HORSES_GUI)) {
			e.setCancelled(true);
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				if (slot == this.marketGUIManager.getBackSlot()) {
					horseOwner.openMarketGUIPage(this.marketGUIManager.getPage(1));
					return;
				}
				
				YourHorsesGUIPage yourHorsesGUIPage = horseOwner.getCurrentYourHorsesGUIPage();
				MarketHorse marketHorse = yourHorsesGUIPage.getMarketHorse(slot);
				if (marketHorse != null) {
					RPGHorse rpgHorse = marketHorse.getRPGHorse();
					this.marketGUIManager.removeHorse(marketHorse, false);
					rpgHorse.setInMarket(false);
					
					if (sqlManager != null) {
						sqlManager.removeMarketHorse(marketHorse, false);
					}
					
					this.marketGUIManager.setupYourHorsesGUI(horseOwner);
					this.stableGUIManager.updateRPGHorse(rpgHorse);
					int horseNumber = horseOwner.getHorseNumber(rpgHorse);
					this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.market-horse-removed").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
					p.closeInventory();
				}
				
			}
			
		} else if (horseOwner.getCurrentHorse() != null && e.getView().getTitle().equals(horseOwner.getCurrentHorse().getHorse().getName()) && e.getView().getTopInventory() == e.getClickedInventory() && e.getSlot() == 0) {
			e.setCancelled(true);
		}
	}
	
	public void clickRPGHorse(Player p, HorseOwner horseOwner, RPGHorse rpgHorse) {
		if (horseOwner.getCurrentHorse() == rpgHorse) {
			horseOwner.setCurrentHorse(null);
			for (String cmd : this.plugin.getConfig().getStringList("command-options.on-despawn")) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
			}
		} else if (rpgHorse.isDead()) {
			this.messagingUtil.sendMessageAtPath(p, "messages.horse-is-dead", rpgHorse);
		} else if (rpgHorse.isInMarket()) {
			this.messagingUtil.sendMessageAtPath(p, "messages.horse-is-in-market", rpgHorse);
		} else {
			if (horseOwner.getCurrentHorse() != null) {
				for (String cmd : this.plugin.getConfig().getStringList("command-options.on-despawn")) {
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
				}
			}
			
			if (!p.isOnGround()) {
				messagingUtil.sendMessageAtPath(p, "messages.not-on-ground");
				return;
			}
			
			horseOwner.setCurrentHorse(rpgHorse);
			for (String cmd : this.plugin.getConfig().getStringList("command-options.on-spawn")) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
			}
		}
		
		if (horseOwner.getGUILocation() == GUILocation.HORSE_GUI) {
			horseOwner.openHorseGUI(horseOwner.getHorseGUI());
		} else if (horseOwner.getGUILocation() == GUILocation.STABLE_GUI) {
			horseOwner.openStableGUIPage(horseOwner.getCurrentStableGUIPage());
		}
	}
	
}
