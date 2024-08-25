package org.plugins.rpghorses;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ManaWands - Developed by Lewes D. B. (Boomclaw).
 * All rights reserved 2019.
 */
public class WorldGuardv6Support extends AbstractWorldGuard {

	public List<String> getRegions(Location l) {
		if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
			return new ArrayList<>();
		}

		final ApplicableRegionSet regions = WorldGuardPlugin.inst().getRegionManager(l.getWorld()).getApplicableRegions(l);

		final Iterator<ProtectedRegion> iter = regions.iterator();

		List<String> regionNames = new ArrayList<>();

		while (iter.hasNext()) {
			ProtectedRegion region = iter.next();

			regionNames.add(region.getId());
		}

		return regionNames;
	}

	public boolean isFlagAllowed(Player player, Location location, String flagName, boolean orNone) {
		FlagRegistry registry = WorldGuardPlugin.inst().getFlagRegistry();
		Flag<?> flag = DefaultFlag.fuzzyMatchFlag(registry, flagName);

		if (flag instanceof StateFlag) {
			StateFlag.State def = ((StateFlag) flag).getDefault();

			StateFlag.State state = WGBukkit.getRegionManager(location.getWorld()).getApplicableRegions(location).queryState(null, (StateFlag) flag);

			if (state == StateFlag.State.ALLOW) return true;
			else if (state == StateFlag.State.DENY) return false;
			else return def == StateFlag.State.ALLOW || orNone;
		}

		return true;
	}

	public boolean isFlagDenied(Player player, Location location, String flagName, boolean orNone) {
		FlagRegistry registry = WorldGuardPlugin.inst().getFlagRegistry();
		Flag<?> flag = DefaultFlag.fuzzyMatchFlag(registry, flagName);

		if (flag instanceof StateFlag) {
			StateFlag.State state = WGBukkit.getRegionManager(location.getWorld()).getApplicableRegions(location).queryState(null, (StateFlag) flag);

			if (state == StateFlag.State.DENY) return true;
			else if (state == StateFlag.State.ALLOW) return false;
			else return orNone;
		}

		return false;
	}

	public void createFlags() {
		try {
			StateFlag flag = new StateFlag(getRPGHorsesPVPFlagName(), true);
			WorldGuardPlugin.inst().getFlagRegistry().register(flag);
		} catch (FlagConflictException e) {
			// Already exists
		} catch (Throwable t) {
			t.printStackTrace();
		}

		try {
			StateFlag flag = new StateFlag(getRPGHorsesFlagName(), true);
			WorldGuardPlugin.inst().getFlagRegistry().register(flag);
		} catch (FlagConflictException e) {
			// Already exists
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
