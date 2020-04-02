package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;

public class PlayerTeleportListener implements Listener {

    private final RPGHorsesMain     plugin;
    private final HorseOwnerManager horseOwnerManager;
    private       RPGMessagingUtil     messagingUtil;

    public PlayerTeleportListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGMessagingUtil messagingUtil) {
        this.plugin = plugin;
        this.horseOwnerManager = horseOwnerManager;
        this.messagingUtil = messagingUtil;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
        RPGHorse currentHorse = horseOwner.getCurrentHorse();

        if (currentHorse != null && !horseOwner.isMountingHorse() && !horseOwner.isDeMountingHorse() && !horseOwner.isChangingHorse()) {
            this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-sent-to-stable").replace("{PLAYER}", "CONSOLE"), currentHorse);
            horseOwner.setCurrentHorse(null);
        } else if (horseOwner.isMountingHorse()) {
            horseOwner.setMountingHorse(false);
        } else if (horseOwner.isDeMountingHorse()) {
            horseOwner.setDeMountingHorse(false);
        } else if (horseOwner.isChangingHorse()) {
            horseOwner.setChangingHorse(false);
        }
    }

}
