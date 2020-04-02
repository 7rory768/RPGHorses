package org.plugins.rpghorses;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.plugins.rpghorses.commands.RPGHorsesAdminCommand;
import org.plugins.rpghorses.commands.RPGHorsesCommand;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.listeners.*;
import org.plugins.rpghorses.managers.*;
import org.plugins.rpghorses.managers.gui.*;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.version.Version;
import rorys.library.configs.CustomConfig;
import rorys.library.configs.PlayerConfigs;
import rorys.library.util.CustomConfigUtil;
import rorys.library.util.ItemUtil;
import rorys.library.util.UpdateNotifier;

import java.util.LinkedHashMap;
import java.util.Map;

/*
TODO:
 Add bStats
 Add update notifier
 */

/*
Future ideas
TODO:
 - Change the trail skin-value to the trail skin-value the horse is currently using
 - Change it so admins can go into other players stables/horse guis
 - Keep horse gui open after calling/dismissing horse
 */
public class RPGHorsesMain extends JavaPlugin {

	private static RPGHorsesMain     plugin;
	private static Version           version = Version.v1_15;
	private static NMS               NMS;
	private        RPGHorseManager   rpgHorseManager;
	private CustomConfig      marketConfig;
	private PlayerConfigs     playerConfigs;
	private ItemUtil          itemUtil;
	private RPGMessagingUtil  messagingUtil;
	private HorseDespawner    horseDespawner;
	private HorseOwnerManager horseOwnerManager;
	private StableGUIManager  stableGuiManager;
	private MarketGUIManager  marketGUIManager;
	private HorseGUIManager   horseGUIManager;
	private TrailGUIManager   trailGUIManager;
	private SellGUIManager    sellGUIManager;
	private HorseCrateManager horseCrateManager;
	private ParticleManager   particleManager;
	private XPManager         xpManager;
	private UpdateNotifier updateNotifier;
	private MessageQueuer     messageQueuer;
	private Permission        permissions;
	private Economy           economy;

	private Map<String, String> helpMessages = new LinkedHashMap<>();

	public static Version getVersion() {
		return version;
	}

	public static NMS getNMS() {
		return NMS;
	}

	public static RPGHorsesMain getInstance() {
		return plugin;
	}

	public RPGHorseManager getRpgHorseManager() {
		return rpgHorseManager;
	}

	@Override
	public void onEnable() {
		plugin = this;


		version = Version.getByName(Bukkit.getBukkitVersion().split("-")[0]);
		Bukkit.getLogger().info("[RPGHorses] " + version.getName() + " detected");

		try {
			final Class<?> clazz = Class.forName("org.plugins.rpghorses." + version.getAbstractName() + ".NMSHandler");
			if (NMS.class.isAssignableFrom(clazz)) {
				NMS = (NMS) clazz.getConstructor().newInstance();
			}
		} catch (final Exception e) {
			this.getLogger().info("Could not find nms support for this version (" + version.getName() + ").");
			this.getLogger().info("Because of this, horses will wander when unmounted");
		}

		if (this.loadHooks()) {
			this.messagingUtil = new RPGMessagingUtil(this);
			messagingUtil.sendMessage(Bukkit.getConsoleSender(), "[RPGHorses] Successfully hooked into &aVault");
			this.loadConfigs();
			this.initializeVariables();
			this.registerEvents();
			this.loadCommands();
			setupHelpMessage();
			Metrics metrics = new Metrics(this, 6955);
		} else {
			messagingUtil.sendMessage(Bukkit.getConsoleSender(), "[RPGHorses] Failed to hook into &cVault&r, plugin disabled");
			plugin.getServer().getPluginManager().disablePlugin(this);
		}
	}

	public void loadConfigs() {
		CustomConfigUtil.loadDefaultConfig(this);

		this.playerConfigs = new PlayerConfigs(this);

		this.marketConfig = new CustomConfig(this, "market");
		CustomConfigUtil.loadConfig(this.marketConfig);
	}

