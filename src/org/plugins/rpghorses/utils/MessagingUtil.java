package org.plugins.rpghorses.utils;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.plugins.rpghorses.horses.RPGHorse;

/**
 * Created by Rory on 6/22/2017.
 */
public class MessagingUtil {

    private final JavaPlugin plugin;
    private String prefix = "";
    private String finalPrefixFormatting = "";

    public MessagingUtil(JavaPlugin plugin) {
        this.plugin = plugin;
        this.updatePrefix();
    }

    public void updatePrefix() {
        this.prefix = this.plugin.getConfig().getString("prefix");
        this.updatePrefixFormatting();
    }

    private void updatePrefixFormatting() {
        String color = "", format = "";
        if (this.prefix.length() > 1) {
            for (int index = this.prefix.length(); index > 1; index--) {
                String bit = this.prefix.substring(index - 2, index);
                if (bit.startsWith("§")) {
                    int chNum = (int) bit.toLowerCase().charAt(1);
                    if ((97 <= chNum && chNum <= 102) || (48 <= chNum && chNum <= 57) || chNum == 114) {
                        color = bit;
                        break;
                    }
                    if (107 <= chNum && chNum <= 112) {
                        format += bit;
                    }

                }
            }
        }
        this.finalPrefixFormatting = color + format;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getPrefixFormatting() {
        return this.finalPrefixFormatting;
    }

    public void sendMessage(CommandSender sender, String msg) {
        sender.sendMessage(this.placeholders(msg));
    }

    public void sendMessage(CommandSender sender, String msg, RPGHorse rpgHorse) {
        sender.sendMessage(this.placeholders(msg, rpgHorse));
    }

    public void sendMessageAtPath(CommandSender sender, String path) {
        sender.sendMessage(this.placeholders(this.plugin.getConfig().getString(path)));
    }

    public void sendMessageAtPath(CommandSender sender, String path, RPGHorse rpgHorse) {
        sender.sendMessage(this.placeholders(this.plugin.getConfig().getString(path), rpgHorse));
    }

    public void broadcastMessage(String msg) {
        Bukkit.broadcastMessage(this.placeholders(msg));
    }

    public void broadcastMessageAtPath(String path) {
        Bukkit.broadcastMessage(this.placeholders(this.plugin.getConfig().getString(path)));
    }

    public String placeholders(String arg) {
        return StringEscapeUtils.unescapeJava(ChatColor.translateAlternateColorCodes('&', arg.replace("{PREFIX}", this.prefix)));
    }

    public String placeholders(String arg, RPGHorse rpgHorse) {
        return StringEscapeUtils.unescapeJava(ChatColor.translateAlternateColorCodes('&', arg.replace("{PREFIX}", this.prefix).replace("{HORSE-NAME}", rpgHorse.getName())));
    }

    public static String format(String arg) {
        return StringEscapeUtils.unescapeJava(ChatColor.translateAlternateColorCodes('&', arg));
    }

    public static String format(String arg, RPGHorse rpgHorse) {
        return StringEscapeUtils.unescapeJava(ChatColor.translateAlternateColorCodes('&', arg.replace("{HORSE-NAME}", rpgHorse.getName())));
    }
    
    public String getProgressBar(double progress, double maxProgress, int progressBarCount, String completedColor, String missingColor, String progressBarChar) {
        int completedProgress;
        if (maxProgress == 0) {
            completedProgress = progressBarCount;
        } else {
            completedProgress = (int) Math.floor((progress / maxProgress) * progressBarCount);
        }
        completedProgress = completedProgress > progressBarCount ? progressBarCount : completedProgress;
        int uncompletedProgress = progressBarCount - completedProgress;
        
        String progressBar = completedColor;
        for (int i = 0; i < completedProgress; i++) {
            progressBar += progressBarChar;
        }
        
        if (uncompletedProgress > 0) {
            progressBar += missingColor;
            for (int i = 0; i < uncompletedProgress; i++) {
                progressBar += progressBarChar;
            }
        }
        return MessagingUtil.format(progressBar);
    }
}
