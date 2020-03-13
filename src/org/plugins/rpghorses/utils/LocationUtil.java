package org.plugins.rpghorses.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Created by Rory on 6/25/2017.
 */
public class LocationUtil {

    public static Location fromString(String string) {
        string = string.replace(',', '.');
        String[] args = string.split("\\|");
        Location loc = new Location(Bukkit.getWorld(args[0]), Double.valueOf(args[1]), Double.valueOf(args[2]), Double.valueOf(args[3]));
        if (args.length > 4) {
            loc.setPitch(Float.valueOf(args[4]));
            loc.setPitch(Float.valueOf(args[5]));
        }
        return loc;
    }

    public static String toBlockString(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public static String toString(Location loc) {
        return (loc.getWorld().getName() + "|" + loc.getX() + "|" + loc.getY() + "|" + loc.getZ() + "|" + loc.getPitch() + "|" + loc.getYaw()).replace('.', ',');
    }
}
