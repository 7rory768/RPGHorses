package org.plugins.rpghorses.managers;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.BukkitSerialization;
import rorys.library.configs.PlayerConfigs;
import rorys.library.util.ItemUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class SQLManager extends rorys.library.managers.SQLManager {
	
	private final RPGHorsesMain plugin;
	private final PlayerConfigs playerConfigs;
	
	private final String HORSE_TABLE = "rpghorses_horses", PLAYER_TABLE = "rpghorses_players";
	
	private final String LOAD_PLAYER, SAVE_PLAYER, LOAD_HORSES, SAVE_HORSE, DELETE_HORSES;
	
	public SQLManager(RPGHorsesMain plugin) {
		super(plugin, "");
		
		this.plugin = plugin;
		this.playerConfigs = plugin.getPlayerConfigs();
		
		LOAD_PLAYER = "SELECT * FROM " + PLAYER_TABLE + " WHERE uuid=?;";
		SAVE_PLAYER = "INSERT INTO " + PLAYER_TABLE + " (uuid, default_horse, auto_mount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE uuid=uuid, default_horse=default_horse, auto_mount=auto_mount;";
		
		LOAD_HORSES = "SELECT * FROM " + HORSE_TABLE + " WHERE owner=? ORDER BY id;";
		SAVE_HORSE = "INSERT INTO " + HORSE_TABLE + " (id, owner, name, tier, xp, health, movement_speed, jump_strength, color, style, type, variant, death_time, in_market, particle, items) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
		DELETE_HORSES = "DELETE FROM " + HORSE_TABLE + " WHERE owner=?;";
		
		load();
	}
	
	public void load() {
		createPlayerTable();
		createHorseTable();
	}
	
	public void createPlayerTable() {
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					execute("CREATE TABLE IF NOT EXISTS " + PLAYER_TABLE + "(uuid VARCHAR(36) PRIMARY KEY NOT NULL, default_horse BOOLEAN DEFAULT 0, auto_mount BOOLEAN DEFAULT 1);");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}
	
	public void createHorseTable() {
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					execute("CREATE TABLE IF NOT EXISTS " + HORSE_TABLE + "(id INTEGER NOT NULL, owner VARCHAR(36) NOT NULL, name VARCHAR(64) NOT NULL, tier INTEGER DEFAULT 1, xp DOUBLE DEFAULT 0, health DOUBLE NOT NULL, movement_speed DOUBLE NOT NULL, jump_strength DOUBLE NOT NULL, color VARCHAR(16), style VARCHAR(16), type VARCHAR(32), variant VARCHAR(32), death_time BIGINT DEFAULT 0, in_market BOOLEAN DEFAULT 0, particle VARCHAR(32), items TEXT);");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}
	
	public HorseOwner loadPlayer(UUID uuid) {
		String uuidStr = uuid.toString();
		
		HorseOwner horseOwner = new HorseOwner(uuid);
		
		try {
			Connection con = getConnection();
			
			// LOAD: Player data
			PreparedStatement preparedStatement = con.prepareStatement(LOAD_PLAYER);
			preparedStatement.setString(1, uuidStr);
			
			ResultSet set = preparedStatement.executeQuery();
			
			boolean defaultHorse = false, autoMount = true;
			
			if (set.next()) {
				defaultHorse = set.getBoolean("default_horse");
				autoMount = set.getBoolean("auto_mount");
			}
			
			horseOwner.setReceivedDefaultHorse(defaultHorse);
			horseOwner.setAutoMount(autoMount);
			
			// LOAD: Horses
			preparedStatement = con.prepareStatement(LOAD_HORSES);
			preparedStatement.setString(1, uuidStr);
			
			set = preparedStatement.executeQuery();
			
			while (set.next()) {
				int index = set.getInt("id");
				String name = set.getString("name");
				int tier = set.getInt("tier");
				double xp = set.getDouble("xp");
				double health = set.getDouble("health");
				double movementSpeed = set.getDouble("movement_speed");
				double jumpStrength = set.getDouble("jump_strength");
				EntityType entityType = EntityType.HORSE;
				Horse.Variant variant = Horse.Variant.HORSE;
				Horse.Color color = Horse.Color.BROWN;
				Horse.Style style = Horse.Style.NONE;
				try {
					color = Horse.Color.valueOf(set.getString("color"));
				} catch (IllegalArgumentException e) {
					Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + set.getString("color") + " is not a valid color )");
				}
				try {
					style = Horse.Style.valueOf(set.getString("style"));
				} catch (IllegalArgumentException e) {
					Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + set.getString("style") + " is not a valid style )");
				}
				
				HashMap<Integer, ItemStack> items = new HashMap<>();
				if (!plugin.getConfig().getBoolean("mysql.save-items")) {
					String itemsString = set.getString("items");
					if (itemsString != null && !itemsString.equals("")) {
						try {
							ItemStack[] itemArray = BukkitSerialization.itemStackArrayFromBase64(itemsString);
							for (int i = 0; i < itemArray.length; i++) {
								if (ItemUtil.itemIsReal(itemArray[i])) items.put(i, itemArray[i]);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					FileConfiguration config = playerConfigs.getConfig(horseOwner.getUUID());
					if (config.isSet("rpghorses." + (index + 1) + ".items")) {
						for (String slotStr : config.getConfigurationSection("rpghorses." + (index + 1) + ".items").getKeys(false)) {
							items.put(Integer.valueOf(slotStr), config.getItemStack("rpghorses." + (index + 1) + ".items." + slotStr));
						}
					}
				}
				
				
				AbstractHorseInfo horseInfo;
				if (plugin.getVersion().getWeight() < 11) {
					try {
						variant = Horse.Variant.valueOf(set.getString("variant"));
					} catch (IllegalArgumentException e) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + set.getString("variant") + " is not a valid variant )");
					}
					horseInfo = new LegacyHorseInfo(style, color, variant);
				} else {
					try {
						entityType = EntityType.valueOf(set.getString("type"));
					} catch (IllegalArgumentException e) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + uuid.toString() + "'s horse ( " + set.getString("type") + " is not a valid entityType )");
					}
					horseInfo = new HorseInfo(entityType, style, color);
				}
				
				Long deathTime = set.getLong("death_time");
				boolean isDead = deathTime + (this.plugin.getConfig().getInt("horse-options.death-cooldown") * 1000) - System.currentTimeMillis() > 0;
				
				boolean inMarket = set.getBoolean("in_market");
				Particle particle = null;
				String particleStr = set.getString("particle");
				if (particleStr != null && !particleStr.equals("")) {
					if (RPGHorsesMain.getVersion().getWeight() >= 9) {
						particle = Particle.valueOf(set.getString("particle"));
					} else {
						Effect effect = Effect.valueOf(set.getString("particle"));
						((LegacyHorseInfo) horseInfo).setEffect(effect);
					}
				}
				
				RPGHorse rpgHorse = new RPGHorse(horseOwner, tier, xp, name, health, movementSpeed, jumpStrength, horseInfo, inMarket, particle, items);
				
				if (isDead) {
					rpgHorse.setDead(true);
					rpgHorse.setDeathTime(deathTime);
				}
				
				horseOwner.addRPGHorse(rpgHorse);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return horseOwner;
	}
	
	public void savePlayer(HorseOwner owner) {
		if (plugin.isEnabled()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					save(owner);
				}
			}.runTaskAsynchronously(plugin);
		} else {
			save(owner);
		}
	}
	
	private void save(HorseOwner owner) {
		String uuidStr = owner.getUUID().toString();
		
		try {
			Connection con = getConnection();
			
			PreparedStatement statement = con.prepareStatement(SAVE_PLAYER);
			statement.setString(1, uuidStr);
			statement.setBoolean(2, owner.hasReceivedDefaultHorse());
			statement.setBoolean(3, owner.autoMountOn());
			statement.executeUpdate();
			
			statement = con.prepareStatement(DELETE_HORSES);
			statement.setString(1, uuidStr);
			statement.executeUpdate();
			
			int index = 0;
			for (RPGHorse horse : owner.getRPGHorses()) {
				statement = con.prepareStatement(SAVE_HORSE);
				statement.setInt(1, index++);
				statement.setString(2, uuidStr);
				statement.setString(3, horse.getName());
				statement.setInt(4, horse.getTier());
				statement.setDouble(5, horse.getXp());
				statement.setDouble(6, horse.getHealth());
				statement.setDouble(7, horse.getMovementSpeed());
				statement.setDouble(8, horse.getJumpStrength());
				statement.setString(9, horse.getColor().name());
				statement.setString(10, horse.getStyle().name());
				statement.setString(11, horse.getEntityType().name());
				
				String variant = "";
				if (plugin.getVersion().getWeight() < 11) {
					variant = ((LegacyHorseInfo) horse.getHorseInfo()).getVariant().name();
				}
				statement.setString(12, variant);
				
				Long deathTime = horse.getDeathTime();
				statement.setLong(13, deathTime == null ? 0L : deathTime);
				
				statement.setBoolean(14, horse.isInMarket());
				
				String particle = "";
				if (plugin.getVersion().getWeight() < 9) {
					Effect effect = ((LegacyHorseInfo) horse.getHorseInfo()).getEffect();
					if (effect != null) {
						particle = effect.name();
					}
				} else if (horse.getParticle() != null) {
					particle = horse.getParticle().name();
				}
				statement.setString(15, particle);
				
				String itemString = "";
				
				if (horse.getHorse() != null && horse.getHorse().isValid()) horse.loadItems();
				HashMap<Integer, ItemStack> items = horse.getItems();
				
				if (plugin.getConfig().getBoolean("mysql.save-items")) {
					int maxSlot = -1;
					for (Integer slot : items.keySet()) {
						if (slot > maxSlot) maxSlot = slot;
					}
					
					if (maxSlot > -1) {
						ItemStack[] itemArray = new ItemStack[maxSlot + 1];
						for (Integer slot : items.keySet()) {
							itemArray[slot] = items.get(slot);
						}
						
						itemString = BukkitSerialization.itemStackArrayToBase64(itemArray);
					}
				}
				
				FileConfiguration config = playerConfigs.getConfig(owner.getUUID());
				config.createSection("rpghorses." + index + ".items");
				for (Integer slot : items.keySet()) {
					config.set("rpghorses." + index + ".items." + slot, items.get(slot));
				}
				
				playerConfigs.saveConfig(owner.getUUID());
				playerConfigs.reloadConfig(owner.getUUID());
				
				statement.setString(16, itemString);
				
				statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
