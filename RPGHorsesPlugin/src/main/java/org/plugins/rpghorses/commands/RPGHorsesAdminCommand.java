package org.plugins.rpghorses.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.HorseCrate;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.*;
import org.plugins.rpghorses.managers.gui.MarketGUIManager;
import org.plugins.rpghorses.managers.gui.StableGUIManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import rorys.library.util.*;

import java.util.UUID;

public class RPGHorsesAdminCommand implements CommandExecutor {

	private final RPGHorsesMain     plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final RPGHorseManager   rpgHorseManager;
	private final StableGUIManager  stableGuiManager;
	private final HorseDespawner    horseDespawner;
	private final HorseCrateManager horseCrateManager;
	private final MarketGUIManager  marketGUIManager;
	private final ParticleManager   particleManager;
	private final MessageQueuer     messageQueuer;
	private final RPGMessagingUtil  messagingUtil;

	public RPGHorsesAdminCommand(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager, StableGUIManager stableGuiManager, HorseDespawner horseDespawner, HorseCrateManager horseCrateManager, MarketGUIManager marketGUIManager, ParticleManager particleManager, MessageQueuer messageQueuer, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.rpgHorseManager = rpgHorseManager;
		this.stableGuiManager = stableGuiManager;
		this.horseDespawner = horseDespawner;
		this.horseCrateManager = horseCrateManager;
		this.marketGUIManager = marketGUIManager;
		this.particleManager = particleManager;
		this.messageQueuer = messageQueuer;
		this.messagingUtil = messagingUtil;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (args.length > 0) {
			String arg1 = args[0];

			if (arg1.equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("rpghorses.reload")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				this.plugin.reloadConfig();
				this.messagingUtil.reload();
				this.stableGuiManager.reload();
				this.horseDespawner.reloadIdleTime();
				TimeUtil.refreshUnitStrings(this.plugin.getConfig(), "time-options.");
				this.horseCrateManager.loadHorseCrates();
				this.particleManager.reload();
				this.messagingUtil.sendMessageAtPath(sender, "messages.config-reloaded");
				return true;
			}

			if (arg1.equalsIgnoreCase("give")) {
				if (!sender.hasPermission("rpghorses.give")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				if (args.length < 3) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " give <horse-crate> <player>");
					return false;
				}

				OfflinePlayer p;
				RPGHorse      rpgHorse;
				if (args.length == 3) {
					HorseCrate horseCrate = horseCrateManager.getHorseCrate(args[1]);
					if (horseCrate == null) {
						this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid crate name: &6" + args[1]);
						this.messagingUtil.sendMessage(sender, "{PREFIX}Valid crate names: &6" + this.horseCrateManager.getHorseCrateList());
						return false;
					}

					String playerArg = args[1];
					p = this.runPlayerCheck(sender, playerArg);
					if (p == null) {
						return false;
					}

					if (this.horseOwnerManager.getHorseCount(p) >= this.horseOwnerManager.getHorseLimit(p)) {
						String message = this.plugin.getConfig().getString("messages.horse-limit").replace("{PLAYER}", sender.getName());
						if (p.isOnline()) {
							this.messagingUtil.sendMessage(p.getPlayer(), message);
						} else {
							this.messageQueuer.queueMessage(p, message);
						}
						return false;
					}

					HorseOwner horseOwner = horseOwnerManager.getHorseOwner(p);
					rpgHorse = horseCrate.getRPGHorse(horseOwner);
				} else {
					if (args.length < 6) {
						this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " give <health> <movement-speed> <jump-strength> <type> [color] [style] <player>");
						return false;
					} else if (args.length > 8) {
						this.messagingUtil.sendMessage(sender, "{PREFIX}Too many arguments, try &6/" + label.toLowerCase() + " give <health> <movement-speed> <jump-strength> <type> [color] [style] <player>");
						return false;
					}

					String playerArg = args[args.length - 1];
					p = this.runPlayerCheck(sender, playerArg);
					if (p == null) {
						return false;
					}

					if (this.horseOwnerManager.getHorseCount(p) >= this.horseOwnerManager.getHorseLimit(p)) {
						String message = this.plugin.getConfig().getString("messages.horse-limit").replace("{PLAYER}", sender.getName());
						if (p.isOnline()) {
							this.messagingUtil.sendMessage(p.getPlayer(), message);
						} else {
							this.messageQueuer.queueMessage(p, message);
						}
						return false;
					}

					String healthArg        = args[1];
					String movementSpeedArg = args[2];
					String jumpStrengthArg  = args[3];

					String typeArg = args[4];

					if (!this.runTypeCheck(sender, typeArg)) {
						return false;
					}

					EntityType    type     = EntityType.HORSE;
					Horse.Variant variant  = null;
					String        typeName = "HORSE";
					if (plugin.getVersion().getWeight() >= 11) {
						type = EntityType.valueOf(typeArg.toUpperCase());
						typeName = type.name();
					} else {
						variant = Horse.Variant.valueOf(typeArg.toUpperCase());
						typeName = variant.name();
					}

					Horse.Color color = Horse.Color.BROWN;
					Horse.Style style = Horse.Style.NONE;
					if (typeName.equals("HORSE") || typeName.equals("LLAMA")) {
						if (args.length == 7) {
							if (!runColorCheck(null, args[5])) {
								if (!runStyleCheck(null, args[5])) {
									runColorCheck(sender, args[5]);
									return false;
								} else {
									style = Horse.Style.valueOf(args[5]);
								}
							} else {
								color = Horse.Color.valueOf(args[5]);
							}
						} else if (args.length == 8) {
							if (!runColorCheck(sender, args[5]) || !runStyleCheck(sender, args[6])) {
								return false;
							}
							color = Horse.Color.valueOf(args[5]);
							style = Horse.Style.valueOf(args[6]);
						}
					}

					if (!this.checkHorseArguments(sender, p, healthArg, movementSpeedArg, jumpStrengthArg, null, null, null, null)) {
						return false;
					}

					double health        = Double.valueOf(healthArg);
					double movementSpeed = Double.valueOf(movementSpeedArg);
					double jumpStrength  = Double.valueOf(jumpStrengthArg);

					HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);

					if (plugin.getVersion().getWeight() < 11) {
						rpgHorse = new RPGHorse(horseOwner, 1, 0, this.plugin.getConfig().getString("horse-options.default-name").replace("{PLAYER}", p.getName()), health, movementSpeed, jumpStrength, new LegacyHorseInfo(style, color, variant), false, null);
					} else {
						rpgHorse = new RPGHorse(horseOwner, 1, 0, this.plugin.getConfig().getString("horse-options.default-name").replace("{PLAYER}", p.getName()), health, movementSpeed, jumpStrength, new HorseInfo(type, style, color), false, null);
					}
				}

