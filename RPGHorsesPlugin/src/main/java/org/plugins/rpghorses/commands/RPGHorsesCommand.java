package org.plugins.rpghorses.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.crates.HorseCrate;
import org.plugins.rpghorses.events.RPGHorseClaimEvent;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.MarketHorse;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.*;
import org.plugins.rpghorses.managers.gui.MarketGUIManager;
import org.plugins.rpghorses.managers.gui.StableGUIManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.utils.ItemUtil;
import roryslibrary.util.MessagingUtil;
import roryslibrary.util.NumberUtil;

import java.util.Map;

public class RPGHorsesCommand implements CommandExecutor {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final RPGHorseManager rpgHorseManager;
	private final StableGUIManager stableGUIManager;
	private final MarketGUIManager marketGUIManager;
	private final HorseCrateManager horseCrateManager;
	private final ParticleManager particleManager;
	private final SQLManager sqlManager;
	private final RPGMessagingUtil messagingUtil;
	
	public RPGHorsesCommand(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager, StableGUIManager stableGUIManager, MarketGUIManager marketGUIManager, HorseCrateManager horseCrateManager, ParticleManager particleManager, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.rpgHorseManager = rpgHorseManager;
		this.stableGUIManager = stableGUIManager;
		this.marketGUIManager = marketGUIManager;
		this.horseCrateManager = horseCrateManager;
		this.particleManager = particleManager;
		this.sqlManager = plugin.getSQLManager();
		this.messagingUtil = messagingUtil;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (args.length > 0) {
			
			String arg1 = args[0];
			
			if (arg1.equalsIgnoreCase("claim")) {
				if (!sender.hasPermission("rpghorses.claim")) {
					messagingUtil.sendNoPermissionMessage(sender);
					return false;
				}
				
				if (!(sender instanceof Player)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Player p = (Player) sender;
				
				if (!plugin.getConfig().getBoolean("horse-options.allow-claiming", false)) {
					messagingUtil.sendMessageAtPath(sender, "messages.claiming-disabled");
					return false;
				}
				
				Entity entity = p.getVehicle();
				
				if (!(entity instanceof LivingEntity) || !rpgHorseManager.isValidEntityType(entity.getType())) {
					messagingUtil.sendMessageAtPath(p, "messages.claim-fail");
					return false;
				}

				ItemStack saddleItem = ((InventoryHolder) entity).getInventory().getItem(0);

				if (plugin.getConfig().getBoolean("horse-options.no-claiming-without-saddle", false) && (saddleItem == null || saddleItem.getType() != Material.SADDLE)) {
					messagingUtil.sendMessageAtPath(sender, "messages.claim-saddle-fail");
					return false;
				}
				
				HorseOwner horseOwner = horseOwnerManager.getHorseOwner(p);
				RPGHorse rpgHorse = rpgHorseManager.getRPGHorse(entity);
				if (rpgHorse != null) {
					messagingUtil.sendMessageAtPath(p, "messages.already-claimed", rpgHorse, "HORSE-OWNER", rpgHorse.getHorseOwner().getPlayerName());
					return false;
				}
				
				if (!plugin.getConfig().getBoolean("horse-options.horse-jacking") && entity instanceof Tameable) {
					Tameable tameable = (Tameable) entity;
					if (tameable.isTamed() && tameable.getOwner() != null && !tameable.getOwner().getUniqueId().equals(p.getUniqueId())) {
						messagingUtil.sendMessageAtPath(p, "messages.claim-jacking", "PLAYER", tameable.getOwner().getName());
						return false;
					}
				}
				
				int horseCount = this.horseOwnerManager.getHorseCount(p);
				if (horseCount >= this.horseOwnerManager.getHorseLimit(p)) {
					this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.claim-limit").replace("{HORSE-LIMIT}", "" + horseOwnerManager.getHorseLimit(p)));
					return false;
				}
				
				RPGHorseClaimEvent event = new RPGHorseClaimEvent(p, entity);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					rpgHorse = new RPGHorse(horseOwner, (LivingEntity) entity, plugin.getConfig().getString("horse-options.default-name", "Horse").replace("{PLAYER}", horseOwner.getPlayerName()));
					horseOwner.addRPGHorse(rpgHorse);
					
					this.stableGUIManager.setupStableGUI(horseOwner);
					
					entity.remove();
					horseOwner.setCurrentHorse(rpgHorse);
					
					messagingUtil.sendMessageAtPath(p, "messages.horse-claim", "ENTITY-TYPE", entity.getType().name().replace("_", " ").toLowerCase());
				}
				
				return true;
			}
			
			if (arg1.equalsIgnoreCase("sell")) {
				if (!sender.hasPermission("rpghorses.sell")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}
				
				if (!(sender instanceof Player)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Player p = (Player) sender;
				
				if (args.length < 3) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label + " sell <horse-number> <price>");
					return false;
				}
				
				String horseNumberArg = args[1];
				if (!this.runHorseNumberCheck(sender, horseNumberArg, p)) {
					return false;
				}
				int horseNumber = Integer.parseInt(horseNumberArg), index = horseNumber - 1;
				
				String priceArg = args[2];
				if (!NumberUtil.isPositiveDouble(priceArg)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid price argument &6" + priceArg);
					return false;
				}
				double price = Double.parseDouble(priceArg);
				
				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
				RPGHorse rpgHorse = horseOwner.getRPGHorse(index);
				if (rpgHorse.isInMarket()) {
					this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-already-in-market").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
					return false;
				} else if (rpgHorse.isDead() || horseOwner.getCurrentHorse() == rpgHorse) {
					this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-market-fail").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
					return false;
				}
				
				rpgHorse.setInMarket(true);
				MarketHorse marketHorse = this.marketGUIManager.addHorse(rpgHorse, price, horseNumber - 1);
				
				if (sqlManager != null && sqlManager.isEnabled()) {
					sqlManager.addMarketHorse(marketHorse);
				}
				
				this.stableGUIManager.updateRPGHorse(rpgHorse);
				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-added-to-market").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
				return true;
			}
			
			if (arg1.equalsIgnoreCase("buy")) {
				if (!sender.hasPermission("rpghorses.buy")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}
				
				if (args.length < 2) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label + " buy <horse-crate>");
					return false;
				}
				
