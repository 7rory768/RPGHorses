package org.plugins.rpghorses.horses;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.plugins.rpghorses.players.HorseOwner;

import java.util.Random;

public class HorseCrate {

    private double price;
    private String name;
    private double minHealth, maxHealth, minMovementSpeed, maxMovementSpeed, minJumpStrength, maxJumpStrength;
    private EntityType entityType;
    private Horse.Color color;
    private Horse.Style style;
    private int tier;

    public HorseCrate(String name, double price, double minHealth, double maxHealth, double minMovementSpeed, double maxMovementSpeed, double minJumpStrength, double maxJumpStrength, EntityType entityType, Horse.Color color, Horse.Style style, int tier) {
        this.name = name;
        this.price = price;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
        this.minMovementSpeed = minMovementSpeed;
        this.maxMovementSpeed = maxMovementSpeed;
        this.minJumpStrength = minJumpStrength;
        this.maxJumpStrength = maxJumpStrength;
        this.entityType = entityType;
        this.color = color;
        this.style = style;
        this.tier = tier;
    }

    public RPGHorse getRPGHorse(HorseOwner horseOwner) {
        double health = new Random().nextDouble() * (this.maxHealth - this.minHealth) + this.minHealth;
        double movementSpeed = new Random().nextDouble() * (this.maxMovementSpeed - this.minMovementSpeed) + this.minMovementSpeed;
        double jumpStrength = new Random().nextDouble() * (this.maxJumpStrength - this.minJumpStrength) + this.minJumpStrength;
        return new RPGHorse(horseOwner, this.tier, 0, null, health, movementSpeed, jumpStrength, this.entityType, this.color, this.style, false, null);
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }
}
