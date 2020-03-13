package org.plugins.rpghorses.horses;

import com.google.common.collect.Sets;
import net.minecraft.server.v1_12_R1.EntityCreature;
import net.minecraft.server.v1_12_R1.PathfinderGoalSelector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.MessagingUtil;

import java.lang.reflect.Field;
import java.util.HashMap;

public class RPGHorse {

    private HorseOwner horseOwner;
    private String name;
    private AbstractHorse horse;
    private int tier = 1;
    private double xp = 0, health = 0, maxHealth = 20, movementSpeed = 1.0, jumpStrength = 1.0;
    private EntityType entityType = EntityType.HORSE;
    private Horse.Color color = Horse.Color.BROWN;
    private Horse.Style style = Horse.Style.NONE;
    private HashMap<Integer, ItemStack> items = new HashMap<>();
    private boolean dead, inMarket, gainedXP;
    private Long deathTime;
    private Particle particle;

    public RPGHorse(HorseOwner horseOwner, int tier, double xp, String name, double health, double movementSpeed, double jumpStrength, EntityType entityType, Horse.Color color, Horse.Style style, boolean inMarket, Particle particle) {
        this.horseOwner = horseOwner;
        this.setName(name);
        this.setTier(tier);
        this.setXp(xp);
        this.setMovementSpeed(movementSpeed);
        this.setJumpStrength(jumpStrength);
        this.setHealth(health);
        this.setEntityType(entityType);
        this.setColor(color);
        this.setStyle(style);
        this.setInMarket(inMarket);
        this.setParticle(particle);
        items.put(0, new ItemStack(Material.SADDLE));
        setItems(items);
    }

    public RPGHorse(HorseOwner horseOwner, int tier, double xp, String name, double health, double movementSpeed, double jumpStrength, EntityType entityType, Horse.Color color, Horse.Style style, boolean inMarket, Particle particle, HashMap<Integer, ItemStack> items) {
        this(horseOwner, tier, xp, name, health, movementSpeed, jumpStrength, entityType, color, style, inMarket, particle);
        this.setItems(items);
    }

    public void setHorseOwner(HorseOwner horseOwner) {
        this.horseOwner = horseOwner;
    }

    public HorseOwner getHorseOwner() {
        return horseOwner;
    }

    public AbstractHorse getHorse() {
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
    
    public void increaseXp(double xp) {
        this.xp += xp;
        gainedXP = true;
    }
    
    public void setXp(double xp) {
        this.xp = xp;
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
                this.horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(this.movementSpeed);
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
                this.horse.setJumpStrength(jumpStrength);
            }
        }
    }

    public EntityType getEntityType() {
        return this.entityType;
    }

    public void setEntityType(EntityType entityType) {
        if (this.entityType != null) {
            this.entityType = entityType;
        }
    }

    public Horse.Color getColor() {
        return this.color;
    }

    public void setColor(Horse.Color color) {
        this.color = color;
    }

    public Horse.Style getStyle() {
        return this.style;
    }

    public void setStyle(Horse.Style style) {
        this.style = style;
    }

    public HashMap<Integer, ItemStack> getItems() {
        return items;
    }

    public void setItems(HashMap<Integer, ItemStack> items) {
        this.items = items;
        if (this.horse != null) {
            Inventory inventory = this.horse.getInventory();
            for (Integer slot : items.keySet()) {
                inventory.setItem(slot, items.get(slot));
            }
        }
    }

    public void loadItems() {
        if (this.horse != null) {
            Inventory inventory = this.horse.getInventory();
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

    public void refreshDeathTime() {
        this.deathTime = System.currentTimeMillis();
    }

    public void setDeathTime(Long deathTime) {
        this.deathTime = deathTime;
    }

    public boolean isInMarket() {
        return inMarket;
    }

    public void setInMarket(boolean inMarket) {
        this.inMarket = inMarket;
    }

    public void setParticle(Particle particle) {
        this.particle = particle;
    }

    public Particle getParticle() {
        return this.particle;
    }

    public void spawnEntity() {
        Player p = Bukkit.getPlayer(horseOwner.getUUID());
        if (p != null && p.isOnline()) {
            Location horseLoc = p.getLocation();

            if (this.horse != null) {
                horseLoc = this.horse.getLocation();
                this.despawnEntity();
            }

            this.horseOwner.setSpawningHorse(true);
            this.horse = (AbstractHorse) horseLoc.getWorld().spawnEntity(horseLoc, entityType);
            
            this.horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(this.maxHealth);
            this.horse.setHealth(health);

            this.horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(this.movementSpeed);

            this.horse.setJumpStrength(this.jumpStrength);

            if (this.name != null) {
                this.horse.setCustomName(MessagingUtil.format("&7" + this.name));
                this.horse.setCustomNameVisible(true);
            }

            this.horse.setAdult();
            this.horse.setAgeLock(true);
            this.horse.setOwner(p);
            this.horse.setRemoveWhenFarAway(false);

            this.removeWanderingBehavior(this.horse);

            if (this.entityType == EntityType.HORSE) {
                Horse horse = (Horse) this.horse;
                horse.setColor(this.color);
                horse.setStyle(style);
            } else if (this.entityType == EntityType.DONKEY) {
                ChestedHorse chestedHorse = (ChestedHorse) this.horse;
                chestedHorse.setCarryingChest(true);
            }

            Inventory inventory = this.horse.getInventory();
            for (Integer slot : this.items.keySet()) {
                inventory.setItem(slot, this.items.get(slot));
            }
        }
    }

    public void despawnEntity() {
        if (this.horse != null) {
            this.loadItems();
            this.loadHealth();
            this.horse.remove();
            this.horse = null;
        }
    }

    public void removeWanderingBehavior(LivingEntity entity) {
        EntityCreature creature = (EntityCreature) (((CraftEntity) entity).getHandle());

        try {
            Field b = PathfinderGoalSelector.class.getDeclaredField("b");
            b.setAccessible(true);
            b.set(creature.goalSelector, Sets.newLinkedHashSet());
            b.set(creature.targetSelector, Sets.newLinkedHashSet());
            
            Field c = PathfinderGoalSelector.class.getDeclaredField("c");
            c.setAccessible(true);
            c.set(creature.goalSelector, Sets.newLinkedHashSet());
            c.set(creature.targetSelector, Sets.newLinkedHashSet());
        } catch (Exception e) {
            e.printStackTrace();
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
        return this.tier == rpgHorse.tier && this.health == rpgHorse.health && this.maxHealth == rpgHorse.maxHealth && this.movementSpeed == rpgHorse.movementSpeed && this.jumpStrength == rpgHorse.jumpStrength && this.entityType == rpgHorse.entityType && this.style == rpgHorse.style && this.color == rpgHorse.color && this.horseOwner.getUUID().equals(rpgHorse.getHorseOwner().getUUID());
    }

}