				if (!(sender instanceof Player)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Player p = (Player) sender;
				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
				
				int horseCount = this.horseOwnerManager.getHorseCount(p);
				if (horseCount >= this.horseOwnerManager.getHorseLimit(p)) {
					this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-limit").replace("{PLAYER}", "CONSOLE"));
					return false;
				}
				
				HorseCrate horseCrate = this.horseCrateManager.getHorseCrate(args[1]);
				if (horseCrate == null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid crate name: &6" + args[1]);
					this.messagingUtil.sendMessage(sender, "{PREFIX}Valid crate names: &6" + this.horseCrateManager.getHorseCrateList());
					return false;
				}
				
				if (plugin.getEconomy() != null && plugin.getEconomy().getBalance(p) < horseCrate.getPrice()) {
					this.messagingUtil.sendMessageAtPath(p, "messages.cant-afford-crate");
					return false;
				}
				
				if (plugin.getEconomy() != null) plugin.getEconomy().withdrawPlayer(p, horseCrate.getPrice());
				
				Map<ItemStack, Integer> itemsMissing = horseCrate.getMissingItems(p);
				
				if (!itemsMissing.isEmpty()) {
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
					
					this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.missing-items-crate"), "ITEMS", items);
					return false;
				}
				
				Inventory inv = p.getInventory();
				
				for (ItemStack itemNeeded : horseCrate.getItemsNeeded()) {
					int amountNeeded = itemNeeded.getAmount();
					for (int slot = 0; slot < inv.getSize(); slot++) {
						ItemStack item = inv.getItem(slot);
						if (ItemUtil.itemIsReal(item)) {
							if (item.isSimilar(itemNeeded)) {
								int amount = item.getAmount();
								
								if (amount <= amountNeeded) {
									inv.setItem(slot, null);
								} else {
									item.setAmount(item.getAmount() - amountNeeded);
								}
								
								amountNeeded -= amount;
								
								if (amountNeeded <= 0) continue;
							}
						}
					}
				}
				
				RPGHorse rpgHorse = horseCrate.getRPGHorse(horseOwner);
				rpgHorse.setName(plugin.getConfig().getString("horse-options.default-name", "Horse").replace("{PLAYER}", p.getName()));
				horseOwner.addRPGHorse(rpgHorse);
				this.stableGUIManager.setupStableGUI(horseOwner);
				this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-crate-bought").replace("{HORSE-CRATE}", horseCrate.getName()).replace("{PRICE}", "" + horseCrate.getPrice()), rpgHorse);
				return true;
			}
			
