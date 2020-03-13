package org.plugins.rpghorses.configs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Rory on 6/18/2017.
 */
public class PlayerConfigs {

    private JavaPlugin plugin;

    private HashMap<UUID, File> playerFiles = new HashMap<>();
    private HashMap<UUID, FileConfiguration> playerConfigs = new HashMap<>();

    public PlayerConfigs(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadConfig(Player p) {
        this.reloadConfig(p.getUniqueId());
    }

    public void reloadConfig(UUID uuid) {
        if (!this.playerFiles.containsKey(uuid)) {
            File pathFile = new File(this.plugin.getDataFolder().getPath() + File.separator + "playerdata");
            pathFile.mkdir();
            File file = new File(pathFile, uuid.toString() + ".yml");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.playerFiles.put(uuid, file);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(this.playerFiles.get(uuid));
        this.playerConfigs.put(uuid, config);
    }

    public FileConfiguration getConfig(Player p) {
        return this.getConfig(p.getUniqueId());
    }

    public FileConfiguration getConfig(UUID uuid) {
        if (!this.playerConfigs.containsKey(uuid)) {
            reloadConfig(uuid);
        }
        return this.playerConfigs.get(uuid);
    }

    public HashMap<UUID, FileConfiguration> getConfigs() {
        HashMap<UUID, FileConfiguration> configs = new HashMap<>();
        for (File file : (new File(this.plugin.getDataFolder().getPath() + File.separator + "playerdata")).listFiles()) {
            String fileName = file.getName();
            UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
            this.playerFiles.put(uuid, file);
            configs.put(uuid, YamlConfiguration.loadConfiguration(file));
        }
        return configs;
    }

    public void saveConfig(Player p) {
        this.saveConfig(p.getUniqueId());
    }

    public void saveConfig(UUID uuid) {
        if (!this.playerConfigs.containsKey(uuid) || !this.playerFiles.containsKey(uuid)) {
            return;
        }
        try {
            getConfig(uuid).save(this.playerFiles.get(uuid));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteConfig(UUID uuid) {
        if (this.playerFiles.containsKey(uuid)) {
            this.playerFiles.get(uuid).delete();
        }
    }

}