				HorseOwner horseOwner = horseOwnerManager.getHorseOwner(p);
				rpgHorse.setName(plugin.getConfig().getString("horse-options.default-name", "Horse").replace("{PLAYER}", horseOwner.getPlayer().getName()));
				horseOwner.addRPGHorse(rpgHorse);

				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-given").replace("{PLAYER}", p.getName()), rpgHorse);

				String message = this.messagingUtil.placeholders(this.plugin.getConfig().getString("messages.horse-received").replace("{PLAYER}", sender.getName()), rpgHorse);
				if (p.isOnline()) {
					this.messagingUtil.sendMessage(p.getPlayer(), message);
					this.stableGuiManager.setupStableGUI(horseOwner);
				} else {
					this.messageQueuer.queueMessage(p, message);
					this.horseOwnerManager.flushHorseOwner(horseOwner);
				}
				return true;

			}

/*            if (arg1.equalsIgnoreCase("set")) {
                if (!sender.hasPermission("rpghorses.set")) {
                    this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
                    return false;
                }

                if (args.length < 9) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " set <health> <movement-speed> <jump-strength> <type> <color> <style> <horse-number> <player>");
                    return false;
                }

                String playerArg = args[8];
                OfflinePlayer p = this.runPlayerCheck(sender, playerArg);
                if (p == null) {
                    return false;
                }

                String healthArg = args[1];
                String movementSpeedArg = args[2];
                String jumpStrengthArg = args[3];
                String typeArg = args[4];
                String colorArg = args[5];
                String styleArg = args[6];
                String horseNumberArg = args[7];

                if (!this.checkHorseArguments(sender, p, healthArg, movementSpeedArg, jumpStrengthArg, typeArg, colorArg, styleArg, horseNumberArg)) {
                    return false;
                }

                double health = Double.valueOf(healthArg);
                double movementSpeed = Double.valueOf(movementSpeedArg);
                double jumpStrength = Double.valueOf(jumpStrengthArg);
                int horseNumber = Integer.valueOf(horseNumberArg);

                EntityType type = EntityType.valueOf(typeArg.toUpperCase());
                Horse.Color color = Horse.Color.valueOf(colorArg.toUpperCase());
                Horse.Style style = Horse.Style.valueOf(styleArg.toUpperCase());

                HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
                RPGHorse rpgHorse = horseOwner.getRPGHorse(horseNumber - 1);
                rpgHorse.setHealth(health);
                rpgHorse.setMovementSpeed(movementSpeed);
                rpgHorse.setJumpStrength(jumpStrength);
                rpgHorse.setEntityType(type);
                rpgHorse.setColor(color);
                rpgHorse.setStyle(style);
                this.rpgHorseManager.updateHorse(rpgHorse);
                if (p.isOnline()) {
                    this.stableGuiManager.updateRPGHorse(rpgHorse);
                }
                this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-set").replace("{PLAYER}", p.getName()).replace("{HORSE-NUMBER}", horseNumberArg), rpgHorse);
                return true;
            }*/

			if (arg1.equalsIgnoreCase("remove")) {
				if (!sender.hasPermission("rpghorses.remove")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				if (args.length < 3) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " remove <horse-number> <player>");
					return false;
				}

				String        horseNumberArg = args[1];
				String        playerArg      = args[2];
				OfflinePlayer p              = this.runPlayerCheck(sender, playerArg);
				if (p == null) {
					return false;
				}

				if (!this.runHorseNumberCheck(sender, horseNumberArg, p)) {
					return false;
				}

				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
				RPGHorse   rpgHorse   = horseOwner.getRPGHorse(Integer.valueOf(horseNumberArg) - 1);
				if (horseOwner.getCurrentHorse().equals(horseOwner)) {
					for (String cmd : this.plugin.getConfig().getStringList("command-options.on-despawn")) {
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessagingUtil.format(cmd.replace("{PLAYER}", p.getName())));
					}
				}
				horseOwner.removeRPGHorse(rpgHorse);

				if (rpgHorse.isInMarket()) {
					this.marketGUIManager.removeHorse(this.marketGUIManager.getPage(rpgHorse), rpgHorse, true);
				}

				String message = this.messagingUtil.placeholders(this.plugin.getConfig().getString("messages.your-horse-was-removed").replace("{PLAYER}", sender.getName()).replace("{HORSE-NUMBER}", horseNumberArg), rpgHorse);
				if (p.isOnline()) {
					this.messagingUtil.sendMessage(p.getPlayer(), message);
					this.stableGuiManager.setupStableGUI(horseOwner);
				} else {
					this.horseOwnerManager.flushHorseOwner(horseOwner);
					this.messageQueuer.queueMessage(p, message);
				}

				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-removed").replace("{PLAYER}", sender.getName()).replace("{HORSE-NUMBER}", horseNumberArg), rpgHorse);
				return true;
			}

			if (arg1.equalsIgnoreCase("upgrade")) {
				if (!sender.hasPermission("rpghorses.upgrade")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				if (args.length < 3) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " upgrade <horse-number> <player>");
					return false;
				}

				String        horseNumberArg = args[1];
				String        playerArg      = args[2];
				OfflinePlayer p              = this.runPlayerCheck(sender, playerArg);
				if (p == null) {
					return false;
				}

				if (!this.runHorseNumberCheck(sender, horseNumberArg, p)) {
					return false;
				}

				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
				RPGHorse   rpgHorse   = horseOwner.getRPGHorse(Integer.valueOf(horseNumberArg) - 1);
				int        tier       = rpgHorse.getTier();
				if (tier >= this.rpgHorseManager.getMaxTier()) {
					this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.max-horse-tier").replace("{PLAYER}", p.getName()).replace("{HORSE-NUMBER}", horseNumberArg), rpgHorse);
					return false;
				}

				rpgHorse.setMovementSpeed(rpgHorse.getMovementSpeed() * this.plugin.getConfig().getDouble("horse-tiers." + tier + ".movement-speed-multiplier"));
				rpgHorse.setJumpStrength(rpgHorse.getJumpStrength() * this.plugin.getConfig().getDouble("horse-tiers." + tier + ".jump-strength-multiplier"));
				rpgHorse.setTier(++tier);

				String message = this.messagingUtil.placeholders(this.plugin.getConfig().getString("messages.your-horse-was-upgraded").replace("{PLAYER}", sender.getName()).replace("{HORSE-NUMBER}", horseNumberArg).replace("{TIER}", "" + tier), rpgHorse);
				if (p.isOnline()) {
					this.messagingUtil.sendMessage(p.getPlayer(), message);
					this.stableGuiManager.updateRPGHorse(rpgHorse);
				} else {
					this.horseOwnerManager.flushHorseOwner(horseOwner);
					this.messageQueuer.queueMessage(p, message);
				}

				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-upgraded").replace("{PLAYER}", p.getName()).replace("{HORSE-NUMBER}", horseNumberArg).replace("{TIER}", "" + tier), rpgHorse);
				return true;
			}

			if (arg1.equalsIgnoreCase("listall")) {
				if (!sender.hasPermission("rpghorses.listall")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				this.messagingUtil.sendMessageAtPath(sender, "listall-format.header");
				String body = this.plugin.getConfig().getString("listall-format.body");
				for (Player p : this.plugin.getServer().getOnlinePlayers()) {
					HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
					RPGHorse   rpgHorse   = horseOwner.getCurrentHorse();
					if (rpgHorse != null) {
						this.messagingUtil.sendMessage(sender, body.replace("{PLAYER}", p.getName()).replace("{HORSE-NUMBER}", "" + horseOwner.getHorseNumber(rpgHorse)).replace("{LOCATION}", LocationUtil.toBlockString(rpgHorse.getHorse().getLocation())), rpgHorse);
					}
				}
				this.messagingUtil.sendMessageAtPath(sender, "listall-format.footer");
				return true;
			}

			if (arg1.equalsIgnoreCase("check")) {
				if (!sender.hasPermission("rpghorses.check")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				if (args.length < 2) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " check <radius>");
					return false;
				}

				Player p;
				try {
					p = (Player) sender;
				} catch (ClassCastException e) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Location loc = p.getLocation();

				String radiusArg = args[1];
				if (!NumberUtil.isPositiveInt(radiusArg)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid radius value: &6" + radiusArg);
					return false;
				}
				int radius = Integer.valueOf(radiusArg);

				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("check-format.header").replace("{RADIUS}", radiusArg));
				String body = this.plugin.getConfig().getString("check-format.body");
				for (Player loopP : this.plugin.getServer().getOnlinePlayers()) {
					HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(loopP);
					RPGHorse   rpgHorse   = horseOwner.getCurrentHorse();
					if (rpgHorse != null && rpgHorse.getHorse().getLocation().distance(loc) <= radius) {
						this.messagingUtil.sendMessage(sender, body.replace("{PLAYER}", loopP.getName()).replace("{HORSE-NUMBER}", "" + horseOwner.getHorseNumber(rpgHorse)).replace("{LOCATION}", LocationUtil.toBlockString(rpgHorse.getHorse().getLocation())), rpgHorse);
					}
				}
				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("listall-format.footer").replace("{RADIUS}", radiusArg));
				return true;
			}

			if (arg1.equalsIgnoreCase("removenear")) {
				if (!sender.hasPermission("rpghorses.removenear")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				if (args.length < 2) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " removenear <radius>");
					return false;
				}

				Player p;
				try {
					p = (Player) sender;
				} catch (ClassCastException e) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Location loc = p.getLocation();

				String radiusArg = args[1];
				if (!NumberUtil.isPositiveInt(radiusArg)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid radius value: &6" + radiusArg);
					return false;
				}
				int radius = Integer.valueOf(radiusArg);

				int horseCount = 0;
				for (Player loopP : this.plugin.getServer().getOnlinePlayers()) {
					HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(loopP);
					RPGHorse   rpgHorse   = horseOwner.getCurrentHorse();
					if (rpgHorse != null && rpgHorse.getHorse().getLocation().distance(loc) <= radius) {
						horseOwner.setCurrentHorse(null);
						this.messagingUtil.sendMessage(loopP, this.plugin.getConfig().getString("messages.horse-sent-to-stable").replace("{PLAYER}", p.getName()), rpgHorse);
						horseCount++;
					}
				}
				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-removenear").replace("{HORSE-COUNT}", "" + horseCount));
				return true;
			}

			if (arg1.equalsIgnoreCase("purgeall")) {
				if (!sender.hasPermission("rpghorses.purgeall")) {
					this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
					return false;
				}

				if (args.length < 2) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label.toLowerCase() + " purgeall <radius>");
					return false;
				}

				Player p;
				try {
					p = (Player) sender;
				} catch (ClassCastException e) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
					return false;
				}
				Location loc = p.getLocation();

				String radiusArg = args[1];
				if (!NumberUtil.isPositiveInt(radiusArg)) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid radius value: &6" + radiusArg);
					return false;
				}
				int radius = Integer.valueOf(radiusArg);

				int horseCount = 0;
				for (Entity entity : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
					if (rpgHorseManager.isValidEntityType(entity.getType())) {
						RPGHorse rpgHorse = rpgHorseManager.getRPGHorse(entity);
						if (rpgHorse != null) {
							rpgHorse.despawnEntity();
						} else {
							horseCount++;
							entity.remove();
						}
					}
				}
				this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-purgeall").replace("{HORSE-COUNT}", "" + horseCount));
				return true;
			}
		}

		if (!sender.hasPermission("rpghorses.help")) {
			this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
			return false;
		}

		plugin.sendHelpMessage(sender, label);
		return true;
	}

	private OfflinePlayer runPlayerCheck(CommandSender sender, String playerArg) {
		OfflinePlayer p = Bukkit.getPlayer(playerArg);
		UUID          uuid;
		if (p == null) {
			uuid = SkinUtil.getUUIDFromName(playerArg);

			if (uuid == null) {
				this.messagingUtil.sendMessage(sender, "{PREFIX}&6" + playerArg + " &7has never played this server before");
				return null;
			}

			p = Bukkit.getOfflinePlayer(uuid);

			if (p == null || !p.hasPlayedBefore()) {
				this.messagingUtil.sendMessage(sender, "{PREFIX}&6" + playerArg + " &7has never played this server before");
				return null;
			}
		}
		return p;
	}

	private boolean checkHorseArguments(CommandSender sender, OfflinePlayer offlinePlayer, String healthArg, String movementSpeedArg, String jumpStrengthArg, String typeArg, String colorArg, String styleArg, String horseNumberArg) {

		if (!this.runHealthCheck(sender, healthArg)) {
			return false;
		}

		if (!this.runMovementSpeedCheck(sender, movementSpeedArg)) {
			return false;
		}

		if (!this.runJumpStrengthCheck(sender, jumpStrengthArg)) {
			return false;
		}

		if (!this.runTypeCheck(sender, typeArg)) {
			return false;
		}

		if (!this.runColorCheck(sender, colorArg)) {
			return false;
		}

		if (!this.runStyleCheck(sender, styleArg)) {
			return false;
		}

		if (!this.runHorseNumberCheck(sender, horseNumberArg, offlinePlayer)) {
			return false;
		}

		return true;
	}

	private boolean runHorseNumberCheck(CommandSender sender, String horseNumberArg, OfflinePlayer offlinePlayer) {
		if (horseNumberArg != null) {
			if (!NumberUtil.isPositiveInt(horseNumberArg) || Integer.valueOf(horseNumberArg) < 1) {
				if (sender != null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid horse-number value: &6" + horseNumberArg);
				}
				return false;
			}

			if (offlinePlayer != null) {
				int horseCount = this.horseOwnerManager.getHorseCount(offlinePlayer);
				if (horseCount < Integer.valueOf(horseNumberArg)) {
					if (sender != null) {
						this.messagingUtil.sendMessage(sender, "{PREFIX}&6" + offlinePlayer.getName() + " &7only has &6" + horseCount + " &7RPGHorses");
					}
					return false;
				}
			}
		}
		return true;
	}

	private boolean runHealthCheck(CommandSender sender, String healthArg) {
		if (healthArg != null) {
			if (!NumberUtil.isPositiveDouble(healthArg)) {
				if (sender != null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid health value: &6" + healthArg);
				}
				return false;
			}
		}
		return true;
	}

	private boolean runMovementSpeedCheck(CommandSender sender, String movementSpeedArg) {
		if (movementSpeedArg != null) {
			if (!NumberUtil.isPositiveDouble(movementSpeedArg)) {
				if (sender != null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid movement-speed value: &6" + movementSpeedArg);
				}
				return false;
			}
		}
		return true;
	}

	private boolean runJumpStrengthCheck(CommandSender sender, String jumpStrengthArg) {
		if (jumpStrengthArg != null) {
			if (!NumberUtil.isPositiveDouble(jumpStrengthArg)) {
				if (sender != null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid jump-strength value: &6" + jumpStrengthArg);
				}
				return false;
			}
		}
		return true;
	}

	private boolean runColorCheck(CommandSender sender, String colorArg) {
		if (colorArg != null) {
			if (!this.rpgHorseManager.isValidColor(colorArg)) {
				if (sender != null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid color value: &6" + colorArg);
					this.messagingUtil.sendMessage(sender, "{PREFIX}Valid colors: " + this.rpgHorseManager.getColorsList());
				}
				return false;
			}
		}
		return true;
	}

	private boolean runStyleCheck(CommandSender sender, String styleArg) {
		if (styleArg != null) {
			if (!this.rpgHorseManager.isValidStyle(styleArg)) {
				if (sender != null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid style value: &6" + styleArg);
					this.messagingUtil.sendMessage(sender, "{PREFIX}Valid styles: " + this.rpgHorseManager.getStylesList());
				}
				return false;
			}
		}
		return true;
	}

	private boolean runTypeCheck(CommandSender sender, String typeArg) {
		if (typeArg != null) {
			if (plugin.getVersion().getWeight() < 11) {
				if (!rpgHorseManager.isValidVariant(typeArg)) {
					if (sender != null) {
						this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid variant value: &6" + typeArg.toUpperCase());
						this.messagingUtil.sendMessage(sender, "{PREFIX}Valid variants: " + rpgHorseManager.getVariantTypesList());
					}
					return false;
				}
			} else if (!this.rpgHorseManager.isValidEntityType(typeArg)) {
				if (sender != null) {
					this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid type value: &6" + typeArg.toUpperCase());
					this.messagingUtil.sendMessage(sender, "{PREFIX}Valid types: " + this.rpgHorseManager.getEntityTypesList());
				}
				return false;
			}
		}
		return true;
	}

}
