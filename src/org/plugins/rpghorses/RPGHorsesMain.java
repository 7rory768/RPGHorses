package org.plugins.rpghorses;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.plugins.rpghorses.commands.RPGHorsesAdminCommand;
import org.plugins.rpghorses.commands.RPGHorsesCommand;
import org.plugins.rpghorses.configs.CustomConfig;
import org.plugins.rpghorses.configs.PlayerConfigs;
import org.plugins.rpghorses.listeners.*;
import org.plugins.rpghorses.managers.*;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.CustomConfigUtil;
import org.plugins.rpghorses.utils.ItemUtil;
import org.plugins.rpghorses.utils.MessagingUtil;

import java.util.logging.Level;

public class RPGHorsesMain extends JavaPlugin {

    private CustomConfig marketConfig;
    private PlayerConfigs playerConfigs;
    private ItemUtil itemUtil;
    private MessagingUtil messagingUtil;
    private HorseDespawner horseDespawner;
    private HorseOwnerManager horseOwnerManager;
    private StableGUIManager stableGuiManager;
    private MarketGUIManager marketGUIManager;
    private RPGHorseManager rpgHorseManager;
    private HorseCrateManager horseCrateManager;
    private ParticleManager particleManager;
    private XPManager xpManager;
    private MessageQueuer messageQueuer;
    private Permission permissions;
    private Economy economy;

    @Override
    public void onEnable() {
        this.loadConfigs();
        if (this.loadHooks()) {
            Bukkit.getLogger().log(Level.INFO, "[RPGHorses] Successfully hooked into Vault");
            this.initializeVariables();
            this.registerEvents();
            this.loadCommands();
        } else {
            new PluginEnableListener(this);
            Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to hook into Vault, plugin disabled");
        }
    }

    @Override
    public void onDisable() {
        if (this.horseDespawner != null) {
            this.horseDespawner.despawnAllRPGHorses();
            this.horseDespawner.cancelIdleHorseTask();
        }

        if (this.stableGuiManager != null) {
            this.stableGuiManager.cancelCooldownTask();
        }

        if (this.horseOwnerManager != null) {
            for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners()) {
                if (horseOwner.isInStableInventory() || horseOwner.isInMarketInventory()) {
                    Bukkit.getPlayer(horseOwner.getUUID()).closeInventory();
                }
            }

            this.horseOwnerManager.saveData();
        }
    }

    public void loadConfigs() {
        CustomConfigUtil.loadDefaultConfig(this);

        this.playerConfigs = new PlayerConfigs(this);

        this.marketConfig = new CustomConfig(this, "market");
        CustomConfigUtil.loadConfig(this.marketConfig);
    }

    public boolean loadHooks() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            this.permissions = permissionProvider.getProvider();
        }

        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            this.economy = economyProvider.getProvider();
        }

        return this.permissions != null && this.economy != null;
    }

    public void initializeVariables() {
        this.messagingUtil = new MessagingUtil(this);
        this.itemUtil = new ItemUtil(this, this.messagingUtil);
        this.horseOwnerManager = new HorseOwnerManager(this, this.playerConfigs, this.permissions);
        this.rpgHorseManager = new RPGHorseManager(this, this.horseOwnerManager, this.economy);
        this.stableGuiManager = new StableGUIManager(this, this.horseOwnerManager, this.rpgHorseManager, this.itemUtil, this.messagingUtil);
        this.marketGUIManager = new MarketGUIManager(this, this.horseOwnerManager, this.rpgHorseManager, this.marketConfig, this.itemUtil);
        this.messageQueuer = new MessageQueuer(this.playerConfigs, this.messagingUtil);
        this.horseDespawner = new HorseDespawner(this, this.horseOwnerManager, this.rpgHorseManager);
        this.horseCrateManager = new HorseCrateManager(this);
        this.particleManager = new ParticleManager(this, this.horseOwnerManager);
        this.xpManager = new XPManager(this, rpgHorseManager, messagingUtil);
    }

    public void registerEvents() {
        new EntityDamageByEntityListener(this, this.rpgHorseManager, this.stableGuiManager);
        new EntityDeathListener(this, this.rpgHorseManager, this.stableGuiManager, xpManager);
        new EntitySpawnListener(this, this.rpgHorseManager, this.horseOwnerManager);
        new InventoryClickListener(this, this.horseOwnerManager, this.rpgHorseManager, this.stableGuiManager, this.marketGUIManager, this.economy, this.messageQueuer, this.messagingUtil);
        new InventoryCloseListener(this, this.horseOwnerManager, this.stableGuiManager);
        new PlayerChatListener(this, this.rpgHorseManager, this.stableGuiManager, this.messagingUtil);
        new PlayerInteractEntityListener(this, this.rpgHorseManager, this.messagingUtil);
        new PlayerJoinListener(this, this.horseOwnerManager, this.stableGuiManager, this.marketGUIManager, this.messageQueuer);
        new PlayerQuitListener(this, this.horseOwnerManager, xpManager);
        new PlayerTeleportListener(this, this.horseOwnerManager, this.messagingUtil);
        new PreCommandListener(this, this.rpgHorseManager, this.stableGuiManager, this.marketGUIManager, this.messagingUtil);
        new VehicleListener(this, this.rpgHorseManager, this.xpManager, this.messagingUtil);
    }

    public void loadCommands() {
        this.getCommand("rpghorses").setExecutor(new RPGHorsesCommand(this, this.horseOwnerManager, this.stableGuiManager, this.marketGUIManager, this.horseCrateManager, this.particleManager, this.economy, this.messagingUtil));
        this.getCommand("rpghorsesadmin").setExecutor(new RPGHorsesAdminCommand(this, this.horseOwnerManager, this.rpgHorseManager, this.stableGuiManager, this.horseDespawner, this.horseCrateManager, this.marketGUIManager, this.particleManager, this.messageQueuer, this.messagingUtil));
    }
}
