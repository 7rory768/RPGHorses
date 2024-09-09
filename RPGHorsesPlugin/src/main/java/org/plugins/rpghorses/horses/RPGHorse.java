package org.plugins.rpghorses.horses;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.events.RPGHorseDespawnEvent;
import org.plugins.rpghorses.events.RPGHorsePostSpawnEvent;
import org.plugins.rpghorses.events.RPGHorsePreSpawnEvent;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.players.HorseOwner;
import roryslibrary.util.MessagingUtil;
import roryslibrary.util.Version;

import java.util.HashMap;

@Getter
@Setter
public class RPGHorse {

	private HorseOwner horseOwner;
	private String sourceCrate;
	private String name;
	private LivingEntity horse;
	private int tier = 1;
	private double xp = 0, health = 0, maxHealth = 20, movementSpeed = 1.0, jumpStrength = 1.0;
	private AbstractHorseInfo horseInfo;
	private HashMap<Integer, ItemStack> items = new HashMap<>();
	private boolean dead, inMarket, gainedXP;
	private Long deathTime;
	private Particle particle;

	private int index = -1;
	private Location lastLocation;
	private long lastMoveTime;

	public RPGHorse(HorseOwner horseOwner, String sourceCrate, int tier, double xp, String name, double health, double maxHealth, double movementSpeed, double jumpStrength, AbstractHorseInfo horseInfo, boolean inMarket, Particle particle) {
		this.horseOwner = horseOwner;
		this.sourceCrate = sourceCrate;
		this.horseInfo = horseInfo;
		this.setName(name);
		this.setTier(tier);
		this.setXp(xp);
		this.setMovementSpeed(movementSpeed);
		this.setJumpStrength(jumpStrength);
		this.setHealth(health);
		this.setMaxHealth(Math.max(health, maxHealth));
		this.setInMarket(inMarket);
		this.setParticle(particle);
		this.items.put(0, new ItemStack(Material.SADDLE));
		setItems(this.items);
	}

	public RPGHorse(HorseOwner horseOwner, String sourceCrate, int tier, double xp, String name, double health, double maxHealth, double movementSpeed, double jumpStrength, AbstractHorseInfo horseInfo, boolean inMarket, Particle particle, HashMap<Integer, ItemStack> items) {
		this(horseOwner, sourceCrate, tier, xp, name, health, maxHealth, movementSpeed, jumpStrength, horseInfo, inMarket, particle);
		if (items != null) {
			this.setItems(items);
		}
	}

	public RPGHorse(HorseOwner horseOwner, LivingEntity entity, String name) {
		this.horseOwner = horseOwner;
		this.sourceCrate = null;
		this.horseInfo = AbstractHorseInfo.getFromEntity(entity);
		this.horse = entity;
		if (RPGHorsesMain.getVersion().getWeight() >= 9) {
			this.maxHealth = this.horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
			this.movementSpeed = this.horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
		} else {
			this.maxHealth = this.horse.getMaxHealth();
		}
		this.setName(name);
		this.setTier(1);
		if (RPGHorsesMain.getVersion().getWeight() >= 11) {
			this.setJumpStrength(((AbstractHorse) this.horse).getJumpStrength());
		} else {
			this.setJumpStrength(((Horse) this.horse).getJumpStrength());
		}
		this.setHealth(entity.getHealth());
		this.setInMarket(false);
		this.setParticle(null);
		Inventory inventory = ((InventoryHolder) this.horse).getInventory();

		ItemStack saddleSlotItem = inventory.getItem(0);
		if (saddleSlotItem == null) {
			inventory.setItem(0, new ItemStack(Material.SADDLE));
		} else if (saddleSlotItem.getType() != Material.SADDLE) {
			horse.getWorld().dropItemNaturally(horse.getLocation(), saddleSlotItem);
			inventory.setItem(0, new ItemStack(Material.SADDLE));
		}

		for (int slot = 0; slot < inventory.getSize(); slot++) {
			ItemStack item = inventory.getItem(slot);
			if (item != null) {
				items.put(slot, item);
			}
		}
	}

	public void setTier(int tier) {
		if (tier > 0 && this.tier != tier) {
			this.tier = tier;
			setXp(0);
			gainedXP = false;
		}
	}

	public void increaseXp(double xp) {
		this.xp += xp;
		gainedXP = true;
	}

	public String getName() {
		if (name == null) {
			return "";
		}
		return name;
	}

	public void setName(String name) {
		if (name != null) {
			this.name = name;
			if (this.horse != null) {
				this.horse.setCustomName(MessagingUtil.format("&7" + this.name));
				this.horse.setCustomNameVisible(true);
			}
		}
	}

