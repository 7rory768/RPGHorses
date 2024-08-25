package org.plugins.rpghorses.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.plugins.rpghorses.AbstractWorldGuard;
import org.plugins.rpghorses.WorldGuardv6Support;
import org.plugins.rpghorses.WorldGuardv7Support;

import java.util.Collections;
import java.util.List;

/*
 * @author Rory Skipper (Roree) on 2024-08-25
 */
public class WorldGuardUtil {

	private static AbstractWorldGuard worldGuard;

	public static AbstractWorldGuard getWorldGuard() {
		if (worldGuard == null) {
			try {
				Class.forName("com.sk89q.worldguard.WorldGuard");
				worldGuard = new WorldGuardv7Support();
			} catch (ClassNotFoundException ignored) {
				try {
					worldGuard = new WorldGuardv6Support();
				} catch (Exception ignored1) {
				}
			}
		}

		return worldGuard;
	}

	public static String getRPGHorsesPVPFlagName() {
		return "rpghorses-pvp";
	}

	public static String getRPGHorsesFlagName() {
		return "rpghorses";
	}

	public static boolean isEnabled() {
		return getWorldGuard() != null;
	}

	public static List<String> getRegions(Location l) {
		if (!isEnabled()) return Collections.emptyList();
		return getWorldGuard().getRegions(l);
	}

	public static boolean areRPGHorsesAllowed(Player player, Location location) {
		if (!isEnabled()) return true;
		return getWorldGuard().isFlagAllowed(player, location, getRPGHorsesFlagName(), true);
	}

	public static boolean isHorsePVPAllowed(Player player, Location location) {
		if (!isEnabled()) return true;
		return getWorldGuard().isFlagAllowed(player, location, AbstractWorldGuard.getRPGHorsesPVPFlagName(), true);
	}

	public static boolean isFlagAllowed(Player player, Location location, String flagName, boolean orNone) {
		if (!isEnabled()) return true;
		return getWorldGuard().isFlagAllowed(player, location, flagName, orNone);
	}

	public static boolean isFlagDenied(Player player, Location location, String flagName, boolean orNone) {
		if (!isEnabled()) return false;
		return !getWorldGuard().isFlagAllowed(player, location, flagName, orNone);
	}

	public static void createFlags() {
		if (!isEnabled()) return;
		getWorldGuard().createFlags();
	}
}
