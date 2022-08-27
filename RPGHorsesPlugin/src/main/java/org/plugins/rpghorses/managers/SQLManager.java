package org.plugins.rpghorses.managers;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.MarketHorse;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.BukkitSerialization;
import org.plugins.rpghorses.utils.MessageType;
import roryslibrary.configs.PlayerConfigs;
import roryslibrary.util.DebugUtil;
import roryslibrary.util.ItemUtil;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLManager extends roryslibrary.managers.SQLManager implements PluginMessageListener {
	
	private final RPGHorsesMain plugin;
	private final PlayerConfigs playerConfigs;
	
	private final String HORSE_TABLE, PLAYER_TABLE, MARKET_TABLE;
	
	private final String LOAD_PLAYER, SAVE_PLAYER;
	private final String LOAD_HORSES, GET_HORSE, SAVE_HORSE, DELETE_HORSES, DELETE_HORSE, LOWER_HORSE_IDS;
	private final String LOAD_MARKET, REMOVE_MARKET_HORSE, ADD_MARKET_HORSE, UPDATE_MARKET_HORSE, LOWER_MARKET_HORSE_IDS, LOWER_MARKET_HORSE_INDEXES;
	
	public SQLManager(RPGHorsesMain plugin) {
		super(plugin, "");
		
		this.plugin = plugin;
		this.playerConfigs = plugin.getPlayerConfigs();
		
		FileConfiguration config = plugin.getConfig();
		
		HORSE_TABLE = "rpghorses_" + config.getString("mysql.horse-table");
		PLAYER_TABLE = "rpghorses_" + config.getString("mysql.player-table");
		MARKET_TABLE = "rpghorses_" + config.getString("mysql.market-table");
		
		LOAD_PLAYER = "SELECT * FROM " + PLAYER_TABLE + " WHERE uuid=?;";
		SAVE_PLAYER = "INSERT INTO " + PLAYER_TABLE + " (uuid, default_horse, auto_mount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE uuid=uuid, default_horse=default_horse, auto_mount=auto_mount;";
		
		LOAD_HORSES = "SELECT * FROM " + HORSE_TABLE + " WHERE owner=? ORDER BY id;";
		GET_HORSE = "SELECT * FROM " + HORSE_TABLE + " WHERE owner=? AND id=? LIMIT 1;";
		SAVE_HORSE = "INSERT INTO " + HORSE_TABLE + " (id, owner, name, tier, xp, health, movement_speed, jump_strength, color, style, type, variant, death_time, in_market, particle, items) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
		DELETE_HORSES = "DELETE FROM " + HORSE_TABLE + " WHERE owner=?;";
		DELETE_HORSE = "DELETE FROM " + HORSE_TABLE + " WHERE owner=? AND id=?;";
		LOWER_HORSE_IDS = "UPDATE " + HORSE_TABLE + " SET id=id-1 WHERE owner=? AND id>?";
		
		LOAD_MARKET = "SELECT * FROM " + MARKET_TABLE + ";";
		REMOVE_MARKET_HORSE = "DELETE FROM " + MARKET_TABLE + " WHERE id=?;";
		UPDATE_MARKET_HORSE = "UPDATE " + HORSE_TABLE + " SET in_market=? WHERE owner=? AND id=?";
		LOWER_MARKET_HORSE_IDS = "UPDATE " + MARKET_TABLE + " SET id=id-1 WHERE id>?";
		ADD_MARKET_HORSE = "INSERT INTO " + MARKET_TABLE + "(id, owner, price, horse_index) VALUES (?,?,?,?);";
		LOWER_MARKET_HORSE_INDEXES = "UPDATE " + MARKET_TABLE + " SET horse_index=horse_index-1 WHERE owner=? AND horse_index>?";
		
		this.plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
		this.plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
		
		load();
	}
	
	public void load() {
		createPlayerTable();
		createHorseTable();
		createMarketTable();
	}
	
	public void clearTables() {
		try {
			execute("DELETE FROM " + PLAYER_TABLE + ";");
			execute("DELETE FROM " + HORSE_TABLE + ";");
			execute("DELETE FROM " + MARKET_TABLE + ";");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createPlayerTable() {
		try {
			execute("CREATE TABLE IF NOT EXISTS " + PLAYER_TABLE + " (uuid VARCHAR(36) PRIMARY KEY NOT NULL, default_horse BOOLEAN DEFAULT 0, auto_mount BOOLEAN DEFAULT 1);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createHorseTable() {
		try {
			execute("CREATE TABLE IF NOT EXISTS " + HORSE_TABLE + " (id INTEGER NOT NULL, owner VARCHAR(36) NOT NULL, name VARCHAR(64) NOT NULL, tier INTEGER DEFAULT 1, xp DOUBLE DEFAULT 0, health DOUBLE NOT NULL, movement_speed DOUBLE NOT NULL, jump_strength DOUBLE NOT NULL, color VARCHAR(16), style VARCHAR(16), type VARCHAR(32), variant VARCHAR(32), death_time BIGINT DEFAULT 0, in_market BOOLEAN DEFAULT 0, particle VARCHAR(32), items TEXT);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createMarketTable() {
		try {
			execute("CREATE TABLE IF NOT EXISTS " + MARKET_TABLE + " (id INTEGER PRIMARY KEY NOT NULL, owner VARCHAR(36) NOT NULL, price DOUBLE NOT NULL, horse_index INTEGER NOT NULL);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		
		if (subchannel.equals("RPGHorses")) {
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						short len = in.readShort();
						byte[] msgbytes = new byte[len];
						in.readFully(msgbytes);
						
						DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
						
						String typeStr = msgin.readUTF();
						DebugUtil.debug("RECEIVED " + typeStr);
						MessageType type = MessageType.valueOf(typeStr);
						
						if (type == MessageType.HORSE_REMOVE) {
							UUID owner = UUID.fromString(msgin.readUTF());
							int index = msgin.readInt();
							
							Player p = Bukkit.getPlayer(owner);
							if (p != null && p.isOnline()) {
								HorseOwner horseOwner = plugin.getHorseOwnerManager().getHorseOwner(p);
								horseOwner.removeRPGHorse(index);
								plugin.getStableGuiManager().setupStableGUI(horseOwner);
							} else {
								RPGHorse rpgHorse = getHorse(owner, index);
								MarketHorse marketHorse = plugin.getMarketGUIManager().getMarketHorse(rpgHorse);
								if (marketHorse != null) {
									plugin.getMarketGUIManager().removeHorse(marketHorse, true);
								} else {
									plugin.getMarketGUIManager().removeHorse(new MarketHorse(-1, rpgHorse, -1, index), true);
								}
							}
						} else {
							
							int id = msgin.readInt();
							UUID owner = UUID.fromString(msgin.readUTF());
							int index = msgin.readInt();
							
							HorseOwner horseOwner = null;
							RPGHorse rpgHorse;
							Player p = Bukkit.getPlayer(owner);
							if (p != null && p.isOnline()) {
								horseOwner = plugin.getHorseOwnerManager().getHorseOwner(p);
								rpgHorse = horseOwner.getRPGHorse(index);
								DebugUtil.debug("GOT PLAYERS HORSE");
							} else {
								rpgHorse = getHorse(owner, index);
							}
							
							if (type == MessageType.MARKET_HORSE_ADD) {
								double price = msgin.readDouble();
								plugin.getMarketGUIManager().addHorse(rpgHorse, price, index);
							} else if (type == MessageType.MARKET_HORSE_REMOVE) {
								boolean horseRemoved = msgin.readBoolean();
								MarketHorse marketHorse = plugin.getMarketGUIManager().getMarketHorse(rpgHorse);
								DebugUtil.debug("GOT MARKET HORSE");
								plugin.getMarketGUIManager().removeHorse(marketHorse, horseRemoved);
								DebugUtil.debug("REMOVED HORSE FROM MARKET");
								
								if (horseRemoved && p != null && p.isOnline()) {
									DebugUtil.debug("REMOVING P HORSE (" + index + ")");
									
									horseOwner.removeRPGHorse(index, false);
									plugin.getStableGuiManager().setupStableGUI(horseOwner);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.runTaskAsynchronously(plugin);
		}
	}
	
	public List<MarketHorse> loadMarket() {
		List<MarketHorse> marketHorses = new ArrayList<>();
		
		try (Connection con = getConnection()) {

			PreparedStatement statement = con.prepareStatement(LOAD_MARKET);
			
			ResultSet set = statement.executeQuery();
			
			while (set.next()) {
				int id = set.getInt("id");
				UUID owner = UUID.fromString(set.getString("owner"));
				double price = set.getDouble("price");
				int index = set.getInt("horse_index");
				
				RPGHorse rpgHorse;
				Player p = Bukkit.getPlayer(owner);
				if (p != null) {
					rpgHorse = plugin.getHorseOwnerManager().getHorseOwner(p).getRPGHorse(index);
				} else {
					rpgHorse = getHorse(owner, index);
				}
				
				marketHorses.add(new MarketHorse(id, rpgHorse, price, index));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return marketHorses;
	}
	
	public void addMarketHorse(MarketHorse horse) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection con = getConnection()) {

					String owner = horse.getRPGHorse().getHorseOwner().getUUID().toString();
					
					PreparedStatement statement = con.prepareStatement(ADD_MARKET_HORSE);
					statement.setInt(1, horse.getId());
					statement.setString(2, owner);
					statement.setDouble(3, horse.getPrice());
					statement.setInt(4, horse.getIndex());
					
					statement.executeUpdate();
					
					Player p = horse.getRPGHorse().getHorseOwner().getPlayer();
					if (p == null || p.isOnline()) {
						statement = con.prepareStatement(UPDATE_MARKET_HORSE);
						statement.setBoolean(1, true);
						statement.setString(2, p.getUniqueId().toString());
						statement.setInt(3, horse.getRPGHorse().getHorseOwner().getHorseNumber(horse.getRPGHorse()) - 1);
						statement.executeUpdate();
					}
					
					// Send a message to the other servers for them to update
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					
					out.writeUTF("Forward"); // So BungeeCord knows to forward it
					out.writeUTF("ALL");
					out.writeUTF("RPGHorses"); // The channel name to check if this your data
					
					ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
					DataOutputStream msgout = new DataOutputStream(msgbytes);
					try {
						msgout.writeUTF(MessageType.MARKET_HORSE_ADD.name());
						msgout.writeInt(horse.getId());
						msgout.writeUTF(owner);
						msgout.writeInt(horse.getIndex());
						msgout.writeDouble(horse.getPrice());
					} catch (IOException exception) {
						exception.printStackTrace();
					}
					
					out.writeShort(msgbytes.toByteArray().length);
					out.write(msgbytes.toByteArray());
					
					Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
					player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}
	
	public void removeMarketHorse(MarketHorse marketHorse, boolean removedHorse) {
		String ownerStr = marketHorse.getRPGHorse().getHorseOwner().getUUID().toString();
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection con = getConnection()) {

					int id = marketHorse.getId();
					DebugUtil.debug("REMOVE MARKET HORSE: " + ownerStr);
					
					PreparedStatement statement = con.prepareStatement(REMOVE_MARKET_HORSE);
					statement.setInt(1, id);
					statement.executeUpdate();
					
					statement = con.prepareStatement(LOWER_MARKET_HORSE_IDS);
					statement.setInt(1, id);
					statement.executeUpdate();
					
					if (removedHorse) {
						Player p = Bukkit.getPlayer(UUID.fromString(ownerStr));
						if (p == null || !p.isOnline()) {
							statement = con.prepareStatement(DELETE_HORSE);
							statement.setString(1, ownerStr);
							statement.setInt(2, marketHorse.getIndex());
							statement.executeUpdate();
							
							statement = con.prepareStatement(LOWER_HORSE_IDS);
							statement.setString(1, ownerStr);
							statement.setInt(2, marketHorse.getIndex());
							statement.executeUpdate();
						}
						
						statement = con.prepareStatement(LOWER_MARKET_HORSE_INDEXES);
						statement.setString(1, ownerStr);
						statement.setInt(2, marketHorse.getIndex());
						statement.executeUpdate();
					} else {
						Player p = marketHorse.getRPGHorse().getHorseOwner().getPlayer();
						if (p == null || p.isOnline()) {
							statement = con.prepareStatement(UPDATE_MARKET_HORSE);
							statement.setBoolean(1, false);
							statement.setString(2, ownerStr);
							statement.setInt(3, marketHorse.getIndex());
							statement.executeUpdate();
						}
					}
					
					// TODO: Plugin messaging
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					
					out.writeUTF("Forward"); // So BungeeCord knows to forward it
					out.writeUTF("ALL");
					out.writeUTF("RPGHorses"); // The channel name to check if this your data
					
					ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
					DataOutputStream msgout = new DataOutputStream(msgbytes);
					try {
						msgout.writeUTF(MessageType.MARKET_HORSE_REMOVE.name());
						msgout.writeInt(id);
						msgout.writeUTF(ownerStr);
						msgout.writeInt(marketHorse.getIndex());
						msgout.writeBoolean(removedHorse);
					} catch (IOException exception) {
						exception.printStackTrace();
					}
					
					out.writeShort(msgbytes.toByteArray().length);
					out.write(msgbytes.toByteArray());
					
					Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
					player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}
	
	public void updateHorse(RPGHorse horse) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection con = getConnection()) {
					PreparedStatement statement = con.prepareStatement(DELETE_HORSE);
				} catch (SQLException e) {
				
				}
			}
		}.runTaskAsynchronously(plugin);
	}
	
	public void removeHorse(RPGHorse horse) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (Connection con = getConnection()) {

					PreparedStatement statement = con.prepareStatement(DELETE_HORSE);
					statement.setString(1, horse.getHorseOwner().getUUID().toString());
					statement.setInt(2, horse.getIndex());
					statement.executeUpdate();
					
					statement = con.prepareStatement(LOWER_HORSE_IDS);
					statement.setString(1, horse.getHorseOwner().getUUID().toString());
					statement.setInt(2, horse.getIndex());
					statement.executeUpdate();
					
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					
					out.writeUTF("Forward"); // So BungeeCord knows to forward it
					out.writeUTF("ALL");
					out.writeUTF("RPGHorses"); // The channel name to check if this your data
					
					ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
					DataOutputStream msgout = new DataOutputStream(msgbytes);
					try {
						msgout.writeUTF(MessageType.HORSE_REMOVE.name());
						msgout.writeUTF(horse.getHorseOwner().getUUID().toString());
						msgout.writeInt(horse.getIndex());
					} catch (IOException exception) {
						exception.printStackTrace();
					}
					
					out.writeShort(msgbytes.toByteArray().length);
					out.write(msgbytes.toByteArray());
					
					Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
					player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}
	
	public RPGHorse getHorse(UUID uuid, int index) {
		try (Connection con = getConnection()) {

			PreparedStatement statement = con.prepareStatement(GET_HORSE);
			statement.setString(1, uuid.toString());
			statement.setInt(2, index);
			
			ResultSet set = statement.executeQuery();
			
			HorseOwner horseOwner = new HorseOwner(uuid);
			
			DebugUtil.debug("LOOKING FOR HORSE (" + index + ") " + uuid.toString());
			if (set.next()) {
				DebugUtil.debug("FOUND HORSE");
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
				
				rpgHorse.setIndex(index);
				return rpgHorse;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		DebugUtil.debug("  FAILED TO FIND HORSE IN MYSQL");
		
		return null;
	}
	
	public HorseOwner loadPlayer(UUID uuid) {
		String uuidStr = uuid.toString();
		
		HorseOwner horseOwner = new HorseOwner(uuid);
		
		try (Connection con = getConnection()) {
			
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
				if (plugin.getConfig().getBoolean("mysql.save-items")) {
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
				
				horseOwner.addRPGHorse(rpgHorse, false);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return horseOwner;
	}
	
	public void savePlayer(HorseOwner owner) {
		if (plugin.isEnabled() && Bukkit.isPrimaryThread()) {
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
	
	public void addHorse(RPGHorse horse) {
		if (plugin.isEnabled() && Bukkit.isPrimaryThread()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					saveHorse(horse);
				}
			}.runTaskAsynchronously(plugin);
		} else {
			saveHorse(horse);
		}
	}

	public void addHorse(RPGHorse horse, Connection con) {
		if (plugin.isEnabled() && Bukkit.isPrimaryThread()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					saveHorse(horse, con);
				}
			}.runTaskAsynchronously(plugin);
		} else {
			saveHorse(horse, con);
		}
	}

	public void saveHorse(RPGHorse horse)
	{
		try (Connection con = getConnection())
		{
			saveHorse(horse, con);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void saveHorse(RPGHorse horse, Connection con)
	{
		try
		{
			HorseOwner owner    = horse.getHorseOwner();
			String     ownerStr = owner.getUUID().toString();
			int        index    = horse.getIndex();

			PreparedStatement statement = con.prepareStatement(SAVE_HORSE);
			statement.setInt(1, index);
			statement.setString(2, ownerStr);
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
			if (plugin.getVersion().getWeight() < 11)
			{
				variant = ((LegacyHorseInfo) horse.getHorseInfo()).getVariant().name();
			}
			statement.setString(12, variant);

			Long deathTime = horse.getDeathTime();
			statement.setLong(13, deathTime == null ? 0L : deathTime);

			statement.setBoolean(14, horse.isInMarket());

			String particle = "";
			if (plugin.getVersion().getWeight() < 9)
			{
				Effect effect = ((LegacyHorseInfo) horse.getHorseInfo()).getEffect();
				if (effect != null)
				{
					particle = effect.name();
				}
			} else if (horse.getParticle() != null)
			{
				particle = horse.getParticle().name();
			}
			statement.setString(15, particle);

			String itemString = "";

			if (horse.getHorse() != null && horse.getHorse().isValid()) horse.loadItems();
			HashMap<Integer, ItemStack> items = horse.getItems();

			if (plugin.getConfig().getBoolean("mysql.save-items"))
			{
				int maxSlot = -1;
				for (Integer slot : items.keySet())
				{
					if (slot > maxSlot) maxSlot = slot;
				}

				if (maxSlot > -1)
				{
					ItemStack[] itemArray = new ItemStack[maxSlot + 1];
					for (Integer slot : items.keySet())
					{
						itemArray[slot] = items.get(slot);
					}

					itemString = BukkitSerialization.itemStackArrayToBase64(itemArray);
				}
			} else
			{

				FileConfiguration config = playerConfigs.getConfig(owner.getUUID());
				config.createSection("rpghorses." + (index + 1) + ".items");
				for (Integer slot : items.keySet())
				{
					config.set("rpghorses." + (index + 1) + ".items." + slot, items.get(slot));
				}
				playerConfigs.saveConfig(owner.getUUID());
				playerConfigs.reloadConfig(owner.getUUID());
			}


			statement.setString(16, itemString);

			statement.executeUpdate();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private void save(HorseOwner owner) {
		String uuidStr = owner.getUUID().toString();
		
		try (Connection con = getConnection()) {

			PreparedStatement statement = con.prepareStatement(SAVE_PLAYER);
			statement.setString(1, uuidStr);
			statement.setBoolean(2, owner.hasReceivedDefaultHorse());
			statement.setBoolean(3, owner.autoMountOn());
			statement.executeUpdate();
			
			statement = con.prepareStatement(DELETE_HORSES);
			statement.setString(1, uuidStr);
			statement.executeUpdate();
			
			for (RPGHorse horse : owner.getRPGHorses()) {
				addHorse(horse, con);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
