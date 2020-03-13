package org.plugins.rpghorses.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.plugins.rpghorses.configs.CustomConfig;

/**
 * Created by Rory on 7/8/2017.
 */
public class CustomConfigUtil {

    public static void loadConfig(CustomConfig customConfig) {
        customConfig.getConfig().options().copyDefaults(true);
        customConfig.saveDefaultConfig();
        customConfig.reloadConfig();
    }

    public static void loadDefaultConfig(JavaPlugin plugin) {
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

}