	public double getHealth() {
		loadHealth();
		return this.health;
	}

	public void setHealth(double health) {
		if (health > 0) {
			this.health = health;

			if (this.horse != null && this.horse.isValid())
				this.horse.setHealth(health);

			if (this.health > this.maxHealth) {
				this.setMaxHealth(health);
			}
		} else {
			this.setDead(true);
		}
	}

	public void loadHealth() {
		if (this.horse != null) {
			this.health = this.horse.getHealth();
		}
	}

	public void setMaxHealth(double maxHealth) {
		if (maxHealth > 0) {
			this.maxHealth = maxHealth;
		}
	}

	public void setMovementSpeed(double newSpeed) {
		if (newSpeed > 0) {
			this.movementSpeed = newSpeed;
			if (this.horse != null) {
				if (RPGHorsesMain.getVersion().getWeight() >= 9) {
					this.horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(this.movementSpeed);
				}
			}
		}
	}

	public void setJumpStrength(double jumpStrength) {
		if (jumpStrength > 0) {
			this.jumpStrength = jumpStrength;
			if (this.horse != null) {
				if (RPGHorsesMain.getVersion().getWeight() >= 11) {
					((AbstractHorse) this.horse).setJumpStrength(jumpStrength);
				} else {
					((Horse) this.horse).setJumpStrength(jumpStrength);
				}
			}
		}
	}

	public Horse.Style getStyle() {
		return this.horseInfo.getStyle();
	}

	public void setStyle(Horse.Style style) {
		this.horseInfo.setStyle(style);
	}

	public void setItems(HashMap<Integer, ItemStack> items) {
		this.items = items;
		if (this.horse != null) {
			Inventory inventory = ((InventoryHolder) this.horse).getInventory();
			for (Integer slot : items.keySet()) {
				inventory.setItem(slot, items.get(slot));
			}
		}
	}

	public void setDead(boolean dead) {
		if (this.dead && !dead) {
			this.setHealth(this.maxHealth);
			this.setDeathTime(null);

			if (this.horse != null)
				this.horse.setHealth(this.health);
		}

		if (dead && !this.dead) this.refreshDeathTime();
		this.dead = dead;
	}

	public boolean hasGainedXP() {
		return gainedXP;
	}

	public void refreshDeathTime() {
		this.deathTime = System.currentTimeMillis();
	}

	public boolean spawnEntity() {
		Player p = horseOwner.getPlayer();
		if (p != null && p.isOnline()) {
			Location horseLoc = p.getLocation();

			if (this.horse != null) {
				horseLoc = this.horse.getLocation();
				this.despawnEntity();
			}

			RPGHorsePreSpawnEvent preSpawnEvent = new RPGHorsePreSpawnEvent(p, this, horseLoc);
			Bukkit.getPluginManager().callEvent(preSpawnEvent);
			if (preSpawnEvent.isCancelled()) return false;

			this.horseOwner.setSpawningHorse(true);

			this.horse = (LivingEntity) horseLoc.getWorld().spawnEntity(horseLoc, horseInfo.getEntityType());
			horse.setMetadata("RPGHorse-HorseOwner", new FixedMetadataValue(RPGHorsesMain.getInstance(), horseOwner.getUUID().toString()));
			horseOwner.setLastHorseLocation(horse.getLocation());

			if (RPGHorsesMain.getVersion().getWeight() < 11) {
				Horse horse = (Horse) this.horse;
				horse.setMaxHealth(maxHealth);
				horse.setAdult();
				horse.setAgeLock(true);
				horse.setOwner(p);
			} else {
				AbstractHorse abstractHorse = (AbstractHorse) horse;
				abstractHorse.setJumpStrength(this.jumpStrength);
				abstractHorse.setAdult();
				abstractHorse.setAgeLock(true);
				abstractHorse.setOwner(p);
			}

			if (RPGHorsesMain.getVersion().getWeight() >= 9) {
				this.horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(this.maxHealth);
				this.horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(this.movementSpeed);
			}

			if (this.name != null) {
				this.horse.setCustomName(MessagingUtil.format("&7" + this.name));
				this.horse.setCustomNameVisible(true);
			}

			this.horse.setHealth(this.health);

			this.horse.setRemoveWhenFarAway(false);

			if (RPGHorsesMain.getNMS() != null && !RPGHorsesMain.getInstance().getConfig().getBoolean("horse-options.allow-wandering", false)) {
				RPGHorsesMain.getNMS().removeBehaviour(horse);
			}

			if (this.horseInfo.getEntityType() == EntityType.HORSE) {
				Horse horse = (Horse) this.horse;
				horse.setColor(this.horseInfo.getColor());
				horse.setStyle(this.horseInfo.getStyle());
				if (RPGHorsesMain.getVersion().getWeight() < 11) {
					horse.setVariant(((LegacyHorseInfo) horseInfo).getVariant());
				}
			} else if (this.horseInfo.getEntityType() == EntityType.DONKEY) {
				ChestedHorse chestedHorse = (ChestedHorse) this.horse;
				chestedHorse.setCarryingChest(true);
			} else if (this.getEntityType() == EntityType.LLAMA) {
				Llama llama = (Llama) horse;
				Llama.Color color = Llama.Color.WHITE;
				try {
					color = Llama.Color.valueOf(getColor().name());
				} catch (IllegalArgumentException e) {
				}
				llama.setColor(color);
			}

			setItems(items);

			Inventory inventory = ((InventoryHolder) horse).getInventory();
			ItemStack saddleSlotItem = inventory.getItem(0);
			if (saddleSlotItem == null) {
				inventory.setItem(0, new ItemStack(Material.SADDLE));
			} else if (saddleSlotItem.getType() != Material.SADDLE) {
				horse.getWorld().dropItemNaturally(horse.getLocation(), saddleSlotItem);
				inventory.setItem(0, new ItemStack(Material.SADDLE));
			}

			if (Version.isRunningMinimum(Version.v1_14))
				horse.setPersistent(false);

			horseOwner.setLastHorseLocation(p.getLocation());

			if (horseOwner.autoMountOn()) {
				if (RPGHorsesMain.getVersion().getWeight() < 11) {
					horse.setPassenger(p);
				} else {
					horse.addPassenger(p);
				}
			}

			Bukkit.getPluginManager().callEvent(new RPGHorsePostSpawnEvent(p, this));
			return true;
		}

		return false;
	}

