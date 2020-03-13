package org.plugins.rpghorses.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.managers.StableGUIManager;
import org.plugins.rpghorses.managers.XPManager;
import org.plugins.rpghorses.players.HorseOwner;

public class EntityDeathListener implements Listener {

    private final RPGHorseManager rpgHorseManager;
    private final StableGUIManager stableGuiManager;
    private final XPManager xpManager;

    public EntityDeathListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager, XPManager xpManager) {
        this.rpgHorseManager = rpgHorseManager;
        this.stableGuiManager = stableGuiManager;
        this.xpManager = xpManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent e) {
        EntityType entityType = e.getEntityType();
        if (this.rpgHorseManager.isValidEntityType(entityType)) {
            RPGHorse rpgHorse = this.rpgHorseManager.getRPGHorse(e.getEntity());
            if (rpgHorse != null) {
                HorseOwner horseOwner = rpgHorse.getHorseOwner();
                rpgHorse.loadItems();
                horseOwner.setCurrentHorse(null);
                rpgHorse.setDead(true);
                rpgHorse.refreshDeathTime();
                e.getDrops().clear();
    
                this.stableGuiManager.updateRPGHorse(rpgHorse);
                xpManager.removeHorseOwner(horseOwner);
            }
        }
    }

}
