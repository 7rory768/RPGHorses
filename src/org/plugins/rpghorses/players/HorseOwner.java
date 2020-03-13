package org.plugins.rpghorses.players;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.plugins.rpghorses.guis.*;
import org.plugins.rpghorses.horses.RPGHorse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HorseOwner {

    private UUID uuid;
    private List<RPGHorse> rpgHorses;
    private RPGHorse currentHorse;
    private boolean spawningHorse, mountingHorse, deMountingHorse, changingHorse, inStableInventory, inMarketInventory, inYourHorsesInventory;
    private Location lastHorseLocation;
    private StableGUI stableGUI;
    private StableGUIPage stableGUIPage;
    private MarketGUIPage marketGUIPage;
    private YourHorsesGUI yourHorsesGUI;
    private YourHorsesGUIPage yourHorsesGUIPage;

    public HorseOwner(Player p) {
        this(p.getUniqueId());
    }

    public HorseOwner(UUID uuid) {
        this(uuid, new ArrayList<>());
    }

    public HorseOwner(UUID uuid, List<RPGHorse> rpgHorses) {
        this.uuid = uuid;
        this.rpgHorses = rpgHorses;
    }

    public UUID getUUID() {
        return uuid;
    }
    
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public List<RPGHorse> getRPGHorses() {
        return this.rpgHorses;
    }

    public RPGHorse getRPGHorse(int index) {
        if (index >= 0 && index < this.rpgHorses.size()) {
            return this.rpgHorses.get(index);
        }
        return null;
    }

    public void addRPGHorse(RPGHorse rpgHorse) {
        this.rpgHorses.add(rpgHorse);
        rpgHorse.setHorseOwner(this);
    }

    public RPGHorse removeRPGHorse(int index) {
        if (index >= 0 && index < this.rpgHorses.size()) {
            RPGHorse rpgHorse = this.rpgHorses.get(index);
            this.removeRPGHorse(rpgHorse);
            return rpgHorse;
        }
        return null;
    }

    public void removeRPGHorse(RPGHorse rpgHorse) {
        if (rpgHorse == this.currentHorse) {
            this.setCurrentHorse(null);
        }
        this.rpgHorses.remove(rpgHorse);
    }

    public RPGHorse getCurrentHorse() {
        return currentHorse;
    }

    public void setCurrentHorse(RPGHorse rpgHorse) {
        if (this.currentHorse != rpgHorse) {
            if (this.currentHorse != null) {
                if (rpgHorse != null) {
                    AbstractHorse horse = this.currentHorse.getHorse();
                    if (horse != null) {
                        for (Entity entity : horse.getPassengers()) {
                            if (entity.getUniqueId() == this.uuid) {
                                this.setChangingHorse(true);
                            }
                        }
                    }
                }
                this.currentHorse.despawnEntity();
            }

            if (rpgHorse != null) {
                this.stableGUI.addGlow(rpgHorse);
            }

            this.currentHorse = rpgHorse;

            if (rpgHorse != null) {
                this.currentHorse.spawnEntity();
            }
        }
    }

    public int getHorseNumber(RPGHorse rpgHorse) {
        for (int i = 0; i < this.rpgHorses.size(); i++) {
            if (this.rpgHorses.get(i) == rpgHorse) {
                return i + 1;
            }
        }
        return -1;
    }

    public boolean isSpawningHorse() {
        return this.spawningHorse;
    }

    public void setSpawningHorse(boolean spawningHorse) {
        this.spawningHorse = spawningHorse;
    }

    public boolean isMountingHorse() {
        return mountingHorse;
    }

    public void setMountingHorse(boolean mountingHorse) {
        this.mountingHorse = mountingHorse;
    }

    public boolean isDeMountingHorse() {
        return deMountingHorse;
    }

    public void setDeMountingHorse(boolean deMountingHorse) {
        this.deMountingHorse = deMountingHorse;
    }

    public boolean isChangingHorse() {
        return changingHorse;
    }

    public void setChangingHorse(boolean changingHorse) {
        this.changingHorse = changingHorse;
    }

    public boolean isInStableInventory() {
        return inStableInventory;
    }

    public void setInStableInventory(boolean inStableInventory) {
        this.inStableInventory = inStableInventory;
        if (inStableInventory == false) {
            this.stableGUIPage = null;
        } else {
            this.inMarketInventory = false;
            this.marketGUIPage = null;
            this.inYourHorsesInventory = false;
            this.yourHorsesGUIPage = null;
        }
    }
    
    public Location getLastHorseLocation() {
        return lastHorseLocation;
    }
    
    public void setLastHorseLocation(Location lastHorseLocation) {
        this.lastHorseLocation = lastHorseLocation;
    }
    
    public void setStableGUI(StableGUI stableGUI) {
        this.stableGUI = stableGUI;
        if (this.inStableInventory && this.stableGUIPage != null) {
            int pageNum = this.stableGUIPage.getPageNum();
            while (pageNum > 0) {
                StableGUIPage stableGUIPage = stableGUI.getPage(pageNum--);
                if (stableGUIPage != null) {
                    this.openStableGUIPage(stableGUIPage);
                    break;
                }
            }
        }
    }

    public void openStableGUIPage(int pageNum) {
        StableGUIPage stableGUIPage = this.stableGUI.getPage(pageNum);
        this.openStableGUIPage(stableGUIPage);
    }

    public void openStableGUIPage(StableGUIPage stableGUIPage) {
        if (stableGUIPage != null) {
            Bukkit.getPlayer(this.uuid).openInventory(stableGUIPage.getGUI());
            this.stableGUIPage = stableGUIPage;
            this.setInStableInventory(true);
        }
    }

    public StableGUI getStableGUI() {
        return stableGUI;
    }

    public StableGUIPage getCurrentStableGUIPage() {
        return this.stableGUIPage;
    }

    public boolean isInYourHorsesInventory() {
        return inYourHorsesInventory;
    }

    public void setInYourHorsesInventory(boolean inYourHorsesInventory) {
        this.inYourHorsesInventory = inYourHorsesInventory;
        if (inYourHorsesInventory == false) {
            this.yourHorsesGUIPage = null;
        } else {
            this.inMarketInventory = false;
            this.marketGUIPage = null;
            this.inStableInventory = false;
            this.stableGUIPage = null;
        }
    }

    public void setYourHorsesGUI(YourHorsesGUI yourHorsesGUI) {
        this.yourHorsesGUI = yourHorsesGUI;
        if (this.inYourHorsesInventory) {
            int pageNum = this.yourHorsesGUIPage.getPageNum();
            while (pageNum > 0) {
                YourHorsesGUIPage yourHorsesGUIPage = yourHorsesGUI.getPage(pageNum--);
                if (yourHorsesGUIPage != null) {
                    this.openYourHorsesGUIPage(yourHorsesGUIPage);
                    break;
                }
            }
        }
    }

    public void openYourHorsesGUIPage(int pageNum) {
        YourHorsesGUIPage yourHorsesGUIPage = this.yourHorsesGUI.getPage(pageNum);
        this.openYourHorsesGUIPage(yourHorsesGUIPage);
    }

    public void openYourHorsesGUIPage(YourHorsesGUIPage yourHorsesGUIPage) {
        if (yourHorsesGUIPage != null) {
            Bukkit.getPlayer(this.uuid).openInventory(yourHorsesGUIPage.getGUI());
            this.yourHorsesGUIPage = yourHorsesGUIPage;
            this.setInYourHorsesInventory(true);
        }
    }

    public YourHorsesGUI getYourHorsesGUI() {
        return yourHorsesGUI;
    }

    public YourHorsesGUIPage getCurrentYourHorsesGUIPage() {
        return this.yourHorsesGUIPage;
    }

    public boolean isInMarketInventory() {
        return inMarketInventory;
    }

    public void setInMarketInventory(boolean inMarketInventory) {
        this.inMarketInventory = inMarketInventory;
        if (inMarketInventory == false) {
            this.marketGUIPage = null;
        } else {
            this.inStableInventory = false;
            this.stableGUIPage = null;
            this.inYourHorsesInventory = false;
            this.yourHorsesGUIPage = null;
        }
    }

    public void openMarketGUIPage(MarketGUIPage marketGUIPage) {
        if (marketGUIPage != null) {
            Bukkit.getPlayer(this.uuid).openInventory(marketGUIPage.getGUI());
            this.marketGUIPage = marketGUIPage;
            this.setInMarketInventory(true);
        }
    }

    public MarketGUIPage getCurrentMarketGUIPage() {
        return marketGUIPage;
    }
}
