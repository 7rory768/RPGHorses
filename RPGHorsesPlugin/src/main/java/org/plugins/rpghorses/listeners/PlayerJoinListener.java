package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.MessageQueuer;
import org.plugins.rpghorses.managers.gui.MarketGUIManager;
import org.plugins.rpghorses.managers.gui.StableGUIManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import rorys.library.util.UpdateNotifier;

public class PlayerJoinListener implements Listener {

    private final RPGHorsesMain plugin;
    private final HorseOwnerManager horseOwnerManager;
    private final StableGUIManager stableGuiManager;
    private final MarketGUIManager marketGUIManager;
    private final RPGMessagingUtil messagingUtil;
    private final MessageQueuer messageQueuer;
    private final UpdateNotifier updateNotifier;

    public PlayerJoinListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, StableGUIManager stableGuiManager, MarketGUIManager marketGUIManager, RPGMessagingUtil messagingUtil, MessageQueuer messageQueuer, UpdateNotifier updateNotifier) {
        this.plugin = plugin;
        this.horseOwnerManager = horseOwnerManager;
        this.stableGuiManager = stableGuiManager;
        this.marketGUIManager = marketGUIManager;
        this.messagingUtil = messagingUtil;
        this.messageQueuer = messageQueuer;
        this.updateNotifier = updateNotifier;

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
                if (p.isOp() && updateNotifier.needsUpdate()) {
                    p.sendMessage(updateNotifier.getUpdateMsg());
                }
            }
        }.runTaskLaterAsynchronously(this.plugin, 5L);
    }

}
