package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.StableGUIManager;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.players.HorseRenamer;
import org.plugins.rpghorses.utils.MessagingUtil;

public class PlayerChatListener implements Listener {

    private final RPGHorsesMain plugin;
    private final RPGHorseManager rpgHorseManager;
    private final StableGUIManager stableGuiManager;
    private final MessagingUtil messagingUtil;

    public PlayerChatListener(RPGHorsesMain plugin, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager, MessagingUtil messagingUtil) {
        this.plugin = plugin;
        this.rpgHorseManager = rpgHorseManager;
        this.stableGuiManager = stableGuiManager;
        this.messagingUtil = messagingUtil;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        HorseRenamer horseRenamer = this.rpgHorseManager.getHorseRenamer(p);
        if (horseRenamer != null) {
            e.setCancelled(true);
            RPGHorse rpgHorse = horseRenamer.getRPGHorse();
            HorseOwner horseOwner = horseRenamer.getHorseOwner();
            int horseNumber = horseOwner.getHorseNumber(rpgHorse);
            String name = e.getMessage().split("\\s")[0];
            rpgHorse.setName(name);
            this.stableGuiManager.updateRPGHorse(rpgHorse);
            this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-renamed").replace("{HORSE-NUMBER}", "" + horseNumber), rpgHorse);
            this.rpgHorseManager.removeHorseRenamer(p.getUniqueId());
        }
    }
}