	public boolean loadHooks() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null) {
			this.permissions = permissionProvider.getProvider();
		}

		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			this.economy = economyProvider.getProvider();
		}

		return this.permissions != null && this.economy != null;
	}

	public void initializeVariables() {
		this.updateNotifier = new UpdateNotifier(this, messagingUtil, 76836);
		this.updateNotifier.checkForUpdate();
		this.itemUtil = new ItemUtil(this);
		this.horseCrateManager = new HorseCrateManager(this);
		this.horseOwnerManager = new HorseOwnerManager(this, this.horseCrateManager, this.playerConfigs, this.permissions);
		this.rpgHorseManager = new RPGHorseManager(this, this.horseOwnerManager, this.economy);
		this.stableGuiManager = new StableGUIManager(this, this.horseOwnerManager, this.rpgHorseManager, this.itemUtil, this.messagingUtil);
		this.marketGUIManager = new MarketGUIManager(this, this.horseOwnerManager, this.rpgHorseManager, this.marketConfig, this.itemUtil);
		this.horseGUIManager = new HorseGUIManager(this, this.stableGuiManager);
		this.messageQueuer = new MessageQueuer(this.playerConfigs, this.messagingUtil);
		this.horseDespawner = new HorseDespawner(this, this.horseOwnerManager, this.rpgHorseManager);
		this.particleManager = new ParticleManager(this, this.horseOwnerManager);
		this.trailGUIManager = new TrailGUIManager(this, particleManager, this.horseOwnerManager);
		this.sellGUIManager = new SellGUIManager(this, stableGuiManager);
		this.xpManager = new XPManager(this, rpgHorseManager, messagingUtil);
	}

	public void registerEvents() {
		new EntityDamageByEntityListener(this, this.rpgHorseManager, this.stableGuiManager);
		new EntityDeathListener(this, this.rpgHorseManager, this.stableGuiManager, xpManager, messagingUtil);
		new EntitySpawnListener(this, this.rpgHorseManager, this.horseOwnerManager);
		new InventoryClickListener(this, this.horseOwnerManager, this.rpgHorseManager, this.stableGuiManager, this.marketGUIManager, this.horseGUIManager, this.trailGUIManager, this.sellGUIManager, this.economy, this.messageQueuer, this.messagingUtil);
		new InventoryCloseListener(this, this.horseOwnerManager, this.stableGuiManager);
		new PlayerChatListener(this, this.rpgHorseManager, this.stableGuiManager, this.messagingUtil);
		new PlayerInteractEntityListener(this, this.rpgHorseManager, this.messagingUtil);
		new PlayerJoinListener(this, this.horseOwnerManager, this.stableGuiManager, this.marketGUIManager, this.messagingUtil, this.messageQueuer, updateNotifier);
		new PlayerQuitListener(this, this.horseOwnerManager, xpManager);
		new PlayerTeleportListener(this, this.horseOwnerManager, this.messagingUtil);
		new PreCommandListener(this, this.rpgHorseManager, this.stableGuiManager, this.marketGUIManager, this.messagingUtil);
		new VehicleListener(this, this.rpgHorseManager, this.xpManager, this.messagingUtil);
	}

	public void loadCommands() {
		this.getCommand("rpghorses").setExecutor(new RPGHorsesCommand(this, this.horseOwnerManager, this.stableGuiManager, this.marketGUIManager, this.horseCrateManager, this.particleManager, this.economy, this.messagingUtil));
		this.getCommand("rpghorsesadmin").setExecutor(new RPGHorsesAdminCommand(this, this.horseOwnerManager, this.rpgHorseManager, this.stableGuiManager, this.horseDespawner, this.horseCrateManager, this.marketGUIManager, this.particleManager, this.messageQueuer, this.messagingUtil));
	}

	public void setupHelpMessage() {
		helpMessages.put("                                &6&lRPGHorses", "rpghorses.help");
		helpMessages.put("&6/rpghorses &8- &7Opens the stable gui", "rpghorses.stable");
		helpMessages.put("&6/{LABEL} help &8- &7Displays this message", "rpghorses.help");
		helpMessages.put("&6/rpghorses market &8- &7Opens the market", "rpghorses.market");
		helpMessages.put("&6/rpghorses sell <horse-number> <price> &8- &7Sells a horse to the market", "rpghorses.sell");
		helpMessages.put("&6/rpghorses buy <horse-crate> <price> &8- &7Buys a new horse", "rpghorses.buy");
		helpMessages.put("&6/rpghorses trail <particle> &8- &7Sets the trail of your current horse", "rpghorses.trail");
		helpMessages.put("&6/rpghorsesadmin give <horse-crate> <player> &8- &7Gives a player a horse crate", "rpghorses.give");
		helpMessages.put("&6/rpghorsesadmin give <health> <movement-speed> <jump-strength> <type> [color] [style] <player> &8- &7Gives a player a RPGHorse", "rpghorses.give");
		//helpMessage.put("&6/rpghorsesadmin set <health> <movement-speed> <jump-height> <type> <color> <style> <horse-number> <player> &8- &7Modifies an existing RPGHorse");
		helpMessages.put("&6/rpghorsesadmin remove <horse-number> <player> &8- &7Removes a RPGHorse from a player's stable", "rpghorses.remove");
		helpMessages.put("&6/rpghorsesadmin upgrade <horse-number> <player> &8- &7Upgrades a player's RPGHorse", "rpghorses.upgrade");
		helpMessages.put("&6/rpghorsesadmin listall &8- &7Lists all active RPGHorses", "rpghorses.listall");
		helpMessages.put("&6/rpghorsesadmin check <radius> &8- &7Lists all active RPGHorses within a certain radius", "rpghorses.check");
		helpMessages.put("&6/rpghorsesadmin removenear <radius> &8- &7Removes all RPGHorses within a certain radius", "rpghorses.removenear");
		helpMessages.put("&6/rpghorsesadmin reload &8- &7Reloads the configuration file", "rpghorses.reload");
	}

	@Override
	public void onDisable() {
		this.horseDespawner.despawnAllRPGHorses();

		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners()) {
			if (horseOwner.getGUILocation() != GUILocation.NONE) {
				horseOwner.getPlayer().closeInventory();
			}
		}

		this.horseOwnerManager.saveData();
	}

	public void sendHelpMessage(CommandSender sender, String label) {
		label = label.toLowerCase();
		String  rpgReplace    = label.contains("rpg") ? "rpg" : "";
		String  horsesReplace = label.contains("horses") ? "horses" : "horse";
		boolean allPerms      = sender.hasPermission("rpghorses.*") || sender.isOp();
		for (String line : plugin.getHelpMessages().keySet()) {
			if (allPerms || sender.hasPermission(plugin.getHelpMessages().get(line))) {
				this.messagingUtil.sendMessage(sender, line.replace("horses", horsesReplace).replace("rpg", rpgReplace).replace("{LABEL}", label));
			}
		}
	}

	public Map<String, String> getHelpMessages() {
		return helpMessages;
	}
}
