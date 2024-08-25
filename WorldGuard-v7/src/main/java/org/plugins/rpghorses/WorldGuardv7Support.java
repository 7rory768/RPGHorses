package org.plugins.rpghorses;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
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
public class WorldGuardv7Support extends AbstractWorldGuard {

	public List<String> getRegions(Location l) {
		if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
			return new ArrayList<>();
		}

		final ApplicableRegionSet regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(l.getWorld())).getApplicableRegions(BukkitAdapter.asBlockVector(l));

		final Iterator<ProtectedRegion> iter = regions.iterator();

		List<String> regionNames = new ArrayList<>();

		while (iter.hasNext()) {
			ProtectedRegion region = iter.next();

			regionNames.add(region.getId());
		}

		return regionNames;
	}

	public boolean isFlagAllowed(Player player, Location location, String flagName, boolean orNone) {
		LocalPlayer localPlayer = player == null ? null : WorldGuardPlugin.inst().wrapPlayer(player);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(flagName);

		if (flag instanceof StateFlag) {
			StateFlag.State def = ((StateFlag) flag).getDefault();
			StateFlag.State state = query.queryState(BukkitAdapter.adapt(location), localPlayer, (StateFlag) flag);

			if (state == StateFlag.State.ALLOW) return true;
			else if (state == StateFlag.State.DENY) return false;
			else return def == StateFlag.State.ALLOW || orNone;
		}

		return true;
	}

	public boolean isFlagDenied(Player player, Location location, String flagName, boolean orNone) {
		LocalPlayer localPlayer = player == null ? null : WorldGuardPlugin.inst().wrapPlayer(player);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(flagName);

		if (flag instanceof StateFlag) {
			StateFlag.State state = query.queryState(BukkitAdapter.adapt(location), localPlayer, (StateFlag) flag);

			if (state == StateFlag.State.DENY) return true;
			else if (state == StateFlag.State.ALLOW) return false;
			else return orNone;
		}

		return true;
	}

	public void createFlags() {
		try {
			StateFlag flag = new StateFlag(getRPGHorsesPVPFlagName(), true);
			WorldGuard.getInstance().getFlagRegistry().register(flag);
		} catch (FlagConflictException e) {
			// Already exists
		} catch (Throwable t) {
			t.printStackTrace();
		}

		try {
			StateFlag flag = new StateFlag(getRPGHorsesFlagName(), true);
			WorldGuard.getInstance().getFlagRegistry().register(flag);
		} catch (FlagConflictException e) {
			// Already exists
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
