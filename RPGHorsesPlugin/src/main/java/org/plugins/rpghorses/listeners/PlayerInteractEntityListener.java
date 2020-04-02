package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.utils.RPGMessagingUtil;

public class PlayerInteractEntityListener implements Listener {

    private final RPGHorseManager  rpgHorseManager;
    private       RPGMessagingUtil   messagingUtil;

    public PlayerInteractEntityListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, RPGMessagingUtil messagingUtil) {
        this.rpgHorseManager = rpgHorseManager;
        this.messagingUtil = messagingUtil;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (this.rpgHorseManager.isValidEntityType(entity.getType())) {
            RPGHorse rpgHorse = this.rpgHorseManager.getRPGHorse(entity);
            if (rpgHorse != null) {
                Player p = e.getPlayer();
                if (!rpgHorse.getHorseOwner().getUUID().equals(p.getUniqueId())) {
                    e.setCancelled(true);
                    this.messagingUtil.sendMessageAtPath(p, "messages.not-your-horse", rpgHorse);
                }
            }
        }
    }

}
