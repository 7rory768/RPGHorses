package org.plugins.rpghorses.horses;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.players.HorseOwner;
import roryslibrary.util.MessagingUtil;

import java.util.HashMap;

public class RPGHorse {
	
	private HorseOwner horseOwner;
	private String name;
	private LivingEntity horse;
	private int tier = 1;
	private double xp = 0, health = 0, maxHealth = 20, movementSpeed = 1.0, jumpStrength = 1.0;
	private AbstractHorseInfo horseInfo;
	private HashMap<Integer, ItemStack> items = new HashMap<>();
	private boolean dead, inMarket, gainedXP;
	private Long deathTime;
	private Particle particle;
	@Getter @Setter
	private int index = -1;
	@Getter @Setter
	private Location lastLocation;
	@Getter @Setter
	private long lastMoveTime;
	
	public RPGHorse(HorseOwner horseOwner, int tier, double xp, String name, double health, double movementSpeed, double jumpStrength, AbstractHorseInfo horseInfo, boolean inMarket, Particle particle) {
		this.horseOwner = horseOwner;
		this.horseInfo = horseInfo;
		this.setName(name);
		this.setTier(tier);
		this.setXp(xp);
		this.setMovementSpeed(movementSpeed);
		this.setJumpStrength(jumpStrength);
		this.setHealth(health);
		this.setInMarket(inMarket);
		this.setParticle(particle);
		this.items.put(0, new ItemStack(Material.SADDLE));
		setItems(this.items);
	}
	
	public RPGHorse(HorseOwner horseOwner, int tier, double xp, String name, double health, double movementSpeed, double jumpStrength, AbstractHorseInfo horseInfo, boolean inMarket, Particle particle, HashMap<Integer, ItemStack> items) {
		this(horseOwner, tier, xp, name, health, movementSpeed, jumpStrength, horseInfo, inMarket, particle);
		if (items != null) {
			this.setItems(items);
		}
	}
	
	public RPGHorse(HorseOwner horseOwner, LivingEntity entity, String name) {
		this.horseOwner = horseOwner;
		this.horseInfo = AbstractHorseInfo.getFromEntity(entity);
		this.horse = entity;
		if (RPGHorsesMain.getVersion().getWeight() >= 9) {
			this.maxHealth = this.horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
			this.movementSpeed = this.horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
		} else {
			this.movementSpeed = this.horse.getMaxHealth();
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
	
	public LivingEntity getHorse() {
		return horse;
	}
	
	public int getTier() {
		return tier;
	}
	
	public void setTier(int tier) {
		if (tier > 0 && this.tier != tier) {
			this.tier = tier;
			setXp(0);
			gainedXP = false;
		}
	}
	
	public double getXp() {
		return xp;
	}
	
	public void setXp(double xp) {
		this.xp = xp;
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
			
			if (this.health != this.maxHealth) {
				this.setMaxHealth(health);
			}
		} else {
			this.setDead(true);
			this.refreshDeathTime();
		}
	}
	
	public double getMaxHealth() {
		return maxHealth;
	}
	
	public void setMaxHealth(double maxHealth) {
		if (maxHealth > 0) {
			this.maxHealth = maxHealth;
		}
	}
	
	public double getMovementSpeed() {
		return this.movementSpeed;
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
	
	public double getJumpStrength() {
		return this.jumpStrength;
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
	
	public AbstractHorseInfo getHorseInfo() {
		return horseInfo;
	}
	
	public Horse.Style getStyle() {
		return this.horseInfo.getStyle();
	}
	
	public void setStyle(Horse.Style style) {
		this.horseInfo.setStyle(style);
	}
	
	public HashMap<Integer, ItemStack> getItems() {
		return items;
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
	
	public boolean isDead() {
		return dead;
	}
	
	public void setDead(boolean dead) {
		if (this.dead == true && dead == false) {
			this.setHealth(this.maxHealth);
			if (this.horse != null) {
				this.horse.setHealth(this.health);
			}
		}
		this.dead = dead;
	}
	
	public boolean hasGainedXP() {
		return gainedXP;
	}
	
	public void setGainedXP(boolean gainedXP) {
		this.gainedXP = gainedXP;
	}
	
	public Long getDeathTime() {
		return deathTime;
	}
	
	public void setDeathTime(Long deathTime) {
		this.deathTime = deathTime;
	}
	
	public void refreshDeathTime() {
		this.deathTime = System.currentTimeMillis();
	}
	
	public boolean isInMarket() {
		return inMarket;
	}
	
	public void setInMarket(boolean inMarket) {
		this.inMarket = inMarket;
	}
	
	public Particle getParticle() {
		return this.particle;
	}
	
	public void setParticle(Particle particle) {
		this.particle = particle;
	}
	
	public void spawnEntity() {
		Player p = horseOwner.getPlayer();
		if (p != null && p.isOnline()) {
			Location horseLoc = p.getLocation();
			
			if (this.horse != null) {
				horseLoc = this.horse.getLocation();
				this.despawnEntity();
			}
			
			this.horseOwner.setSpawningHorse(true);
			
			this.horse = (LivingEntity) horseLoc.getWorld().spawnEntity(horseLoc, horseInfo.getEntityType());
			if (RPGHorsesMain.getVersion().getWeight() < 11) {
				this.horse.setMaxHealth(health);
				Horse horse = (Horse) this.horse;
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
			this.horse.setHealth(horse.getMaxHealth());
			
			this.horse.setRemoveWhenFarAway(false);
			
			if (RPGHorsesMain.getNMS() != null) {
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
			
			if (horseOwner.autoMountOn()) {
				if (RPGHorsesMain.getVersion().getWeight() < 11) {
					horse.setPassenger(p);
				} else {
					horse.addPassenger(p);
				}
			}
		}
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
		if (this.horse != null) {
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
	
	public void loadHealth() {
		if (this.horse != null) {
			this.health = this.horse.getHealth();
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
	
	public HorseOwner getHorseOwner() {
		return horseOwner;
	}
	
	public void setHorseOwner(HorseOwner horseOwner) {
		this.horseOwner = horseOwner;
	}
	
}
