package org.plugins.rpghorses.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.HorseCrate;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.*;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.MessagingUtil;
import org.plugins.rpghorses.utils.NumberUtil;

public class RPGHorsesCommand implements CommandExecutor {

    private final RPGHorsesMain plugin;
    private final HorseOwnerManager horseOwnerManager;
    private final StableGUIManager stableGUIManager;
    private final MarketGUIManager marketGUIManager;
    private final HorseCrateManager horseCrateManager;
    private final ParticleManager particleManager;
    private final Economy economy;
    private final MessagingUtil messagingUtil;

    public RPGHorsesCommand(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, StableGUIManager stableGUIManager, MarketGUIManager marketGUIManager, HorseCrateManager horseCrateManager, ParticleManager particleManager, Economy economy, MessagingUtil messagingUtil) {
        this.plugin = plugin;
        this.horseOwnerManager = horseOwnerManager;
        this.stableGUIManager = stableGUIManager;
        this.marketGUIManager = marketGUIManager;
        this.horseCrateManager = horseCrateManager;
        this.particleManager = particleManager;
        this.economy = economy;
        this.messagingUtil = messagingUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0) {

            String arg1 = args[0];

            if (arg1.equalsIgnoreCase("sell")) {
                if (!sender.hasPermission("rpghorses.sell")) {
                    this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
                    return false;
                }

                if (!(sender instanceof Player)) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
                    return false;
                }
                Player p = (Player) sender;

                if (args.length < 3) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label + " sell <horse-number> <price>");
                    return false;
                }

                String horseNumberArg = args[1];
                if (!this.runHorseNumberCheck(sender, horseNumberArg, p)) {
                    return false;
                }
                int horseNumber = Integer.valueOf(horseNumberArg), index = horseNumber - 1;

                String priceArg = args[2];
                if (!NumberUtil.isPositiveDouble(priceArg)) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid price argument &6" + priceArg);
                    return false;
                }
                double price = Double.valueOf(priceArg);

                HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
                RPGHorse rpgHorse = horseOwner.getRPGHorse(index);
                if (rpgHorse.isInMarket()) {
                    this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-already-in-market").replace("{HORSE-NUMBER}", "" + horseNumber));
                    return false;
                } else if (rpgHorse.isDead() || horseOwner.getCurrentHorse() == rpgHorse) {
                    this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-market-fail").replace("{HORSE-NUMBER}", "" + horseNumber));
                    return false;
                }

                rpgHorse.setInMarket(true);
                this.marketGUIManager.addHorse(rpgHorse, price, horseNumber - 1);
                this.stableGUIManager.updateRPGHorse(rpgHorse);
                this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-added-to-market").replace("{HORSE-NUMBER}", "" + horseNumber));
                return true;
            }

            if (arg1.equalsIgnoreCase("buy")) {
                if (!sender.hasPermission("rpghorses.buy")) {
                    this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
                    return false;
                }

                if (args.length < 2) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label + " buy <horse-crate>");
                    return false;
                }

                if (!(sender instanceof Player)) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
                    return false;
                }
                Player p = (Player) sender;
                HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);

                int horseCount = this.horseOwnerManager.getHorseCount(p);
                if (horseCount >= this.horseOwnerManager.getHorseLimit(p)) {
                    this.messagingUtil.sendMessage(sender, this.plugin.getConfig().getString("messages.horse-limit").replace("{PLAYER}", "CONSOLE"));
                    return false;
                }

                HorseCrate horseCrate = this.horseCrateManager.getHorseCrate(args[1]);
                if (horseCrate == null) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid crate name: &6" + args[1]);
                    this.messagingUtil.sendMessage(sender, "{PREFIX}Valid crate names: &6" + this.horseCrateManager.getHorseCrateList());
                    return false;
                }

                if (this.economy.getBalance(p) < horseCrate.getPrice()) {
                    this.messagingUtil.sendMessageAtPath(p, "messages.cant-afford-crate");
                    return false;
                }

                this.economy.withdrawPlayer(p, horseCrate.getPrice());
                RPGHorse rpgHorse = horseCrate.getRPGHorse(horseOwner);
                horseOwner.addRPGHorse(rpgHorse);
                this.stableGUIManager.setupStableGUI(horseOwner);
                this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-crate-bought").replace("{HORSE-CRATE}", horseCrate.getName()).replace("{PRICE}", "" + horseCrate.getPrice()));
                return true;
            }

            if (arg1.equalsIgnoreCase("market")) {
                if (!sender.hasPermission("rpghorses.market")) {
                    this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
                    return false;
                }

                if (!(sender instanceof Player)) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
                    return false;
                }
                Player p = (Player) sender;

                HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
                horseOwner.openMarketGUIPage(this.marketGUIManager.getPage(1));
                return true;
            }

            if (arg1.equalsIgnoreCase("trail")) {

                if (!(sender instanceof Player)) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
                    return false;
                }
                Player p = (Player) sender;

                if (args.length < 2) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}Not enough arguments, try &6/" + label + " trail <particle>");
                    return false;
                }

                String particleArg = args[1];
                Particle particle;
                try {
                    particle = Particle.valueOf(particleArg);
                } catch (IllegalArgumentException e) {
                    this.messagingUtil.sendMessage(p, "{PREFIX}Invalid particle argument: " + particleArg);
                    this.messagingUtil.sendMessage(p, "{PREFIX}Valid particles: " + this.particleManager.getParticleList());
                    return false;
                }

                if (!p.hasPermission("rpghorses.trail." + particle.name().toLowerCase()) && !p.hasPermission("rpghorses.trail.*")) {
                    this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.no-permission-particles").replace("{PARTICLE}", particle.name()));
                    return false;
                }

                HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
                RPGHorse rpgHorse = horseOwner.getCurrentHorse();
                if (rpgHorse == null) {
                    this.messagingUtil.sendMessageAtPath(p, "messages.particle-fail");
                    return false;
                }

                rpgHorse.setParticle(particle);
                this.messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.particle-set").replace("{HORSE-NUMBER}", "" + horseOwner.getHorseNumber(rpgHorse)).replace("{PARTICLE}", particle.name()));
                return true;
            }

        }

        if (!sender.hasPermission("rpghorses.stable")) {
            this.messagingUtil.sendMessageAtPath(sender, "messages.no-permission");
            return false;
        }

        if (!(sender instanceof Player)) {
            this.messagingUtil.sendMessage(sender, "{PREFIX}This command can only be used in-game");
            return false;
        }
        Player p = (Player) sender;

        HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p.getUniqueId());
        if (horseOwner.getStableGUI() == null) {
            this.stableGUIManager.setupStableGUI(horseOwner);
        } else {
            for (RPGHorse rpgHorse : horseOwner.getRPGHorses()) {
                if (rpgHorse.hasGainedXP()) {
                    stableGUIManager.updateRPGHorse(rpgHorse);
                    rpgHorse.setGainedXP(false);
                }
            }
        }
        
        if (horseOwner.getRPGHorses().size() == 0) {
            this.messagingUtil.sendMessageAtPath(p, "messages.no-horses");
            return false;
        }
        
        horseOwner.openStableGUIPage(1);
        return true;
    }

    private boolean runHorseNumberCheck
            (CommandSender
                     sender, String
                     horseNumberArg, OfflinePlayer
                     offlinePlayer) {
        if (horseNumberArg != null) {
            if (!NumberUtil.isPositiveInt(horseNumberArg) || Integer.valueOf(horseNumberArg) < 1) {
                this.messagingUtil.sendMessage(sender, "{PREFIX}Invalid horse-number value: &6" + horseNumberArg);
                return false;
            }

            if (offlinePlayer != null) {
                int horseCount = this.horseOwnerManager.getHorseCount(offlinePlayer);
                if (horseCount < Integer.valueOf(horseNumberArg)) {
                    this.messagingUtil.sendMessage(sender, "{PREFIX}&6" + offlinePlayer.getName() + " &7only has &6" + horseCount + " &7RPGHorses");
                    return false;
                }
            }
        }
        return true;
    }
}
