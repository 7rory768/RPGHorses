package org.plugins.rpghorses;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/*
 * @author Rory Skipper (Roree) on 2024-08-25
 */
public abstract class AbstractWorldGuard {

	public static String getRPGHorsesPVPFlagName() {
		return "rpghorses-pvp";
	}

	public static String getRPGHorsesFlagName() {
		return "rpghorses";
	}

	public abstract List<String> getRegions(Location l);

	public abstract boolean isFlagAllowed(Player player, Location location, String flagName, boolean orNone);

	public abstract boolean isFlagDenied(Player player, Location location, String flagName, boolean orNone);

	public abstract void createFlags();
}
