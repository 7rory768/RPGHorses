package org.plugins.rpghorses.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.plugins.rpghorses.RPGHorsesMain;

public class PluginEnableListener implements Listener {

    private final RPGHorsesMain plugin;

    public PluginEnableListener(RPGHorsesMain plugin) {
        this.plugin = plugin;

        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPluginEnable(PluginEnableEvent e) {
        if (e.getPlugin().getName().equals("Vault")) {
            Bukkit.getLogger().info("[RPGHorses] Vault loading...");
            if (!this.plugin.getServer().getPluginManager().isPluginEnabled(this.plugin)) {
                this.plugin.getServer().getPluginManager().enablePlugin(this.plugin);
            }
        }
    }
}
