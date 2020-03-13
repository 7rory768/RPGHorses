package org.plugins.rpghorses.listeners;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.MarketGUIPage;
import org.plugins.rpghorses.guis.StableGUIPage;
import org.plugins.rpghorses.guis.UpgradeGUI;
import org.plugins.rpghorses.guis.YourHorsesGUIPage;
import org.plugins.rpghorses.horses.MarketHorse;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.*;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.MessagingUtil;

public class InventoryClickListener implements Listener {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final RPGHorseManager rpgHorseManager;
	private final StableGUIManager stableGuiManager;
	private final MarketGUIManager marketGUIManager;
	private final Economy economy;
	private final MessageQueuer messageQueuer;
	private final MessagingUtil messagingUtil;
	
	public InventoryClickListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager, MarketGUIManager marketGUIManager, Economy economy, MessageQueuer messageQueuer, MessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.rpgHorseManager = rpgHorseManager;
		this.stableGuiManager = stableGuiManager;
		this.marketGUIManager = marketGUIManager;
		this.economy = economy;
		this.messageQueuer = messageQueuer;
		this.messagingUtil = messagingUtil;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
		if (horseOwner.isInStableInventory()) {
			
			e.setCancelled(true);
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				int slot = e.getSlot();
				
				UpgradeGUI upgradeGUI = this.stableGuiManager.getUpgradeGUI(p);
				if (upgradeGUI != null) {
					if (slot == this.stableGuiManager.getUpgradeItemSlot()) {
						RPGHorse rpgHorse = upgradeGUI.getRPGHorse();
						int tier = rpgHorse.getTier() + 1;
						int horseNumber = horseOwner.getHorseNumber(rpgHorse);
						if (this.rpgHorseManager.getUpgradeCost(rpgHorse) <= this.economy.getBalance(p)) {
							if (rpgHorse.getXp() > rpgHorseManager.getXPNeededToUpgrade(rpgHorse)) {
								if (this.rpgHorseManager.upgradeHorse(p, rpgHorse)) {
									this.stableGuiManager.updateRPGHorse(rpgHorse);
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.upgrade-horse-success").replace("{HORSE-NUMBER}", "" + horseNumber).replace("{TIER}", "" + tier), rpgHorse);
									p.getLocation().getWorld().playSound(p.getLocation(), Sound.valueOf(this.plugin.getConfig().getString("upgrade-options.success-sound.sound")), Float.valueOf(this.plugin.getConfig().getString("upgrade-options.success-sound.volume")), Float.valueOf(this.plugin.getConfig().getString("upgrade-options.success-sound.pitch")));
									for (String cmd : this.plugin.getConfig().getStringList("command-options.on-upgrade-success")) {
										Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
									}
								} else {
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.upgrade-horse-failure").replace("{HORSE-NUMBER}", "" + horseNumber).replace("{TIER}", "" + tier), rpgHorse);
									p.getLocation().getWorld().playSound(p.getLocation(), Sound.valueOf(this.plugin.getConfig().getString("upgrade-options.failure-sound.sound")), Float.valueOf(this.plugin.getConfig().getString("upgrade-options.failure-sound.volume")), Float.valueOf(this.plugin.getConfig().getString("upgrade-options.failure-sound.pitch")));
									for (String cmd : this.plugin.getConfig().getStringList("command-options.on-upgrade-fail")) {
										Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
									}
								}
							} else {
								messagingUtil.sendMessageAtPath(p, "messages.not-enough-xp", rpgHorse);
							}
						} else {
							this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.cant-afford-upgrade").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
						}
						p.closeInventory();
					}
					return;
				}
				
				StableGUIPage stableGUIPage = horseOwner.getCurrentStableGUIPage();
				if (stableGUIPage == null) {
					horseOwner.setInStableInventory(false);
					return;
				}
				
				if (slot == this.stableGuiManager.getNextPageSlot()) {
					horseOwner.openStableGUIPage(stableGUIPage.getPageNum() + 1);
				} else if (slot == this.stableGuiManager.getPreviousPageSlot()) {
					horseOwner.openStableGUIPage(stableGUIPage.getPageNum() - 1);
				} else {
					RPGHorse rpgHorse = stableGUIPage.getRPGHorse(slot);
					if (rpgHorse != null) {
						int horseNumber = horseOwner.getHorseNumber(rpgHorse);
						if (e.isShiftClick()) {
							if (e.isLeftClick()) {
								this.rpgHorseManager.addHorseRenamer(p, rpgHorse);
								this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.rename-horse").replace("{HORSE-NUMBER}", "" + horseNumber));
							} else if (e.isRightClick()) {
								if (rpgHorse.getTier() == this.rpgHorseManager.getMaxTier()) {
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.max-tier-horse").replace("{HORSE-NUMBER}", "" + horseNumber));
								} else if (this.rpgHorseManager.getUpgradeCost(rpgHorse) <= this.economy.getBalance(p)) {
								    if (rpgHorse.getXp() > rpgHorseManager.getXPNeededToUpgrade(rpgHorse)) {
									this.stableGuiManager.openUpgradeGUI(p, rpgHorse);
									return;
									} else {
                                        messagingUtil.sendMessageAtPath(p, "messages.not-enough-xp", rpgHorse);
                                    }
								} else {
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.cant-afford-upgrade").replace("{HORSE-NUMBER}", "" + horseNumber));
								}
							}
						} else {
							if (e.isLeftClick()) {
								if (horseOwner.getCurrentHorse() == rpgHorse) {
									horseOwner.setCurrentHorse(null);
									for (String cmd : this.plugin.getConfig().getStringList("command-options.on-despawn")) {
										Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
									}
								} else if (rpgHorse.isDead()) {
									this.messagingUtil.sendMessageAtPath(p, "messages.horse-is-dead");
								} else if (rpgHorse.isInMarket()) {
									this.messagingUtil.sendMessageAtPath(p, "messages.horse-is-in-market");
								} else {
									if (horseOwner.getCurrentHorse() != null) {
										for (String cmd : this.plugin.getConfig().getStringList("command-options.on-despawn")) {
											Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
										}
									}
									horseOwner.setCurrentHorse(rpgHorse);
									for (String cmd : this.plugin.getConfig().getStringList("command-options.on-spawn")) {
										Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
									}
								}
								p.closeInventory();
							} else if (e.isRightClick()) {
								this.rpgHorseManager.addRemoveConfirmation(p, rpgHorse);
								this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.confirm-remove-horse").replace("{HORSE-NUMBER}", "" + horseNumber));
							}
						}
						p.closeInventory();
					}
				}
			}
			
		} else if (horseOwner.isInMarketInventory()) {
			
			e.setCancelled(true);
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				int slot = e.getSlot();
				
				MarketGUIPage marketGUIPage = horseOwner.getCurrentMarketGUIPage();
				if (marketGUIPage == null) {
					horseOwner.setInMarketInventory(false);
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
					RPGHorse rpgHorse = marketGUIPage.getRPGHorse(slot);
					if (rpgHorse != null) {
						
						if (rpgHorse.getHorseOwner().getUUID().equals(p.getUniqueId())) {
							this.messagingUtil.sendMessageAtPath(p.getPlayer(), "messages.market-buy-own-horse", rpgHorse);
						} else {
							
							if (this.horseOwnerManager.getHorseCount(p) >= this.horseOwnerManager.getHorseLimit(p)) {
								this.messagingUtil.sendMessageAtPath(p.getPlayer(), "messages.market-horse-limit", rpgHorse);
							} else {
								
								double price = this.marketGUIManager.getPrice(marketGUIPage, rpgHorse);
								if (this.economy.getBalance(p) < price) {
									this.messagingUtil.sendMessageAtPath(p, "messages.cant-afford-market-horse", rpgHorse);
								} else {
									this.economy.withdrawPlayer(p, price);
									rpgHorse.setInMarket(false);
									this.marketGUIManager.removeHorse(marketGUIPage, rpgHorse, true);
									String oldPlayerName = "null";
									if (rpgHorse.getHorseOwner() != null) {
										HorseOwner oldHorseOwner = rpgHorse.getHorseOwner();
										oldHorseOwner.removeRPGHorse(rpgHorse);
										Player oldOwner = Bukkit.getPlayer(oldHorseOwner.getUUID());
										if (oldOwner != null) {
											oldPlayerName = oldOwner.getName();
											this.stableGuiManager.setupStableGUI(oldHorseOwner);
											this.messagingUtil.sendMessage(oldOwner, this.plugin.getConfig().getString("messages.market-horse-sold").replace("{PRICE}", "" + price).replace("{PLAYER}", p.getName()), rpgHorse);
										} else {
											OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(oldHorseOwner.getUUID());
											oldPlayerName = offlinePlayer.getName();
											this.messageQueuer.queueMessage(offlinePlayer, this.messagingUtil.placeholders(this.plugin.getConfig().getString("messages.market-horse-sold").replace("{PRICE}", "" + price).replace("{PLAYER}", p.getName()), rpgHorse));
											this.horseOwnerManager.flushHorseOwner(oldHorseOwner);
										}
									}
									horseOwner.addRPGHorse(rpgHorse);
									this.stableGuiManager.setupStableGUI(horseOwner);
									this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.market-horse-bought").replace("{PRICE}", "" + price).replace("{PLAYER}", oldPlayerName), rpgHorse);
								}
							}
						}
						p.closeInventory();
					}
				}
			}
		} else if (horseOwner.isInYourHorsesInventory()) {
			e.setCancelled(true);
			if (e.getClickedInventory() == p.getOpenInventory().getTopInventory()) {
				int slot = e.getSlot();
				
				if (slot == this.marketGUIManager.getBackSlot()) {
					horseOwner.openMarketGUIPage(this.marketGUIManager.getPage(1));
					return;
				}
				
				YourHorsesGUIPage yourHorsesGUIPage = horseOwner.getCurrentYourHorsesGUIPage();
				MarketHorse marketHorse = yourHorsesGUIPage.getMarketHorse(slot);
				if (marketHorse != null) {
					RPGHorse rpgHorse = marketHorse.getRPGHorse();
					this.marketGUIManager.removeHorse(this.marketGUIManager.getPage(marketHorse.getRPGHorse()), rpgHorse, false);
					rpgHorse.setInMarket(false);
					this.marketGUIManager.setupYourHorsesGUI(horseOwner);
					this.stableGuiManager.updateRPGHorse(rpgHorse);
					int horseNumber = horseOwner.getHorseNumber(rpgHorse);
					this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.market-horse-removed").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
					p.closeInventory();
				}
				
			}
			
		}
	}
	
}
