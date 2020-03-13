package org.plugins.rpghorses.configs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

/**
 * Created by Rory on 6/18/2017.
 */
public class CustomConfig {

    private final JavaPlugin plugin;
    private final String fileName;

    private FileConfiguration config = null;
    private File file = null;

    public CustomConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void reloadConfig() {
        if (this.file == null) {
            this.file = new File(this.plugin.getDataFolder(), this.fileName + ".yml");
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);

        // Look for defaults in the jar
        if (this.plugin != null) {
            try {
                InputStream inputStream = this.plugin.getResource(this.fileName + ".yml");
                if (inputStream != null) {
                    Reader defConfigStream = new InputStreamReader(inputStream, "UTF8");
                    if (defConfigStream != null) {
                        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                        this.config.setDefaults(defConfig);
                        this.config.options().copyDefaults(true);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getConfig() {
        if (this.config == null) {
            reloadConfig();
        }
        return this.config;
    }

    public void saveConfig() {
        if (this.config == null || this.file == null) {
            return;
        }
        try {
            getConfig().save(this.file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void saveDefaultConfig() {
        if (this.file == null) {
            this.file = new File(this.plugin.getDataFolder(), this.fileName + ".yml");
        }
        if (!this.file.exists()) {
            this.plugin.saveResource(this.fileName + ".yml", false);
        }
    }

}