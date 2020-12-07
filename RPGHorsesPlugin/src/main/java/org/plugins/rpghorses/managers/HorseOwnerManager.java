package org.plugins.rpghorses.managers;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import roryslibrary.configs.PlayerConfigs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class HorseOwnerManager {
	
	private final RPGHorsesMain plugin;
	private final HorseCrateManager horseCrateManager;
	private final SQLManager sqlManager;
	private final PlayerConfigs playerConfigs;
	private final Permission permissions;
	
	private HashSet<HorseOwner> horseOwners = new HashSet<>();
	
	public HorseOwnerManager(RPGHorsesMain plugin, HorseCrateManager horseCrateManager, PlayerConfigs playerConfigs, Permission permissions) {
		this.plugin = plugin;
		this.sqlManager = plugin.getSQLManager();
		this.horseCrateManager = horseCrateManager;
		this.playerConfigs = playerConfigs;
		this.permissions = permissions;
		
		// load owners
		this.loadData();
	}
	
	public void loadData() {
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			this.loadData(p);
		}
	}
	
	public HorseOwner loadData(Player p) {
		if (p.isOnline()) {
			return this.loadData(p.getUniqueId());
		}
		return null;
	}
	
	public HorseOwner loadData(UUID uuid) {
		FileConfiguration config = this.playerConfigs.getConfig(uuid);
		
		HorseOwner horseOwner;
		if (sqlManager != null) {
			horseOwner = sqlManager.loadPlayer(uuid);
		} else {
			horseOwner = new HorseOwner(uuid);
			
			horseOwner.setReceivedDefaultHorse(config.getBoolean("received-default-horse", false));
			horseOwner.setAutoMount(config.getBoolean("auto-mount", true));
			
			if (config.getConfigurationSection("rpghorses") != null) {
				for (String horseIndex : config.getConfigurationSection("rpghorses").getKeys(false)) {
					String path = "rpghorses." + horseIndex + ".";
					String name = config.getString(path + "name");
					int tier = config.getInt(path + "tier");
					double xp = config.getDouble(path + "xp");
					double health = config.getDouble(path + "health");
					double movementSpeed = config.getDouble(path + "movement-speed");
					double jumpStrength = config.getDouble(path + "jump-strength");
					EntityType entityType = EntityType.HORSE;
					Horse.Variant variant = Horse.Variant.HORSE;
					Horse.Color color = Horse.Color.BROWN;
					Horse.Style style = Horse.Style.NONE;
					try {
						color = Horse.Color.valueOf(config.getString(path + "color", "BROWN"));
					} catch (IllegalArgumentException e) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + config.getString(path + "color") + " is not a valid color )");
					}
					try {
						style = Horse.Style.valueOf(config.getString(path + "style", "NONE"));
					} catch (IllegalArgumentException e) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + config.getString(path + "style") + " is not a valid style )");
					}
					
					Long deathTime = Long.valueOf(config.getString(path + "death-time", "0"));
					boolean isDead = deathTime + (this.plugin.getConfig().getInt("horse-options.death-cooldown") * 1000) - System.currentTimeMillis() > 0;
					
					boolean inMarket = config.getBoolean(path + "in-market", false);
					Particle particle = null;
					if (RPGHorsesMain.getVersion().getWeight() >= 9 && config.isSet(path + "particle")) {
						particle = Particle.valueOf(config.getString(path + "particle"));
					}
					
					HashMap<Integer, ItemStack> items = null;
					if (config.isSet(path + "items")) {
						items = new HashMap<>();
						for (String slotStr : config.getConfigurationSection(path + "items").getKeys(false)) {
							items.put(Integer.valueOf(slotStr), config.getItemStack(path + "items." + slotStr));
						}
					}
					
					
					AbstractHorseInfo horseInfo;
					if (plugin.getVersion().getWeight() < 11) {
						try {
							variant = Horse.Variant.valueOf(config.getString(path + "variant", "HORSE"));
						} catch (IllegalArgumentException e) {
							Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + config.getString(path + "variant") + " is not a valid variant )");
						}
						horseInfo = new LegacyHorseInfo(style, color, variant);
					} else {
						try {
							entityType = EntityType.valueOf(config.getString(path + "type", "HORSE"));
						} catch (IllegalArgumentException e) {
							Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + config.getString(path + "type") + " is not a valid entityType )");
						}
						horseInfo = new HorseInfo(entityType, style, color);
					}
					
					if (RPGHorsesMain.getVersion().getWeight() < 9 && config.isSet(path + "particle")) {
						Effect effect = Effect.valueOf(config.getString(path + "particle"));
						((LegacyHorseInfo) horseInfo).setEffect(effect);
					}
					
					RPGHorse rpgHorse = new RPGHorse(horseOwner, tier, xp, name, health, movementSpeed, jumpStrength, horseInfo, inMarket, particle, items);
					
					if (isDead) {
						rpgHorse.setDead(true);
						rpgHorse.setDeathTime(deathTime);
					}
					
					horseOwner.addRPGHorse(rpgHorse);
				}
			}
		}
		
		if (!horseOwner.hasReceivedDefaultHorse() && horseCrateManager.getDefaultHorseCrate() != null && horseOwner.getPlayer() != null) {
			RPGHorse rpgHorse = horseCrateManager.getDefaultHorseCrate().getRPGHorse(horseOwner);
			rpgHorse.setName(plugin.getConfig().getString("horse-options.default-name", "Horse").replace("{PLAYER}", Bukkit.getPlayer(uuid).getName()));
			horseOwner.addRPGHorse(rpgHorse);
			horseOwner.setReceivedDefaultHorse(true);
		}
		
		if (horseOwner.getPlayer() != null) this.horseOwners.add(horseOwner);
		return horseOwner;
	}
	
	public HashSet<HorseOwner> getHorseOwners() {
		return horseOwners;
	}
	
	public void flushHorseOwner(HorseOwner horseOwner) {
		this.saveData(this.removeHorseOwner(horseOwner));
	}
	
	private HorseOwner removeHorseOwner(HorseOwner horseOwner) {
		if (horseOwner != null) {
			this.horseOwners.remove(horseOwner);
			horseOwner.setCurrentHorse(null);
		}
		return horseOwner;
	}
	
	public void saveData(HorseOwner horseOwner) {
		UUID uuid = horseOwner.getUUID();
		
		if (sqlManager != null) {
			sqlManager.savePlayer(horseOwner);
		} else {
			FileConfiguration config = this.playerConfigs.getConfig(uuid);
			if (config.getConfigurationSection("rpghorses") != null) {
				config.set("rpghorses", null);
			}
			config.createSection("rpghorses");
			int count = 0;
			for (RPGHorse rpgHorse : horseOwner.getRPGHorses()) {
				String path = "rpghorses." + ++count + ".";
				config.set(path + "name", rpgHorse.getName());
				config.set(path + "tier", rpgHorse.getTier());
				config.set(path + "xp", rpgHorse.getXp());
				config.set(path + "health", rpgHorse.getHealth());
				config.set(path + "movement-speed", rpgHorse.getMovementSpeed());
				config.set(path + "jump-strength", rpgHorse.getJumpStrength());
				config.set(path + "type", rpgHorse.getEntityType().name());
				config.set(path + "color", rpgHorse.getColor().name());
				config.set(path + "style", rpgHorse.getStyle().name());
				if (plugin.getVersion().getWeight() < 11) {
					config.set(path + "variant", ((LegacyHorseInfo) rpgHorse.getHorseInfo()).getVariant().name());
				}
				config.set(path + "death-time", rpgHorse.getDeathTime());
				config.set(path + "in-market", rpgHorse.isInMarket());
				if (RPGHorsesMain.getVersion().getWeight() < 9) {
					Effect effect = ((LegacyHorseInfo) rpgHorse.getHorseInfo()).getEffect();
					if (effect != null) {
						config.set(path + "particle", effect.name());
					}
				} else if (rpgHorse.getParticle() != null) {
					config.set(path + "particle", rpgHorse.getParticle().name());
				}
				config.createSection(path + "items");
				HashMap<Integer, ItemStack> items = rpgHorse.getItems();
				for (Integer slot : items.keySet()) {
					config.set(path + "items." + slot, items.get(slot));
				}
			}
			
			config.set("received-default-horse", horseOwner.hasReceivedDefaultHorse());
			config.set("auto-mount", horseOwner.autoMountOn());
			
			if (count == 0) {
				this.playerConfigs.deleteConfig(uuid);
			} else {
				this.playerConfigs.saveConfig(uuid);
				this.playerConfigs.reloadConfig(uuid);
			}
		}
	}
	
	public int getHorseLimit(OfflinePlayer p) {
		if (this.permissions.playerHas(null, p, "rpghorses.limit.*")) {
			return Integer.MAX_VALUE;
		}
		
		for (int i = 200; i > 0; i--) {
			if (this.permissions.playerHas(null, p, "rpghorses.limit." + i)) {
				return i;
			}
		}
		return 0;
	}
	
	public int getHorseCount(OfflinePlayer p) {
		if (p.isOnline()) {
			return this.getHorseCount(p.getPlayer());
		}
		
		FileConfiguration config = this.playerConfigs.getConfig(p.getUniqueId());
		if (config.isConfigurationSection("rpghorses")) {
			return config.getConfigurationSection("rpghorses").getKeys(false).size();
		} else {
			playerConfigs.deleteConfig(p.getUniqueId());
		}
		return 0;
	}
	
	public int getHorseCount(Player p) {
		return this.getHorseOwner(p).getRPGHorses().size();
	}
	
	public HorseOwner getHorseOwner(OfflinePlayer p) {
		return this.getHorseOwner(p.getUniqueId());
	}
	
	private HorseOwner getHorseOwner(UUID uuid) {
		for (HorseOwner horseOwner : this.horseOwners) {
			if (horseOwner.getUUID().equals(uuid)) {
				return horseOwner;
			}
		}
		return this.loadData(uuid);
	}
	
	public void saveData() {
		for (HorseOwner horseOwner : this.horseOwners) {
			this.saveData(horseOwner);
		}
	}
	
	public void saveData(Player p) {
		this.saveData(this.getHorseOwner(p));
	}
}