			if (arg1.equalsIgnoreCase("market")) {
				if (!sender.hasPermission("rpghorses.market")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}
				
				if (!(sender instanceof Player)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Player p = (Player) sender;
				
				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
				horseOwner.openMarketGUIPage(this.marketGUIManager.getPage(1));
				return true;
			}
			
			if (arg1.equalsIgnoreCase("trail")) {
				
				if (!(sender instanceof Player)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Player p = (Player) sender;
				
				if (args.length < 2) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label + " trail <particle>");
					return false;
				}
				
				String particleArg = args[1];
				if (!particleManager.isValidParticle(particleArg)) {
					this.messagingUtil.sendMessage(p, "{PREFIX}Invalid particle argument: " + particleArg);
					this.messagingUtil.sendMessage(p, "{PREFIX}Valid particles: " + this.particleManager.getParticleList());
					return false;
				}
				
				if (!p.hasPermission("rpghorses.trail." + particleArg.toLowerCase()) && !p.hasPermission("rpghorses.trail.*")) {
					this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.no-permission-particles").replace("{PARTICLE}", particleArg.toLowerCase()));
					return false;
				}
				
				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
				RPGHorse rpgHorse = horseOwner.getCurrentHorse();
				if (rpgHorse == null) {
					this.messagingUtil.sendMessageAtPath(p, "messages.particle-fail");
					return false;
				}
				
				if (RPGHorsesMain.getVersion().getWeight() < 9) {
					((LegacyHorseInfo) rpgHorse.getHorseInfo()).setEffect(Effect.valueOf(particleArg.toUpperCase()));
				} else {
					rpgHorse.setParticle(Particle.valueOf(particleArg.toUpperCase()));
				}
				this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.particle-set").replace("{HORSE-NUMBER}", "" + horseOwner.getHorseNumber(rpgHorse)).replace("{PARTICLE}", particleArg.toUpperCase()), rpgHorse);
				return true;
			}

			if (arg1.equalsIgnoreCase("togglehorse")) {
				if (!sender.hasPermission("rpghorses.togglehorse")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				if (!(sender instanceof Player)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Player p = (Player) sender;

				if (args.length < 2) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label + " togglehorse <horse-number>");
					return false;
				}

				String horseNumberArg = args[1];
				if (!this.runHorseNumberCheck(sender, horseNumberArg, p)) {
					return false;
				}
				int horseNumber = Integer.parseInt(horseNumberArg), index = horseNumber - 1;

				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
				RPGHorse rpgHorse = horseOwner.getRPGHorse(index);
				horseOwner.toggleRPGHorse(rpgHorse);
				return true;
			}
			
			if (!sender.hasPermission("rpghorses.help")) {
				this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
				return false;
			}
			
			plugin.sendHelpMessage(sender, label);
			return true;
		}
		
		if (!sender.hasPermission("rpghorses.stable")) {
			this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
			return false;
		}
		
		if (!(sender instanceof Player)) {
			this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
			return false;
		}
		Player p = (Player) sender;
		
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
		if (horseOwner.getStableGUI() == null) {
			this.stableGUIManager.setupStableGUI(horseOwner);
		} else {
			for (RPGHorse rpgHorse : horseOwner.getRPGHorses()) {
				if (rpgHorse.hasGainedXP()) {
					stableGUIManager.updateRPGHorse(rpgHorse);
					rpgHorse.setGainedXP(false);
				}
			}
		}
		
		if (horseOwner.getRPGHorses().size() == 0) {
			this.messagingUtil.sendMessageAtPath(p, "messages.no-horses");
			return false;
		}
		
		horseOwner.openStableGUIPage(1);
		return true;
	}
	
	private boolean runHorseNumberCheck(CommandSender sender, String horseNumberArg, OfflinePlayer offlinePlayer) {
		if (horseNumberArg != null) {
			if (!NumberUtil.isPositiveInt(horseNumberArg) || Integer.valueOf(horseNumberArg) < 1) {
				this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid horse-number value: &6" + horseNumberArg);
				return false;
			}
			
			if (offlinePlayer != null) {
				int horseCount = this.horseOwnerManager.getHorseCount(offlinePlayer);
				if (horseCount < Integer.valueOf(horseNumberArg)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}&6" + offlinePlayer.getName() + " &7only has &6" + horseCount + " &7RPGHorses");
					return false;
				}
			}
		}
		return true;
	}
}