	public EntityType getEntityType() {
		return horseInfo.getEntityType();
	}

	public void setEntityType(EntityType entityType) {
		horseInfo.setEntityType(entityType);
	}

	public Horse.Color getColor() {
		return horseInfo.getColor();
	}

	public void setColor(Horse.Color color) {
		this.horseInfo.setColor(color);
	}

	public void despawnEntity() {
		if (!Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTask(RPGHorsesMain.getInstance(), this::despawnEntity);
			return;
		}

		if (this.horse != null) {
			RPGHorseDespawnEvent despawnEvent = new RPGHorseDespawnEvent(horseOwner.getPlayer(), horse);
			Bukkit.getPluginManager().callEvent(despawnEvent);

			horseOwner.setLastHorseLocation(null);

			this.loadItems();
			this.loadHealth();
			this.horse.remove();
			this.horse = null;
		}
	}

	public void loadItems() {
		if (this.horse != null) {
			this.items.clear();
			Inventory inventory = ((InventoryHolder) this.horse).getInventory();
			for (int slot = 0; slot < inventory.getSize(); slot++) {
				this.items.put(slot, inventory.getItem(slot));
			}
		}
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof RPGHorse)) {
			return false;
		}
		return this.equals((RPGHorse) object);
	}

	public boolean equals(RPGHorse rpgHorse) {
        /*Bukkit.getLogger().info("tier equals? " + (this.tier == rpgHorse.tier));
        Bukkit.getLogger().info("health equals? " + (this.health == rpgHorse.health));
        Bukkit.getLogger().info("maxHealth equals? " + (this.maxHealth == rpgHorse.maxHealth));
        Bukkit.getLogger().info("movementSpeed equals? " + (this.movementSpeed == rpgHorse.movementSpeed));
        Bukkit.getLogger().info("jumpStrength equals? " + (this.jumpStrength == rpgHorse.jumpStrength));
        Bukkit.getLogger().info("entityType equals? " + (this.entityType == rpgHorse.entityType));
        Bukkit.getLogger().info("style equals? " + (this.style == rpgHorse.style));
        Bukkit.getLogger().info("color equals? " + (this.color == rpgHorse.color));
        Bukkit.getLogger().info("horseOwner uuid equals? " + (this.horseOwner.getUUID().equals(rpgHorse.getHorseOwner().getUUID())));*/
		return this.tier == rpgHorse.tier && this.health == rpgHorse.health && this.maxHealth == rpgHorse.maxHealth && this.movementSpeed == rpgHorse.movementSpeed && this.jumpStrength == rpgHorse.jumpStrength && this.horseInfo.equals(rpgHorse.horseInfo) && this.horseOwner.getUUID().equals(rpgHorse.getHorseOwner().getUUID());
	}

}
