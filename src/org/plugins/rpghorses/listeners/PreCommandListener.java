package org.plugins.rpghorses.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.MarketGUIManager;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.managers.StableGUIManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.players.RemoveHorseConfirmation;
import org.plugins.rpghorses.utils.MessagingUtil;

public class PreCommandListener implements Listener {

    private final RPGHorsesMain plugin;
    private final RPGHorseManager rpgHorseManager;
    private final StableGUIManager stableGuiManager;
    private final MarketGUIManager marketGUIManager;
    private final MessagingUtil messagingUtil;

    public PreCommandListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager, MarketGUIManager marketGUIManager, MessagingUtil messagingUtil) {
        this.plugin = plugin;
        this.rpgHorseManager = rpgHorseManager;
        this.stableGuiManager = stableGuiManager;
        this.marketGUIManager = marketGUIManager;
        this.messagingUtil = messagingUtil;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void preCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (e.getMessage().toLowerCase().startsWith("/confirm")) {
            RemoveHorseConfirmation removeHorseConfirmation = this.rpgHorseManager.getRemovingHorseConfirmation(p);
            if (removeHorseConfirmation != null) {
                e.setCancelled(true);
                RPGHorse rpgHorse = removeHorseConfirmation.getRPGHorse();
                HorseOwner horseOwner = removeHorseConfirmation.getHorseOwner();
                int horseNumber = horseOwner.getHorseNumber(rpgHorse);
                RPGHorse currentHorse = horseOwner.getCurrentHorse();
                if (currentHorse != null && currentHorse.equals(rpgHorse)) {
                    for (String cmd : this.plugin.getConfig().getStringList("command-options.on-despawn")) {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
                    }
                }
                horseOwner.removeRPGHorse(rpgHorse);
                this.stableGuiManager.setupStableGUI(horseOwner);
                if (rpgHorse.isInMarket()) {
                    this.marketGUIManager.removeHorse(this.marketGUIManager.getPage(rpgHorse), rpgHorse, true);
                }
                this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.your-horse-was-removed").replace("{HORSE-NUMBER}", "" + horseNumber).replace("{PLAYER}", p.getName()), rpgHorse);
            }
        }
    }

}
