package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.managers.MarketGUIManager;
import org.plugins.rpghorses.managers.StableGUIManager;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.MessageQueuer;
import org.plugins.rpghorses.players.HorseOwner;

public class PlayerJoinListener implements Listener {

    private final RPGHorsesMain plugin;
    private final HorseOwnerManager horseOwnerManager;
    private final StableGUIManager stableGuiManager;
    private final MarketGUIManager marketGUIManager;
    private final MessageQueuer messageQueuer;

    public PlayerJoinListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, StableGUIManager stableGuiManager, MarketGUIManager marketGUIManager, MessageQueuer messageQueuer) {
        this.plugin = plugin;
        this.horseOwnerManager = horseOwnerManager;
        this.stableGuiManager = stableGuiManager;
        this.marketGUIManager = marketGUIManager;
        this.messageQueuer = messageQueuer;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        HorseOwner horseOwner = this.horseOwnerManager.loadData(p);
        this.stableGuiManager.setupStableGUI(horseOwner);
        new BukkitRunnable() {
            @Override
            public void run() {
                marketGUIManager.setupYourHorsesGUI(horseOwner);
                messageQueuer.sendQueuedMessages(p);
            }
        }.runTaskLaterAsynchronously(this.plugin, 5L);
    }

}
