package org.plugins.rpghorses;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.plugins.rpghorses.commands.RPGHorsesAdminCommand;
import org.plugins.rpghorses.commands.RPGHorsesCommand;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.listeners.*;
import org.plugins.rpghorses.managers.*;
import org.plugins.rpghorses.managers.gui.*;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.ItemUtil;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.utils.WorldGuardUtil;
import org.plugins.rpghorses.version.Version;
import roryslibrary.configs.CustomConfig;
import roryslibrary.configs.PlayerConfigs;
import roryslibrary.util.CustomConfigUtil;
import roryslibrary.util.DebugUtil;
import roryslibrary.util.TimeUtil;
import roryslibrary.util.UpdateNotifier;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
TODO:
 - Change the trail skin-value to the trail skin-value the horse is currently using
 - Change it so admins can go into other players stables/horse guis
 */

@Getter
public class RPGHorsesMain extends JavaPlugin {

	private static RPGHorsesMain plugin;
	@Getter
	private static Version version;
	@Getter
	private static NMS NMS;

	private ExecutorService executorService;
	private RPGHorseManager rpgHorseManager;
	private CustomConfig marketConfig;
	private PlayerConfigs playerConfigs;
	private ItemUtil itemUtil;
	private RPGMessagingUtil messagingUtil;
	private HorseDespawner horseDespawner;
	private HorseOwnerManager horseOwnerManager;
	private StableGUIManager stableGuiManager;
	private MarketGUIManager marketGUIManager;
	private HorseGUIManager horseGUIManager;
	private TrailGUIManager trailGUIManager;
	private SellGUIManager sellGUIManager;
	private HorseCrateManager horseCrateManager;
	private ParticleManager particleManager;
	private XPManager xpManager;
	private SQLManager sqlManager;
	private UpdateNotifier updateNotifier;
	private MessageQueuer messageQueuer;
	private Permission permissions;
	private Economy economy;

	private Map<String, String> helpMessages = new LinkedHashMap<>();

	public static RPGHorsesMain getInstance() {
		return plugin;
	}

	public SQLManager getSQLManager() {
		return sqlManager;
	}

	@Override
	public void onLoad() {
		if (WorldGuardUtil.isEnabled()) {
			WorldGuardUtil.createFlags();
			getLogger().info(" Successfully hooked into WorldGuard");
		} else {
			getLogger().info(" Failed to hook into WorldGuard");
		}
	}

	@Override
	public void onEnable() {
		plugin = this;
		new DebugUtil().setPlugin(this);

		executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("RPGHorses").build());

		version = Version.getVersion();
		Bukkit.getLogger().info("[RPGHorses] " + version.getName() + " detected from Bukkit version " + Bukkit.getBukkitVersion().split("-")[0]);

		try {
			final Class<?> clazz = Class.forName("org.plugins.rpghorses." + version.getAbstractName() + ".NMSHandler");
			if (NMS.class.isAssignableFrom(clazz)) {
				NMS = (NMS) clazz.getConstructor().newInstance();
			}
		} catch (final Exception e) {
			this.getLogger().info("Could not find nms support for this version (" + version.getName() + ").");
			this.getLogger().info("Because of this, horses will wander when unmounted");
		}

		this.messagingUtil = new RPGMessagingUtil(this);

		this.loadConfigs();
		this.initializeVariables();
		this.registerEvents();
		this.loadCommands();
		setupHelpMessage();
		Metrics metrics = new Metrics(this, 6955);
		metrics.addCustomChart(new SimplePie("sql", () -> getConfig().getBoolean("sql.enabled") ? "Enabled" : "Disabled"));

		Bukkit.getScheduler().runTaskLater(this, () -> {
			if (this.loadVault()) {
				messagingUtil.sendMessage(Bukkit.getConsoleSender(), "[RPGHorses] Successfully hooked into &aVault");
			} else {
				messagingUtil.sendMessage(Bukkit.getConsoleSender(), "[RPGHorses] Failed to hook into &cVault&7, some features may be broken");
			}
		}, 1L);
	}

	public void loadConfigs() {
		CustomConfigUtil.loadDefaultConfig(this);
		TimeUtil.refreshUnitStrings(getConfig(), "time-options.");

		this.playerConfigs = new PlayerConfigs(this);

		this.marketConfig = new CustomConfig(this, "market");
		CustomConfigUtil.loadConfig(this.marketConfig);

		updateConfigs();
	}

	public void updateConfigs() {
		if (Version.getVersion().getWeight() < Version.v1_20.getWeight())
			return;

		FileConfiguration config = getConfig();

		int version = config.getInt("version", 1);
		int latestVersion = 6;

		if (version < 2) {
			config.set("stable-options.market-horse-item.skin-value", null);
			config.set("stable-options.market-horse-item.textures-url", "https://textures.minecraft.net/texture/16234ae7d55903ea8bc34413cd52ded3b37c92eee5ae533fc5126a65461f11f");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("stable-options.market-horse-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/33098-minecraft-earth-shop"));
			}

			config.set("stable-options.previous-page-item.skin-value", null);
			config.set("stable-options.previous-page-item.textures-url", "https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("stable-options.previous-page-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7827-oak-wood-arrow-left"));
			}

			// put new textures-url values from config.yml
			config.set("stable-options.next-page-item.skin-value", null);
			config.set("stable-options.next-page-item.textures-url", "https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("stable-options.next-page-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7828-oak-wood-arrow-right"));
			}

			config.set("horse-gui-options.items.sell-item.skin-value", null);
			config.set("horse-gui-options.items.sell-item.textures-url", "https://textures.minecraft.net/texture/7e3deb57eaa2f4d403ad57283ce8b41805ee5b6de912ee2b4ea736a9d1f465a7");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.sell-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/33098-minecraft-earth-shop"));
			}

			config.set("horse-gui-options.items.rename-item.skin-value", null);
			config.set("horse-gui-options.items.rename-item.textures-url", "https://textures.minecraft.net/texture/11bb4266a22dcbc4607621b9c768932950160c2b96708267d707d44551378cd7");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.rename-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/35130-red-a"));
			}

			config.set("horse-gui-options.items.toggle-automount-on.skin-value", null);
			config.set("horse-gui-options.items.toggle-automount-on.textures-url", "https://textures.minecraft.net/texture/a92e31ffb59c90ab08fc9dc1fe26802035a3a47c42fee63423bcdb4262ecb9b6");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.toggle-automount-on.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/21771-lime-checkmarkp"));
			}

			config.set("horse-gui-options.items.toggle-automount-off.skin-value", null);
			config.set("horse-gui-options.items.toggle-automount-off.textures-url", "https://textures.minecraft.net/texture/ff9d9de62ecae9b798555fd23e8ca35e2605291939c1862fe79066698c9508a7");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.toggle-automount-off.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/21766-red-checkmark"));
			}

			config.set("horse-gui-options.items.trails-item.skin-value", null);
			config.set("horse-gui-options.items.trails-item.textures-url", "https://textures.minecraft.net/texture/badc048a7ce78f7dad72a07da27d85c0916881e5522eeed1e3daf217a38c1a");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.trails-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7822-oak-wood-question-mark"));
			}

			// upgrade-item
			config.set("horse-gui-options.items.upgrade-item.skin-value", null);
			config.set("horse-gui-options.items.upgrade-item.textures-url", "https://textures.minecraft.net/texture/a99aaf2456a6122de8f6b62683f2bc2eed9abb81fd5bea1b4c23a58156b669");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.upgrade-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/11214-quartz-arrow-up"));
			}

			// delete-item
			config.set("horse-gui-options.items.delete-item.skin-value", null);
			config.set("horse-gui-options.items.delete-item.textures-url", "https://textures.minecraft.net/texture/1d9bfe5f2bc5a64024f7591b24f95112508d7d66adc998bfd3ce5afdf149ae4f");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.delete-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/4517-trash-can"));
			}

			// back-item
			config.set("horse-gui-options.items.back-item.skin-value", null);
			config.set("horse-gui-options.items.back-item.textures-url", "https://textures.minecraft.net/texture/bfe567282e78607f2ca2aef583b8efebc91959f84cae4a83bed10dcd5b0cfccd");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("horse-gui-options.items.back-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/18241-hay-bale"));
			}

			config.set("market-options.your-horses-item.skin-value", null);
			config.set("market-options.your-horses-item.textures-url", "https://textures.minecraft.net/texture/bfe567282e78607f2ca2aef583b8efebc91959f84cae4a83bed10dcd5b0cfccd");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("market-options.your-horses-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/18241-hay-bale"));
			}

			// back-item
			config.set("market-options.back-item.skin-value", null);
			config.set("market-options.back-item.textures-url", "https://textures.minecraft.net/texture/bfe567282e78607f2ca2aef583b8efebc91959f84cae4a83bed10dcd5b0cfccd");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("market-options.back-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/18241-hay-bale"));
			}

			// previous-page-item
			config.set("market-options.previous-page-item.skin-value", null);
			config.set("market-options.previous-page-item.textures-url", "https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("market-options.previous-page-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7827-oak-wood-arrow-left"));
			}

			// next-page-item
			config.set("market-options.next-page-item.skin-value", null);
			config.set("market-options.next-page-item.textures-url", "https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("market-options.next-page-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7828-oak-wood-arrow-right"));
			}

			// sell-gui-options.items.decrease-item1
			config.set("sell-gui-options.items.decrease-item1.skin-value", null);
			config.set("sell-gui-options.items.decrease-item1.textures-url", "https://textures.minecraft.net/texture/7437346d8bda78d525d19f540a95e4e79daeda795cbca5a13256236312cf");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.decrease-item1.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7824-oak-wood-arrow-down"));
			}

			// sell-gui-options.items.decrease-item2
			config.set("sell-gui-options.items.decrease-item2.skin-value", null);
			config.set("sell-gui-options.items.decrease-item2.textures-url", "https://textures.minecraft.net/texture/9b7ce683d0868aa4378aeb60caa5ea80596bcffdaab6b5af2d12595837a84853");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.decrease-item2.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/515-stone-arrow-down"));
			}

			// sell-gui-options.items.decrease-item3
			config.set("sell-gui-options.items.decrease-item3.skin-value", null);
			config.set("sell-gui-options.items.decrease-item3.textures-url", "https://textures.minecraft.net/texture/3912d45b1c78cc22452723ee66ba2d15777cc288568d6c1b62a545b29c7187");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.decrease-item3.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/11221-quartz-arrow-down"));
			}

			// sell-gui-options.items.increase-item1
			config.set("sell-gui-options.items.increase-item1.skin-value", null);
			config.set("sell-gui-options.items.increase-item1.textures-url", "https://textures.minecraft.net/texture/3040fe836a6c2fbd2c7a9c8ec6be5174fdfdfa1a2f55e366156fa5f712e10");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.increase-item1.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7825-oak-wood-arrow-up"));
			}

			// sell-gui-options.items.increase-item2
			config.set("sell-gui-options.items.increase-item2.skin-value", null);
			config.set("sell-gui-options.items.increase-item2.textures-url", "https://textures.minecraft.net/texture/58fe251a40e4167d35d081c27868ac151af96b6bd16dd2834d5dc7235f47791d");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.increase-item2.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/514-stone-arrow-up"));
			}

			// sell-gui-options.items.increase-item3
			config.set("sell-gui-options.items.increase-item3.skin-value", null);
			config.set("sell-gui-options.items.increase-item3.textures-url", "https://textures.minecraft.net/texture/a99aaf2456a6122de8f6b62683f2bc2eed9abb81fd5bea1b4c23a58156b669");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.increase-item3.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/11214-quartz-arrow-up"));
			}

			// confirm-item
			config.set("sell-gui-options.items.confirm-item.skin-value", null);
			config.set("sell-gui-options.items.confirm-item.textures-url", "https://textures.minecraft.net/texture/a92e31ffb59c90ab08fc9dc1fe26802035a3a47c42fee63423bcdb4262ecb9b6");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.confirm-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/21771-lime-checkmark"));
			}

			// back-item
			config.set("sell-gui-options.items.back-item.skin-value", null);
			config.set("sell-gui-options.items.back-item.textures-url", "https://textures.minecraft.net/texture/bfe567282e78607f2ca2aef583b8efebc91959f84cae4a83bed10dcd5b0cfccd");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("sell-gui-options.items.back-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/18241-hay-bale"));
			}

			// trail-gui-options.unknown-trail
			config.set("trail-gui-options.unknown-trail.skin-value", null);
			config.set("trail-gui-options.unknown-trail.textures-url", "https://textures.minecraft.net/texture/bc8ea1f51f253ff5142ca11ae45193a4ad8c3ab5e9c6eec8ba7a4fcb7bac40");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.unknown-trail.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/9236-white-question-mark"));
			}

			// back-item
			config.set("trail-gui-options.back-item.skin-value", null);
			config.set("trail-gui-options.back-item.textures-url", "https://textures.minecraft.net/texture/bfe567282e78607f2ca2aef583b8efebc91959f84cae4a83bed10dcd5b0cfccd");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.back-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/18241-hay-bale"));
			}

			// trails.DRIP_WATER
			config.set("trail-gui-options.trails.DRIP_WATER", null);
			config.set("trail-gui-options.trails.DRIP_WATER.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzRmY2JjMjU2ZDBiZTdlNjgzYWY4NGUzOGM0YmNkYjcxYWZiOTM5ODUzOGEyOWFhOTZjYmZhMzE4YjJlYSJ9fX0=");
			config.set("trail-gui-options.trails.DRIP_WATER.textures-url", "https://textures.minecraft.net/texture/34fcbc256d0be7e683af84e38c4bcdb71afb9398538a29aa96cbfa318b2ea");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.DRIP_WATER.slin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/13980-water"));
				config.setInlineComments("trail-gui-options.trails.DRIP_WATER.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/13980-water"));
			}

			// trails.CRIT
			config.set("trail-gui-options.trails.CRIT", null);
			config.set("trail-gui-options.trails.CRIT.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzdlNmM0MGY2OGI3NzVmMmVmY2Q3YmQ5OTE2YjMyNzg2OWRjZjI3ZTI0Yzg1NWQwYTE4ZTA3YWMwNGZlMSJ9fX0=");
			config.set("trail-gui-options.trails.CRIT.textures-url", "https://textures.minecraft.net/texture/c7e6c40f68b775f2efcd7bd9916b327869dcf27e24c855d0a18e07ac04fe1");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.CRIT.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/2361-sword"));
				config.setInlineComments("trail-gui-options.trails.CRIT.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/2361-sword"));
			}

			// CRIT_MAGIC
			config.set("trail-gui-options.trails.CRIT_MAGIC", null);
			config.set("trail-gui-options.trails.CRIT_MAGIC.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2VlYTM0NTkwOGQxN2RjNDQ5NjdkMWRjZTQyOGYyMmYyYjE5Mzk3MzcwYWJlYjc3YmRjMTJlMmRkMWNiNiJ9fX0=");
			config.set("trail-gui-options.trails.CRIT_MAGIC.textures-url", "https://textures.minecraft.net/texture/7eea345908d17dc44967d1dce428f22f2b19397370abeb77bdc12e2dd1cb6");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.CRIT_MAGIC.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/4103-magic-book"));
				config.setInlineComments("trail-gui-options.trails.CRIT_MAGIC.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/4103-magic-book"));
			}

			// SPELL
			config.set("trail-gui-options.trails.SPELL", null);
			config.set("trail-gui-options.trails.SPELL.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDY2YjEwYmY2ZWUyY2Q3ZTNhYzk2ZDk3NDllYTYxNmFhOWM3MzAzMGJkY2FlZmZhZWQyNDllNTVjODQ5OTRhYyJ9fX0=");
			config.set("trail-gui-options.trails.SPELL.textures-url", "https://textures.minecraft.net/texture/466b10bf6ee2cd7e3ac96d9749ea616aa9c73030bdcaeffaed249e55c84994ac");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.SPELL.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/19792-cloud"));
				config.setInlineComments("trail-gui-options.trails.SPELL.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/19792-cloud"));
			}

			// SPELL_INSTANT
			config.set("trail-gui-options.trails.SPELL_INSTANT", null);
			config.set("trail-gui-options.trails.SPELL_INSTANT.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTUzZGQ0NTc5ZWRjMmE2ZjIwMzJmOTViMWMxODk4MTI5MWI2YzdjMTFlYjM0YjZhOGVkMzZhZmJmYmNlZmZmYiJ9fX0=");
			config.set("trail-gui-options.trails.SPELL_INSTANT.textures-url", "https://textures.minecraft.net/texture/953dd4579edc2a6f2032f95b1c18981291b6c7c11eb34b6a8ed36afbfbcefffb");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.SPELL_INSTANT.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/2300-repeat"));
				config.setInlineComments("trail-gui-options.trails.SPELL_INSTANT.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/2300-repeat"));
			}

			// SPELL_MOB
			config.set("trail-gui-options.trails.SPELL_MOB", null);
			config.set("trail-gui-options.trails.SPELL_MOB.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDRiMDM3OTRiOWIzZTNiNWQwN2UzYmU2OGI5NmFmODdkZjIxNWMzNzUyZTU0NzM2YzgwZjdkNTBiZDM0MzdhNCJ9fX0=");
			config.set("trail-gui-options.trails.SPELL_MOB.textures-url", "https://textures.minecraft.net/texture/44b03794b9b3e3b5d07e3be68b96af87df215c3752e54736c80f7d50bd3437a4");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.SPELL_MOB.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/18667-wool-rainbow"));
				config.setInlineComments("trail-gui-options.trails.SPELL_MOB.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/18667-wool-rainbow"));
			}

			// DRIP_LAVA
			config.set("trail-gui-options.trails.DRIP_LAVA", null);
			config.set("trail-gui-options.trails.DRIP_LAVA.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjY5NjVlNmE1ODY4NGMyNzdkMTg3MTdjZWM5NTlmMjgzM2E3MmRmYTk1NjYxMDE5ZGJjZGYzZGJmNjZiMDQ4In19fQ==");
			config.set("trail-gui-options.trails.DRIP_LAVA.textures-url", "https://textures.minecraft.net/texture/b6965e6a58684c277d18717cec959f2833a72dfa95661019dbcdf3dbf66b048");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.DRIP_LAVA.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/1055-lava"));
				config.setInlineComments("trail-gui-options.trails.DRIP_LAVA.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/1055-lava"));
			}

			// VILLAGER_ANGRY
			config.set("trail-gui-options.trails.VILLAGER_ANGRY", null);
			config.set("trail-gui-options.trails.VILLAGER_ANGRY.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmJiMTI1NmViOWY2NjdjMDVmYjIxZTAyN2FhMWQ1MzU1OGJkYTc0ZTI0MGU0ZmE5ZTEzN2Q4NTFjNDE2ZmU5OCJ9fX0=");
			config.set("trail-gui-options.trails.VILLAGER_ANGRY.textures-url", "https://textures.minecraft.net/texture/fbb1256eb9f667c05fb21e027aa1d53558bda74e240e4fa9e137d851c416fe98");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.VILLAGER_ANGRY.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/34653-angry-particle"));
				config.setInlineComments("trail-gui-options.trails.VILLAGER_ANGRY.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/34653-angry-particle"));
			}

			// VILLAGER_HAPPY
			config.set("trail-gui-options.trails.VILLAGER_HAPPY", null);
			config.set("trail-gui-options.trails.VILLAGER_HAPPY.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjcyYzA1ZGQ3NjI4OGY0MzI4YTI0MzkxYmY0NzI1ZmQyMjYwNTkyZGIzY2Y5YjJiYzIwMzJkZDA1OTZjZjQ0MCJ9fX0=");
			config.set("trail-gui-options.trails.VILLAGER_HAPPY.textures-url", "https://textures.minecraft.net/texture/b72c05dd76288f4328a24391bf4725fd2260592db3cf9b2bc2032dd0596cf440");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.VILLAGER_HAPPY.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/22339-emerald-block"));
				config.setInlineComments("trail-gui-options.trails.VILLAGER_HAPPY.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/22339-emerald-block"));
			}

			// NOTE
			config.set("trail-gui-options.trails.NOTE", null);
			config.set("trail-gui-options.trails.NOTE.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNlZWI3N2Q0ZDI1NzI0YTljYWYyYzdjZGYyZDg4Mzk5YjE0MTdjNmI5ZmY1MjEzNjU5YjY1M2JlNDM3NmUzIn19fQ==");
			config.set("trail-gui-options.trails.NOTE.textures-url", "https://textures.minecraft.net/texture/4ceeb77d4d25724a9caf2c7cdf2d88399b1417c6b9ff5213659b653be4376e3");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.NOTE.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/1104-note-block"));
				config.setInlineComments("trail-gui-options.trails.NOTE.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/1104-note-block"));
			}

			// PORTAL
			config.set("trail-gui-options.trails.PORTAL", null);
			config.set("trail-gui-options.trails.PORTAL.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjBiZmMyNTc3ZjZlMjZjNmM2ZjczNjVjMmM0MDc2YmNjZWU2NTMxMjQ5ODkzODJjZTkzYmNhNGZjOWUzOWIifX19");
			config.set("trail-gui-options.trails.PORTAL.textures-url", "https://textures.minecraft.net/texture/b0bfc2577f6e26c6c6f7365c2c4076bccee653124989382ce93bca4fc9e39b");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.PORTAL.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/1109-nether-portal"));
				config.setInlineComments("trail-gui-options.trails.PORTAL.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/1109-nether-portal"));
			}

			// ENCHANTMENT_TABLE
			config.set("trail-gui-options.trails.ENCHANTMENT_TABLE", null);
			config.set("trail-gui-options.trails.ENCHANTMENT_TABLE.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjJmNzkwMTZjYWQ4NGQxYWUyMTYwOWM0ODEzNzgyNTk4ZTM4Nzk2MWJlMTNjMTU2ODI3NTJmMTI2ZGNlN2EifX19");
			config.set("trail-gui-options.trails.ENCHANTMENT_TABLE.textures-url", "https://textures.minecraft.net/texture/b2f79016cad84d1ae21609c4813782598e387961be13c15682752f126dce7a");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.ENCHANTMENT_TABLE.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/763-enchantment-table"));
				config.setInlineComments("trail-gui-options.trails.ENCHANTMENT_TABLE.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/763-enchantment-table"));
			}

			// SNOWBALL
			config.set("trail-gui-options.trails.SNOWBALL", null);
			config.set("trail-gui-options.trails.SNOWBALL.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzAyOTFlOTMyNjI5NmFhYWYyN2YyNTVkZTk4MWU3ODc4NjhiYzYyZmU0NjYzOWVlODdiMjhhMTk1ZTlkOTliZiJ9fX0=");
			config.set("trail-gui-options.trails.SNOWBALL.textures-url", "https://textures.minecraft.net/texture/30291e9326296aaaf27f255de981e787868bc62fe46639ee87b28a195e9d99bf");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.SNOWBALL.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/11470-snowball"));
				config.setInlineComments("trail-gui-options.trails.SNOWBALL.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/11470-snowball"));
			}

			// SLIME
			config.set("trail-gui-options.trails.SLIME", null);
			config.set("trail-gui-options.trails.SLIME.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDkzNGE5ZjVhYjE3ODlhN2Q4ZGQ5NmQzMjQ5M2NkYWNmZjU3N2Q4YzgxZTdiMjM5MTdkZmYyZTMyYmQwYmMxMCJ9fX0=");
			config.set("trail-gui-options.trails.SLIME.textures-url", "https://textures.minecraft.net/texture/4934a9f5ab1789a7d8dd96d32493cdacff577d8c81e7b23917dff2e32bd0bc10");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.SLIME.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/1157-slime-ball"));
				config.setInlineComments("trail-gui-options.trails.SLIME.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/1157-slime-ball"));
			}

			// HEART
			config.set("trail-gui-options.trails.HEART", null);
			config.set("trail-gui-options.trails.HEART.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjEyNjZiNzQ4MjQyMTE1YjMwMzcwOGQ1OWNlOWQ1NTIzYjdkNzljMTNmNmRiNGViYzkxZGQ0NzIwOWViNzU5YyJ9fX0=");
			config.set("trail-gui-options.trails.HEART.textures-url", "https://textures.minecraft.net/texture/f1266b748242115b303708d59ce9d5523b7d79c13f6db4ebc91dd47209eb759c");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.HEART.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/34655-heart-particle"));
				config.setInlineComments("trail-gui-options.trails.HEART.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/miscellaneous/34655-heart-particle"));
			}

			// BARRIER
			config.set("trail-gui-options.trails.BARRIER", null);
			config.set("trail-gui-options.trails.BARRIER.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==");
			config.set("trail-gui-options.trails.BARRIER.textures-url", "https://textures.minecraft.net/texture/3ed1aba73f639f4bc42bd48196c715197be2712c3b962c97ebf9e9ed8efa025");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.BARRIER.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/3229-barrier"));
				config.setInlineComments("trail-gui-options.trails.BARRIER.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/blocks/3229-barrier"));
			}

			// TOTEM
			config.set("trail-gui-options.trails.TOTEM", null);
			config.set("trail-gui-options.trails.TOTEM.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTFkMWNmMjg5MTY1ZmJiMTEyZDg5MjFkNDc3MDhlNDlmYjcwOTI1NzM5YjFjYmQxNzk4ZGFmZjQyMjgwNmU4YSJ9fX0=");
			config.set("trail-gui-options.trails.TOTEM.textures-url", "https://textures.minecraft.net/texture/a1d1cf289165fbb112d8921d47708e49fb70925739b1cbd1798daff422806e8a");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.TOTEM.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/32234-totem-of-undying"));
				config.setInlineComments("trail-gui-options.trails.TOTEM.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/32234-totem-of-undying"));
			}

			// NAUTILUS
			config.set("trail-gui-options.trails.NAUTILUS", null);
			config.set("trail-gui-options.trails.NAUTILUS.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODEyNmNhYzE2ZmQ4ZTQ3NTE2ZTg0NTIwY2QzOTgxYzQ1ZDcwOGY1NWQzNDU4NDk0ZDhmMDgxYzUwNWQ2ZDMwNCJ9fX0=");
			config.set("trail-gui-options.trails.NAUTILUS.textures-url", "https://textures.minecraft.net/texture/8126cac16fd8e47516e84520cd3981c45d708f55d3458494d8f081c505d6d304");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.NAUTILUS.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/20119-nautilus-shell"));
				config.setInlineComments("trail-gui-options.trails.NAUTILUS.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/decoration/20119-nautilus-shell"));
			}

			// DRIPPING_HONEY
			config.set("trail-gui-options.trails.DRIPPING_HONEY", null);
			config.set("trail-gui-options.trails.DRIPPING_HONEY.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTZkY2M4ZjM3YWM5OWQ5NTFlY2JjNWRmNWU4NTgyMTMzZjVmMjMwN2U3NjlhZjZiNmNmZmY0MjgyMTgwNjcifX19");
			config.set("trail-gui-options.trails.DRIPPING_HONEY.textures-url", "https://textures.minecraft.net/texture/e6dcc8f37ac99d951ecbc5df5e8582133f5f2307e769af6b6cfff428218067");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trails.DRIPPING_HONEY.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/food%20&%20drinks/871-honey"));
				config.setInlineComments("trail-gui-options.trails.DRIPPING_HONEY.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/food%20&%20drinks/871-honey"));
			}
		}

		if (version < 3) {
			config.set("trail-gui-options.previous-page-item.material", "PLAYER_HEAD");
			config.set("trail-gui-options.previous-page-item.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==");
			config.set("trail-gui-options.previous-page-item.textures-url", "https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.previous-page-item.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7827-oak-wood-arrow-left"));
				config.setInlineComments("trail-gui-options.previous-page-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7827-oak-wood-arrow-left"));
			}

			config.set("trail-gui-options.next-page-item.material", "PLAYER_HEAD");
			config.set("trail-gui-options.next-page-item.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19");
			config.set("trail-gui-options.next-page-item.textures-url", "https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.next-page-item.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7826-oak-wood-arrow-right"));
				config.setInlineComments("trail-gui-options.next-page-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/alphabet/7826-oak-wood-arrow-right"));
			}

			config.set("trail-gui-options.trail-item.skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ5ZjRiZGIxYWJkMWFiNzgyNDM5YTgyZGJlMGQ1NDM2Zjk2YmY2MjgzYmQ0MjE1NmFmYjdhOTA4OWYwN2FkNCJ9fX0=");
			config.set("trail-gui-options.trail-item.textures-url", "https://textures.minecraft.net/texture/ad9f4bdb1abd1ab782439a82dbe0d5436f96bf6283bd42156afb7a9089f07ad4");

			if (roryslibrary.util.Version.isRunningMinimum(roryslibrary.util.Version.v1_18)) {
				config.setInlineComments("trail-gui-options.trail-item.skin-value", Collections.singletonList("https://minecraft-heads.com/custom-heads/head/61528-horse-brush"));
				config.setInlineComments("trail-gui-options.trail-item.textures-url", Collections.singletonList("https://minecraft-heads.com/custom-heads/head/61528-horse-brush"));
			}
		}

		if (version < 4) {
			config.set("messages.all-horses-removed", "{PREFIX}&7All of &6{PLAYER}'s {TOTAL-HORSES} &7horses removed successfully");
			config.set("messages.all-your-horses-were-removed", "{PREFIX}&7All your &6{TOTAL-HORSES} &7have been removed by {PLAYER}");
			config.set("messages.no-horse-pvp", "{PREFIX}You can't attack other player's horses here");

			config.set("horse-gui-options.items.max-upgrade-item.enabled", true);
			config.set("horse-gui-options.items.max-upgrade-item.PURPOSE", "MAXED_LEVEL");
			config.set("horse-gui-options.items.max-upgrade-item.material", "PLAYER_HEAD");
			config.set("horse-gui-options.items.max-upgrade-item.textures-url", "https://textures.minecraft.net/texture/a99aaf2456a6122de8f6b62683f2bc2eed9abb81fd5bea1b4c23a58156b669");
			config.set("horse-gui-options.items.max-upgrade-item.name", "&6&lMaxed Horse");
			config.set("horse-gui-options.items.max-upgrade-item.lore", Collections.singletonList("&7Your horse is already max level"));
			config.set("horse-gui-options.items.max-upgrade-item.x-cord", 7);
			config.set("horse-gui-options.items.max-upgrade-item.y-cord", 2);
		}

		if (version < 5) {
			config.set("messages.horse-is-dead", "{PREFIX}You horse is dead and won't respawn for another &6{TIME-LEFT}");

			List<String> newUpgradeLore = new ArrayList<>();
			newUpgradeLore.add("&7Click to upgrade your horse");
			newUpgradeLore.add("");
			newUpgradeLore.add("&7Tier: &d{OLD-TIER} &7-> &d{NEW-TIER}");
			newUpgradeLore.add("&7Health: &c{OLD-HEALTH} &7-> &c{NEW-HEALTH}");
			newUpgradeLore.add("&7Speed: &b{OLD-SPEED} &7-> &b{NEW-SPEED}");
			newUpgradeLore.add("&7Jump: &e{OLD-JUMP-STRENGTH} &7-> &e{NEW-JUMP-STRENGTH}");
			newUpgradeLore.add("");
			newUpgradeLore.add("&7Cost: &a${COST}");
			newUpgradeLore.add("&7Horse XP Needed: &a{HORSE-EXP-NEEDED}");
			config.set("horse-gui-options.items.upgrade-item.lore", newUpgradeLore);

			config.set("horse-options.health-regen-interval", 20);
			config.setComments("horse-options.health-regen-interval", Collections.singletonList("How often should rpg-horses regen health? (in ticks)"));
			config.set("horse-options.health-regen-amount", 1.0);
			config.setComments("horse-options.health-regen-amount", Collections.singletonList("How much health should rpg-horses regen each interval?"));
			config.set("horse-options.only-regen-active-horses", false);
			config.setComments("horse-options.only-regen-active-horses", Collections.singletonList("Should rpg-horses regen health only when active?"));
		}

		if (version < 6) {
			config.set("messages.horse-rename-title", "&6Type the new name");

			for (Particle particle : Particle.values()) {
				if (particle.getDataType() != Color.class && particle.getDataType() != Void.class)
					continue;

				String path = "trail-gui-options.trails." + particle.name();

				if (config.contains(path, true) && !config.isConfigurationSection(path))
					continue;

				ConfigurationSection section = config.isConfigurationSection(path) ? config.getConfigurationSection(path) : config.createSection(path);

				StringBuilder placeholder = new StringBuilder();
				for (String word : particle.name().split("_"))
					placeholder.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");

				if (!section.contains("placeholder")) section.set("placeholder", placeholder.toString().trim());
				if (!section.contains("enabled")) section.set("enabled", true);
				if (!section.contains("textures-url")) {
					if (particle.name().equals("SOUL_FIRE_FLAME")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/d20cb73f207b07d259ca1eef7cade302c7ea104b8a0c240b897a577971122e56");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDIwY2I3M2YyMDdiMDdkMjU5Y2ExZWVmN2NhZGUzMDJjN2VhMTA0YjhhMGMyNDBiODk3YTU3Nzk3MTEyMmU1NiJ9fX0=");
					} else if (particle.name().equals("ITEM_SNOWBALL")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/e115c7968ec3771ee9ff6ae6bca2d5ba3962aa727a4fa8d37608e4c9bf1512bb");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTExNWM3OTY4ZWMzNzcxZWU5ZmY2YWU2YmNhMmQ1YmEzOTYyYWE3MjdhNGZhOGQzNzYwOGU0YzliZjE1MTJiYiJ9fX0=");
					} else if (particle.name().equals("BUBBLE_COLUMN_UP")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/ae40fe6a9db22f2b12e605e492995bd46ac9367b26b8ab85e07266801becf71d");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjhmYjQwNGFmY2VhMTg3MTg3MmI4MWM3MTM5N2E1YzE2NTY0Mjg2MjYxZTI2NDdmZDY3NmZmYjk5MTc2MzJhZiJ9fX0=");
					} else if (particle.name().equals("BUBBLE_POP")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/ae40fe6a9db22f2b12e605e492995bd46ac9367b26b8ab85e07266801becf71d");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjhmYjQwNGFmY2VhMTg3MTg3MmI4MWM3MTM5N2E1YzE2NTY0Mjg2MjYxZTI2NDdmZDY3NmZmYjk5MTc2MzJhZiJ9fX0=");
					} else if (particle.name().equals("CAMPFIRE_COSY_SMOKE") || particle.name().equals("CAMPFIRE_SIGNAL_SMOKE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6d95965d79ddf613f594fd20d51e765c09a2a5ff8d0e09cff19a8ea4302358ad");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQ5NTk2NWQ3OWRkZjYxM2Y1OTRmZDIwZDUxZTc2NWMwOWEyYTVmZjhkMGUwOWNmZjE5YThlYTQzMDIzNThhZCJ9fX0=");
					} else if (particle.name().equals("CHERRY_LEAVES")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/9679861f7ecfe494103214d8a69b4cf51bd6a51c22662dc72345f23115c0a6ee");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTY3OTg2MWY3ZWNmZTQ5NDEwMzIxNGQ4YTY5YjRjZjUxYmQ2YTUxYzIyNjYyZGM3MjM0NWYyMzExNWMwYTZlZSJ9fX0=");
					} else if (particle.name().equals("CLOUD")) {
						section.set("textures-url", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWE5NTcwNDE1Zjk0YjM5NGZmNTFhOTI1OWYxZmNmOWRiMzA2Njc3NDM4YmRjOGJhYzM1ZGNkNTkxYWEwMmVkZSJ9fX0=");
						if (!section.contains("skin-value"))
							section.set("skin-value", "https://textures.minecraft.net/texture/1a9570415f94b394ff51a9259f1fcf9db306677438bdc8bac35dcd591aa02ede");
					} else if (particle.name().equals("COMPOSTER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/53cb8f61e7f6bf957a2134e96faeb0fc924137d4bff88d9518b2bf662586c92e");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTNjYjhmNjFlN2Y2YmY5NTdhMjEzNGU5NmZhZWIwZmM5MjQxMzdkNGJmZjg4ZDk1MThiMmJmNjYyNTg2YzkyZSJ9fX0=");
					} else if (particle.name().equals("CRIMSON_SPORE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/944e504f311f0076384b3e09060fefefc44e0770523e684dd3df9402e06aa1b1");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQ0ZTUwNGYzMTFmMDA3NjM4NGIzZTA5MDYwZmVmZWZjNDRlMDc3MDUyM2U2ODRkZDNkZjk0MDJlMDZhYTFiMSJ9fX0=");
					} else if (particle.name().equals("CURRENT_DOWN")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/f0d1df8046f0b5d934c3e05798eacfeea6d7b595dbe26debf7db9acc8c4fa798");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjBkMWRmODA0NmYwYjVkOTM0YzNlMDU3OThlYWNmZWVhNmQ3YjU5NWRiZTI2ZGViZjdkYjlhY2M4YzRmYTc5OCJ9fX0=");
					} else if (particle.name().equals("DAMAGE_INDICATOR")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/5cd20bd8c48ea2a27055eaf90b1a920f4eaf9a81ec8c3328a4c974c9e13ce3c2");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWNkMjBiZDhjNDhlYTJhMjcwNTVlYWY5MGIxYTkyMGY0ZWFmOWE4MWVjOGMzMzI4YTRjOTc0YzllMTNjZTNjMiJ9fX0=");
					} else if (particle.name().equals("DOLPHIN")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/8e9688b950d880b55b7aa2cfcd76e5a0fa94aac6d16f78e833f7443ea29fed3");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGU5Njg4Yjk1MGQ4ODBiNTViN2FhMmNmY2Q3NmU1YTBmYTk0YWFjNmQxNmY3OGU4MzNmNzQ0M2VhMjlmZWQzIn19fQ==");
					} else if (particle.name().equals("DRAGON_BREATH")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/8f81c9d42680ad7bc686a1281073fbbe2fd8161913e5a3bd99693d2c404d8828");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGY4MWM5ZDQyNjgwYWQ3YmM2ODZhMTI4MTA3M2ZiYmUyZmQ4MTYxOTEzZTVhM2JkOTk2OTNkMmM0MDRkODgyOCJ9fX0=");
					} else if (particle.name().equals("DRIPPING_DRIPSTONE_LAVA") || particle.name().equals("DRIPPING_DRIPSTONE_WATER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/450ad731ce6dc21eb5eef6948bd2180a7a32fa1fe3851e47e3c00c6e246249a2");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDUwYWQ3MzFjZTZkYzIxZWI1ZWVmNjk0OGJkMjE4MGE3YTMyZmExZmUzODUxZTQ3ZTNjMDBjNmUyNDYyNDlhMiJ9fX0=");
					} else if (particle.name().equals("DRIPPING_HONEY")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/8aebd0df92ebf43a744e86663230cb08274faf303e6b3caef4e50d3838fbf14f");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFlYmQwZGY5MmViZjQzYTc0NGU4NjY2MzIzMGNiMDgyNzRmYWYzMDNlNmIzY2FlZjRlNTBkMzgzOGZiZjE0ZiJ9fX0=");
					} else if (particle.name().equals("DRIPPING_LAVA")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/b69a32cfff03155b9f8109858d8303b06fe70d0b535ba64b51d0302ff339e0cb");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjY5YTMyY2ZmZjAzMTU1YjlmODEwOTg1OGQ4MzAzYjA2ZmU3MGQwYjUzNWJhNjRiNTFkMDMwMmZmMzM5ZTBjYiJ9fX0=");
					} else if (particle.name().equals("DRIPPING_OBSIDIAN_TEAR")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6426ab888298adf1effd811c8074def09780e7d9d12ba4c77b73fda9982dd0fe");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQyNmFiODg4Mjk4YWRmMWVmZmQ4MTFjODA3NGRlZjA5NzgwZTdkOWQxMmJhNGM3N2I3M2ZkYTk5ODJkZDBmZSJ9fX0=");
					} else if (particle.name().equals("DRIPPING_WATER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/49f1f07e2b1c32bb64a128e529f3af1e5286e518544edf8cbaa6c4065b476b");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDlmMWYwN2UyYjFjMzJiYjY0YTEyOGU1MjlmM2FmMWU1Mjg2ZTUxODU0NGVkZjhjYmFhNmM0MDY1YjQ3NmIifX19");
					} else if (particle.name().equals("DUST_PLUME")) {
						section.set("textures-url", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDk5NDdkZTEyOTU4ZmU5NWU2YWExMTRmODRkNmYzOGRhMzAxOTJkZDZkNjllZDYzYzZmYjk3NjA4OTE0ZDM1MiJ9fX0=");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDk5NDdkZTEyOTU4ZmU5NWU2YWExMTRmODRkNmYzOGRhMzAxOTJkZDZkNjllZDYzYzZmYjk3NjA4OTE0ZDM1MiJ9fX0=");
					} else if (particle.name().equals("EGG_CRACK")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/bee0d852d452d50827b745088d7e7a1829b0690cf242bded7eab3532ad89608");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmVlMGQ4NTJkNDUyZDUwODI3Yjc0NTA4OGQ3ZTdhMTgyOWIwNjkwY2YyNDJiZGVkN2VhYjM1MzJhZDg5NjA4In19fQ==");
					} else if (particle.name().equals("ELDER_GUARDIAN")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/4340a268f25fd5cc276ca147a8446b2630a55867a2349f7ca107c26eb58991");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDM0MGEyNjhmMjVmZDVjYzI3NmNhMTQ3YTg0NDZiMjYzMGE1NTg2N2EyMzQ5ZjdjYTEwN2MyNmViNTg5OTEifX19");
					} else if (particle.name().equals("ELECTRIC_SPARK")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/c465c121958c0522e3dccb3d14d68612d6317cd380b0e646b61b7420b904af02");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzQ2NWMxMjE5NThjMDUyMmUzZGNjYjNkMTRkNjg2MTJkNjMxN2NkMzgwYjBlNjQ2YjYxYjc0MjBiOTA0YWYwMiJ9fX0=");
					} else if (particle.name().equals("ENCHANT")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/28951399d0ebd0dfe87e50f0d6dee25274d93f1fbb38505ec971b601d1c2cb9");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjg5NTEzOTlkMGViZDBkZmU4N2U1MGYwZDZkZWUyNTI3NGQ5M2YxZmJiMzg1MDVlYzk3MWI2MDFkMWMyY2I5In19fQ==");
					} else if (particle.name().equals("ENCHANTED_HIT")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/348a7ea198ec4efd8b56bcda8aa4230039e04d1338ee98fa85897bd4f342d632");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ4YTdlYTE5OGVjNGVmZDhiNTZiY2RhOGFhNDIzMDAzOWUwNGQxMzM4ZWU5OGZhODU4OTdiZDRmMzQyZDYzMiJ9fX0=");
					} else if (particle.name().equals("END_ROD")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/d80b3d019cca367ce3ca6f182fcf91ac9b8fb0be29afcedfe558ecee20b817de");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDgwYjNkMDE5Y2NhMzY3Y2UzY2E2ZjE4MmZjZjkxYWM5YjhmYjBiZTI5YWZjZWRmZTU1OGVjZWUyMGI4MTdkZSJ9fX0=");
					} else if (particle.name().equals("EXPLOSION")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/602a1169309f05ef2f061b1fa0fe225f29d73a24f8f07ccc2a705deeaca069d1");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjAyYTExNjkzMDlmMDVlZjJmMDYxYjFmYTBmZTIyNWYyOWQ3M2EyNGY4ZjA3Y2NjMmE3MDVkZWVhY2EwNjlkMSJ9fX0=");
					} else if (particle.name().equals("EXPLOSION_EMITTER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/fc1087630208c9bc782c54722faeb99b8d5c9975a341c2f487b086824190b81b");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmMxMDg3NjMwMjA4YzliYzc4MmM1NDcyMmZhZWI5OWI4ZDVjOTk3NWEzNDFjMmY0ODdiMDg2ODI0MTkwYjgxYiJ9fX0=");
					} else if (particle.name().equals("FALLING_DRIPSTONE_LAVA")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/1c9e6bfdcbe297b53ce1f127a2af3cb9539415c418138fd1bdda57242c69edd");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWM5ZTZiZmRjYmUyOTdiNTNjZTFmMTI3YTJhZjNjYjk1Mzk0MTVjNDE4MTM4ZmQxYmRkYTU3MjQyYzY5ZWRkIn19fQ==");
					} else if (particle.name().equals("FALLING_DRIPSTONE_WATER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/e6799bfaa3a2c63ad85dd378e66d57d9a97a3f86d0d9f683c498632f4f5c");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY3OTliZmFhM2EyYzYzYWQ4NWRkMzc4ZTY2ZDU3ZDlhOTdhM2Y4NmQwZDlmNjgzYzQ5ODYzMmY0ZjVjIn19fQ==");
					} else if (particle.name().equals("FALLING_DUST")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/93a1b830399ab432a5178fdaf3939b24bf25c724a66be947296c503352bc380d");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTNhMWI4MzAzOTlhYjQzMmE1MTc4ZmRhZjM5MzliMjRiZjI1YzcyNGE2NmJlOTQ3Mjk2YzUwMzM1MmJjMzgwZCJ9fX0=");
					} else if (particle.name().equals("FALLING_HONEY")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/b906bfd817cc5818c7620e288f4e413014318c0bcbb5ecd6da8a5923d51c52f2");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjkwNmJmZDgxN2NjNTgxOGM3NjIwZTI4OGY0ZTQxMzAxNDMxOGMwYmNiYjVlY2Q2ZGE4YTU5MjNkNTFjNTJmMiJ9fX0=");
					} else if (particle.name().equals("FALLING_LAVA")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/15c6a8036dfb9bd1e4aecb3ce3fbc340aec30b676c08dc9dd4280f508c4cedda");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTVjNmE4MDM2ZGZiOWJkMWU0YWVjYjNjZTNmYmMzNDBhZWMzMGI2NzZjMDhkYzlkZDQyODBmNTA4YzRjZWRkYSJ9fX0=");
					} else if (particle.name().equals("FALLING_NECTAR")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/bb9960eade2dcb6e744c7d084def00599fe7327e3973cd2ca0af4a2a1e6eac08");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI5OTYwZWFkZTJkY2I2ZTc0NGM3ZDA4NGRlZjAwNTk5ZmU3MzI3ZTM5NzNjZDJjYTBhZjRhMmExZTZlYWMwOCJ9fX0=");
					} else if (particle.name().equals("FALLING_OBSIDIAN_TEAR")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6426ab888298adf1effd811c8074def09780e7d9d12ba4c77b73fda9982dd0fe");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQyNmFiODg4Mjk4YWRmMWVmZmQ4MTFjODA3NGRlZjA5NzgwZTdkOWQxMmJhNGM3N2I3M2ZkYTk5ODJkZDBmZSJ9fX0=");
					} else if (particle.name().equals("FALLING_SPORE_BLOSSOM")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/195f349cc4c017d2ad2a5300da87a43f7960fdf16ef3e05ad44ac22063a0c270");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTk1ZjM0OWNjNGMwMTdkMmFkMmE1MzAwZGE4N2E0M2Y3OTYwZmRmMTZlZjNlMDVhZDQ0YWMyMjA2M2EwYzI3MCJ9fX0=");
					} else if (particle.name().equals("FALLING_WATER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/314d8b1a62815e62dfd547ad90a3f3574529b3e04f70f435a4e205f5683ad62f");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzE0ZDhiMWE2MjgxNWU2MmRmZDU0N2FkOTBhM2YzNTc0NTI5YjNlMDRmNzBmNDM1YTRlMjA1ZjU2ODNhZDYyZiJ9fX0=");
					} else if (particle.name().equals("FIREWORK")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/a71baf92ffd639f71c79918f5979eef652c6371e6e1bb92ea5599990509ec2b7");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTcxYmFmOTJmZmQ2MzlmNzFjNzk5MThmNTk3OWVlZjY1MmM2MzcxZTZlMWJiOTJlYTU1OTk5OTA1MDllYzJiNyJ9fX0=");
					} else if (particle.name().equals("FISHING")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/ff75871c90b94f4fbc167e351d36e8aeae1cc2fec03b16629007f74c989de648");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmY3NTg3MWM5MGI5NGY0ZmJjMTY3ZTM1MWQzNmU4YWVhZTFjYzJmZWMwM2IxNjYyOTAwN2Y3NGM5ODlkZTY0OCJ9fX0=");
					} else if (particle.name().equals("FLAME")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/21708574e7e9a6a76c9aa8214797b1ac38950689ef9cdda18d50362398b6101d");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjE3MDg1NzRlN2U5YTZhNzZjOWFhODIxNDc5N2IxYWMzODk1MDY4OWVmOWNkZGExOGQ1MDM2MjM5OGI2MTAxZCJ9fX0=");
					} else if (particle.name().equals("FLASH")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/5f3a8c1681dd2837e2b223310cc3cc6655eed4474bb6a63bf2df5b87429debe1");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWYzYThjMTY4MWRkMjgzN2UyYjIyMzMxMGNjM2NjNjY1NWVlZDQ0NzRiYjZhNjNiZjJkZjViODc0MjlkZWJlMSJ9fX0=");
					} else if (particle.name().equals("GLOW")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/d1b11a60c643f1447fa45d22d38fd4da74617a45b4351e58472abacf0eb7cabb");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDFiMTFhNjBjNjQzZjE0NDdmYTQ1ZDIyZDM4ZmQ0ZGE3NDYxN2E0NWI0MzUxZTU4NDcyYWJhY2YwZWI3Y2FiYiJ9fX0=");
					} else if (particle.name().equals("GLOW_SQUID_INK")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/7b424dd82d8249af6668447b920b6cbc94ae14ab9410dd9d0ae3f042f50e270d");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2I0MjRkZDgyZDgyNDlhZjY2Njg0NDdiOTIwYjZjYmM5NGFlMTRhYjk0MTBkZDlkMGFlM2YwNDJmNTBlMjcwZCJ9fX0=");
					} else if (particle.name().equals("GUST") || particle.name().equals("GUST_EMITTER_LARGE") || particle.name().equals("GUST_EMITTER_SMALL")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/1b24d578daf1e8624b62cd647864522a26bfcdc02bac1102f9c1d9d82d7b25d2");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWIyNGQ1NzhkYWYxZTg2MjRiNjJjZDY0Nzg2NDUyMmEyNmJmY2RjMDJiYWMxMTAyZjljMWQ5ZDgyZDdiMjVkMiJ9fX0=");
					} else if (particle.name().equals("INFESTED")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/ea953869a0c53e7c5c012115c2a4303ccbff9d2fb78bad4e28d066ed16d0f91");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE5NTM4NjlhMGM1M2U3YzVjMDEyMTE1YzJhNDMwM2NjYmZmOWQyZmI3OGJhZDRlMjhkMDY2ZWQxNmQwZjkxIn19fQ==");
					} else if (particle.name().equals("INSTANT_EFFECT")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6fd77129617aa0a4f9f7b09e429847f477eb2b7696ac151f19bce48e1157fded");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmZkNzcxMjk2MTdhYTBhNGY5ZjdiMDllNDI5ODQ3ZjQ3N2ViMmI3Njk2YWMxNTFmMTliY2U0OGUxMTU3ZmRlZCJ9fX0=");
					} else if (particle.name().equals("ITEM_COBWEB")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/7960da26126bc02ae882310105c765e10694792e67716dd50bafec190febecf3");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzk2MGRhMjYxMjZiYzAyYWU4ODIzMTAxMDVjNzY1ZTEwNjk0NzkyZTY3NzE2ZGQ1MGJhZmVjMTkwZmViZWNmMyJ9fX0=");
					} else if (particle.name().equals("ITEM_SLIME")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/c98665bf1702c68db939f7bfe885021fb73a0817d91af43830293f7162a5f901");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk4NjY1YmYxNzAyYzY4ZGI5MzlmN2JmZTg4NTAyMWZiNzNhMDgxN2Q5MWFmNDM4MzAyOTNmNzE2MmE1ZjkwMSJ9fX0=");
					} else if (particle.name().equals("ITEM_SNOWBALL")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/1dfd7724c69a024dcfc60b16e00334ab5738f4a92bafb8fbc76cf15322ea0293");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWRmZDc3MjRjNjlhMDI0ZGNmYzYwYjE2ZTAwMzM0YWI1NzM4ZjRhOTJiYWZiOGZiYzc2Y2YxNTMyMmVhMDI5MyJ9fX0=");
					} else if (particle.name().equals("LANDING_HONEY")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/4a7e0d55ea8051569955da138697174a0bd76190b2d58b997509737dce5fb61f");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGE3ZTBkNTVlYTgwNTE1Njk5NTVkYTEzODY5NzE3NGEwYmQ3NjE5MGIyZDU4Yjk5NzUwOTczN2RjZTVmYjYxZiJ9fX0=");
					} else if (particle.name().equals("LANDING_LAVA")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/a4b93f41532fc27ef7df933c181ac3166d56037d5c5ff75d2e85afe37ca257d3");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTRiOTNmNDE1MzJmYzI3ZWY3ZGY5MzNjMTgxYWMzMTY2ZDU2MDM3ZDVjNWZmNzVkMmU4NWFmZTM3Y2EyNTdkMyJ9fX0=");
					} else if (particle.name().equals("LANDING_OBSIDIAN_TEAR")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6426ab888298adf1effd811c8074def09780e7d9d12ba4c77b73fda9982dd0fe");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQyNmFiODg4Mjk4YWRmMWVmZmQ4MTFjODA3NGRlZjA5NzgwZTdkOWQxMmJhNGM3N2I3M2ZkYTk5ODJkZDBmZSJ9fX0=");
					} else if (particle.name().equals("LARGE_SMOKE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/1d057965c99a69039e4054d8d3db4b3d88504065ce4f7652def02603f2fb8e41");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQwNTc5NjVjOTlhNjkwMzllNDA1NGQ4ZDNkYjRiM2Q4ODUwNDA2NWNlNGY3NjUyZGVmMDI2MDNmMmZiOGU0MSJ9fX0=");
					} else if (particle.name().equals("LAVA")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6ee23579dbceb451dbf37b86a626fe671cf595f3e1e5c78faa3e77cd11da270b");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmVlMjM1NzlkYmNlYjQ1MWRiZjM3Yjg2YTYyNmZlNjcxY2Y1OTVmM2UxZTVjNzhmYWEzZTc3Y2QxMWRhMjcwYiJ9fX0=");
					} else if (particle.name().equals("MYCELIUM")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/7eb4c41f481e816cf4b507b0a17595f2ba1f24664dc432be347d4e7a4eb3");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ViNGM0MWY0ODFlODE2Y2Y0YjUwN2IwYTE3NTk1ZjJiYTFmMjQ2NjRkYzQzMmJlMzQ3ZDRlN2E0ZWIzIn19fQ==");
					} else if (particle.name().equals("NAUTILUS")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/32805e4a7982e706ebd2ea6c8209402cd7c1cf9cc08b7aace3a8af7718cca7dc");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI4MDVlNGE3OTgyZTcwNmViZDJlYTZjODIwOTQwMmNkN2MxY2Y5Y2MwOGI3YWFjZTNhOGFmNzcxOGNjYTdkYyJ9fX0=");
					} else if (particle.name().equals("NOTE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/e82b0b7c68e88800030e674522aab40396fa543072b949c0600e66c2ed352ff0");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTgyYjBiN2M2OGU4ODgwMDAzMGU2NzQ1MjJhYWI0MDM5NmZhNTQzMDcyYjk0OWMwNjAwZTY2YzJlZDM1MmZmMCJ9fX0=");
					} else if (particle.name().equals("OMINOUS_SPAWNING")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/70baa9218f78b78d73ebf87217c042bf43d34dba3d4c4ccf1d33f612456115f0");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzBiYWE5MjE4Zjc4Yjc4ZDczZWJmODcyMTdjMDQyYmY0M2QzNGRiYTNkNGM0Y2NmMWQzM2Y2MTI0NTYxMTVmMCJ9fX0=");
					} else if (particle.name().equals("POOF")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/1b24d578daf1e8624b62cd647864522a26bfcdc02bac1102f9c1d9d82d7b25d2");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWIyNGQ1NzhkYWYxZTg2MjRiNjJjZDY0Nzg2NDUyMmEyNmJmY2RjMDJiYWMxMTAyZjljMWQ5ZDgyZDdiMjVkMiJ9fX0=");
					} else if (particle.name().equals("PORTAL")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/2c915db3fc40a79b63c2c453f0c490981e5227c5027501283272138533dea519");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmM5MTVkYjNmYzQwYTc5YjYzYzJjNDUzZjBjNDkwOTgxZTUyMjdjNTAyNzUwMTI4MzI3MjEzODUzM2RlYTUxOSJ9fX0=");
					} else if (particle.name().equals("RAID_OMEN")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/34543e3dfb5048b43ca3c3abcf0df4b3fcfb17a99854d76cfaa67a661f564fd9");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ1NDNlM2RmYjUwNDhiNDNjYTNjM2FiY2YwZGY0YjNmY2ZiMTdhOTk4NTRkNzZjZmFhNjdhNjYxZjU2NGZkOSJ9fX0=");
					} else if (particle.name().equals("RAIN")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/df0d96336ffcc7f12a8a8f5e3bd258ec2d8037ba24a435917c3d531b216de6d7");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGYwZDk2MzM2ZmZjYzdmMTJhOGE4ZjVlM2JkMjU4ZWMyZDgwMzdiYTI0YTQzNTkxN2MzZDUzMWIyMTZkZTZkNyJ9fX0=");
					} else if (particle.name().equals("REVERSE_PORTAL")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/2f920986c572389a23c8ac0471b88df44a4776803be6444f5ff0b9d1552c274e");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmY5MjA5ODZjNTcyMzg5YTIzYzhhYzA0NzFiODhkZjQ0YTQ3NzY4MDNiZTY0NDRmNWZmMGI5ZDE1NTJjMjc0ZSJ9fX0=");
					} else if (particle.name().equals("SCULK_CHARGE_POP")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/57bc418359ddf397d3bc291e51bdd231e1624b88c542ad132f0aebd2bf4a6e54");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTdiYzQxODM1OWRkZjM5N2QzYmMyOTFlNTFiZGQyMzFlMTYyNGI4OGM1NDJhZDEzMmYwYWViZDJiZjRhNmU1NCJ9fX0=");
					} else if (particle.name().equals("SCULK_SOUL")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/3eea90cbcc2a2021892a56fa9aa5f797e6e43d98c406f8d6b52c69ba3f463be0");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VlYTkwY2JjYzJhMjAyMTg5MmE1NmZhOWFhNWY3OTdlNmU0M2Q5OGM0MDZmOGQ2YjUyYzY5YmEzZjQ2M2JlMCJ9fX0=");
					} else if (particle.name().equals("SMALL_FLAME")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/bb58b83f07618ea79a1a1202b5a77b14df1c8e35b6e1deb8a26d8976f85360c3");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI1OGI4M2YwNzYxOGVhNzlhMWExMjAyYjVhNzdiMTRkZjFjOGUzNWI2ZTFkZWI4YTI2ZDg5NzZmODUzNjBjMyJ9fX0=");
					} else if (particle.name().equals("SMALL_GUST")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/1b24d578daf1e8624b62cd647864522a26bfcdc02bac1102f9c1d9d82d7b25d2");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWIyNGQ1NzhkYWYxZTg2MjRiNjJjZDY0Nzg2NDUyMmEyNmJmY2RjMDJiYWMxMTAyZjljMWQ5ZDgyZDdiMjVkMiJ9fX0=");
					} else if (particle.name().equals("SMOKE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/1d057965c99a69039e4054d8d3db4b3d88504065ce4f7652def02603f2fb8e41");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQwNTc5NjVjOTlhNjkwMzllNDA1NGQ4ZDNkYjRiM2Q4ODUwNDA2NWNlNGY3NjUyZGVmMDI2MDNmMmZiOGU0MSJ9fX0=");
					} else if (particle.name().equals("SNOWFLAKE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/e115c7968ec3771ee9ff6ae6bca2d5ba3962aa727a4fa8d37608e4c9bf1512bb");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTExNWM3OTY4ZWMzNzcxZWU5ZmY2YWU2YmNhMmQ1YmEzOTYyYWE3MjdhNGZhOGQzNzYwOGU0YzliZjE1MTJiYiJ9fX0=");
					} else if (particle.name().equals("SONIC_BOOM")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/9f02dc2fa1a220321376b5d5384f4adb392b1f7b610b638eab5c41611a744b71");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWYwMmRjMmZhMWEyMjAzMjEzNzZiNWQ1Mzg0ZjRhZGIzOTJiMWY3YjYxMGI2MzhlYWI1YzQxNjExYTc0NGI3MSJ9fX0=");
					} else if (particle.name().equals("SOUL")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/54b8f3dbea08b3640555729c32d7461aa49ee229b9d6bb9ba3c907fdc45fa952");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTRiOGYzZGJlYTA4YjM2NDA1NTU3MjljMzJkNzQ2MWFhNDllZTIyOWI5ZDZiYjliYTNjOTA3ZmRjNDVmYTk1MiJ9fX0=");
					} else if (particle.name().equals("SOUL_FIRE_FLAME")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/d20cb73f207b07d259ca1eef7cade302c7ea104b8a0c240b897a577971122e56");
						if (!section.contains("skin-value"))
							section.set("skin-value", "https://textures.minecraft.net/texture/d20cb73f207b07d259ca1eef7cade302c7ea104b8a0c240b897a577971122e56");
					} else if (particle.name().equals("SPIT")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/92b31239520511ca7b6712ef0ecfb55b6c56b9347240f4cbf9925ce0bf0fa445");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTJiMzEyMzk1MjA1MTFjYTdiNjcxMmVmMGVjZmI1NWI2YzU2YjkzNDcyNDBmNGNiZjk5MjVjZTBiZjBmYTQ0NSJ9fX0=");
					} else if (particle.name().equals("SPLASH")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6f5cb401239a3410632c1bf040c9b6b2492d2819b88a3632f1de3e0ceec08812");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmY1Y2I0MDEyMzlhMzQxMDYzMmMxYmYwNDBjOWI2YjI0OTJkMjgxOWI4OGEzNjMyZjFkZTNlMGNlZWMwODgxMiJ9fX0=");
					} else if (particle.name().equals("SPORE_BLOSSOM_AIR")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/195f349cc4c017d2ad2a5300da87a43f7960fdf16ef3e05ad44ac22063a0c270");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTk1ZjM0OWNjNGMwMTdkMmFkMmE1MzAwZGE4N2E0M2Y3OTYwZmRmMTZlZjNlMDVhZDQ0YWMyMjA2M2EwYzI3MCJ9fX0=");
					} else if (particle.name().equals("SQUID_INK")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/938a42a80c70b243643ee5015e9d5149af54ade83ff55c2179f8a8b3b10805f6");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTM4YTQyYTgwYzcwYjI0MzY0M2VlNTAxNWU5ZDUxNDlhZjU0YWRlODNmZjU1YzIxNzlmOGE4YjNiMTA4MDVmNiJ9fX0=");
					} else if (particle.name().equals("SWEEP_ATTACK")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/7e40ddae351c6674b88d6c6c978d188e4bbe5694b25c12985f4d0976f0690e5e");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2U0MGRkYWUzNTFjNjY3NGI4OGQ2YzZjOTc4ZDE4OGU0YmJlNTY5NGIyNWMxMjk4NWY0ZDA5NzZmMDY5MGU1ZSJ9fX0=");
					} else if (particle.name().equals("TOTEM_OF_UNDYING")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/2bea24d751d25ecf29eed1159bc4f91903ab0eeab4da0675e1a986501f9159b9");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmJlYTI0ZDc1MWQyNWVjZjI5ZWVkMTE1OWJjNGY5MTkwM2FiMGVlYWI0ZGEwNjc1ZTFhOTg2NTAxZjkxNTliOSJ9fX0=");
					} else if (particle.name().equals("TRIAL_OMEN")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/cfe2f511677d39c43ea376512b69212e45e810dc9cf966dbcc8980d9e0167674");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2ZlMmY1MTE2NzdkMzljNDNlYTM3NjUxMmI2OTIxMmU0NWU4MTBkYzljZjk2NmRiY2M4OTgwZDllMDE2NzY3NCJ9fX0=");
					} else if (particle.name().equals("TRIAL_SPAWNER_DETECTION")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/c327758fb3852b874e12a31d90b6ccaf77c7de8215ab843703c76682544b607");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzMyNzc1OGZiMzg1MmI4NzRlMTJhMzFkOTBiNmNjYWY3N2M3ZGU4MjE1YWI4NDM3MDNjNzY2ODI1NDRiNjA3In19fQ==");
					} else if (particle.name().equals("TRIAL_SPAWNER_DETECTION_OMINOUS")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/76d126affd03def502bfaa91a34e7c1562421490002a85c2b5815bdd4248e12");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzZkMTI2YWZmZDAzZGVmNTAyYmZhYTkxYTM0ZTdjMTU2MjQyMTQ5MDAwMmE4NWMyYjU4MTViZGQ0MjQ4ZTEyIn19fQ==");
					} else if (particle.name().equals("UNDERWATER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/3725c8adb9fe6b34b48740a130cec44b2825fe32dad198570050ee4b4edadf33");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzcyNWM4YWRiOWZlNmIzNGI0ODc0MGExMzBjZWM0NGIyODI1ZmUzMmRhZDE5ODU3MDA1MGVlNGI0ZWRhZGYzMyJ9fX0=");
					} else if (particle.name().equals("VAULT_CONNECTION")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/334375719d19ef4ca29fd0b2baaadb9ad4c8de18e8791fc2413b2b9f4b54f5e0");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzM0Mzc1NzE5ZDE5ZWY0Y2EyOWZkMGIyYmFhYWRiOWFkNGM4ZGUxOGU4NzkxZmMyNDEzYjJiOWY0YjU0ZjVlMCJ9fX0=");
					} else if (particle.name().equals("VIBRATION")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/f8c211d66c803aac15ab86f79c7edfd6c3b2034d23355a92f6bd42e835260be0");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjhjMjExZDY2YzgwM2FhYzE1YWI4NmY3OWM3ZWRmZDZjM2IyMDM0ZDIzMzU1YTkyZjZiZDQyZTgzNTI2MGJlMCJ9fX0=");
					} else if (particle.name().equals("WARPED_SPORE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/e257972cde33e8eba83efa5e84095d7650a45c0a08a851a557c82819018a560a");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTI1Nzk3MmNkZTMzZThlYmE4M2VmYTVlODQwOTVkNzY1MGE0NWMwYTA4YTg1MWE1NTdjODI4MTkwMThhNTYwYSJ9fX0=");
					} else if (particle.name().equals("WAX_OFF")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/69fbe9bf6618cab10cfc5e1f4952da88c752607d823212fd36b9f0366a4c");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjlmYmU5YmY2NjE4Y2FiMTBjZmM1ZTFmNDk1MmRhODhjNzUyNjA3ZDgyMzIxMmZkMzZiOWYwMzY2YTRjIn19fQ==");
					} else if (particle.name().equals("WAX_ON")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/db2ea2742c31ef7ff5e5e83cafa9c72c5d172e88d7c8d0dc1eac2eea5c1cc");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGIyZWEyNzQyYzMxZWY3ZmY1ZTVlODNjYWZhOWM3MmM1ZDE3MmU4OGQ3YzhkMGRjMWVhYzJlZWE1YzFjYyJ9fX0=");
					} else if (particle.name().equals("WHITE_ASH")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/e900b1c6f390e6cc11ae17355de8056f1b8770c5ec8be43615a5175f1bfa91c9");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTkwMGIxYzZmMzkwZTZjYzExYWUxNzM1NWRlODA1NmYxYjg3NzBjNWVjOGJlNDM2MTVhNTE3NWYxYmZhOTFjOSJ9fX0=");
					} else if (particle.name().equals("WHITE_SMOKE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/ce3acc78b624c070849184e0a266d9e99aa671fb3e38d66c2c3c5191194793");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2UzYWNjNzhiNjI0YzA3MDg0OTE4NGUwYTI2NmQ5ZTk5YWE2NzFmYjNlMzhkNjZjMmMzYzUxOTExOTQ3OTMifX19");
					} else if (particle.name().equals("WITCH")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/8aa986a6e1c2d88ff198ab2c3259e8d2674cb83a6d206f883bad2c8ada819");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhOTg2YTZlMWMyZDg4ZmYxOThhYjJjMzI1OWU4ZDI2NzRjYjgzYTZkMjA2Zjg4M2JhZDJjOGFkYTgxOSJ9fX0=");
					} else if (particle.name().equals("EFFECT")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/3ef2432ef305361384d4318df5bda5bd1ac2d9bea06d1f5cfead6dd87e37ddf5");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VmMjQzMmVmMzA1MzYxMzg0ZDQzMThkZjViZGE1YmQxYWMyZDliZWEwNmQxZjVjZmVhZDZkZDg3ZTM3ZGRmNSJ9fX0=");
					} else if (particle.name().equals("HAPPY_VILLAGER")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/5e5dbc49e8188da1b936072d45bc7c13b42a0a4c0624b21058aa3f5283955b8e");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU1ZGJjNDllODE4OGRhMWI5MzYwNzJkNDViYzdjMTNiNDJhMGE0YzA2MjRiMjEwNThhYTNmNTI4Mzk1NWI4ZSJ9fX0=");
					} else if (particle.name().equals("ENTITY_EFFECT")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/67844a5b74f341039560f280db1fcdf836e3b6a48dc2a09351937626977e1c2");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjc4NDRhNWI3NGYzNDEwMzk1NjBmMjgwZGIxZmNkZjgzNmUzYjZhNDhkYzJhMDkzNTE5Mzc2MjY5NzdlMWMyIn19fQ==");
					} else if (particle.name().equals("SNEEZE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/6a463d2460b0f50a5ca8e6db53738ea25dd216706069e296cf64cd3371cf9a31");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmE0NjNkMjQ2MGIwZjUwYTVjYThlNmRiNTM3MzhlYTI1ZGQyMTY3MDYwNjllMjk2Y2Y2NGNkMzM3MWNmOWEzMSJ9fX0=");
					} else if (particle.name().equals("SCRAPE")) {
						section.set("textures-url", "https://textures.minecraft.net/texture/3299ec4d18a080034328b667efb95d7093d19b5be1ac91b7000df0f15eddf80a");
						if (!section.contains("skin-value"))
							section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI5OWVjNGQxOGEwODAwMzQzMjhiNjY3ZWZiOTVkNzA5M2QxOWI1YmUxYWM5MWI3MDAwZGYwZjE1ZWRkZjgwYSJ9fX0=");
					}
						/*else if (particle.name().equals("")) {
							section.set("textures-url", "");
							if (!section.contains("skin-value"))
								section.set("skin-value", "");
						} */
					else {
						section.set("textures-url", "https://textures.minecraft.net/texture/fc271052719ef64079ee8c1498951238a74dac4c27b95640db6fbddc2d6b5b6e");
					}

					if (!section.contains("skin-value")) {
						section.set("skin-value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmMyNzEwNTI3MTllZjY0MDc5ZWU4YzE0OTg5NTEyMzhhNzRkYWM0YzI3Yjk1NjQwZGI2ZmJkZGMyZDZiNWI2ZSJ9fX0=");
					}
				}
			}
		}

		config.set("version", latestVersion);
		config.setInlineComments("version", Collections.singletonList("Don't change this, used for automatic config updates"));
		saveConfig();
		reloadConfig();
	}

	public boolean loadVault() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null) {
			this.permissions = permissionProvider.getProvider();
		} else {
			messagingUtil.sendMessage(Bukkit.getConsoleSender(), "[RPGHorses] Failed to hook into &cVault&r you're missing a permissions plugin");
		}

		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			this.economy = economyProvider.getProvider();
		} else {
			messagingUtil.sendMessage(Bukkit.getConsoleSender(), "[RPGHorses] Failed to hook into &cVault&r you're missing an economy plugin");
		}

		return this.permissions != null && this.economy != null;
	}

	public void initializeVariables() {
		this.updateNotifier = new UpdateNotifier(this, messagingUtil, 76836);

		this.itemUtil = new ItemUtil(this);
		this.horseCrateManager = new HorseCrateManager(this);
		if (plugin.getConfig().getBoolean("mysql.enabled", false)) this.sqlManager = new SQLManager(this);
		this.horseOwnerManager = new HorseOwnerManager(this, this.horseCrateManager, this.playerConfigs);
		this.rpgHorseManager = new RPGHorseManager(this, this.horseOwnerManager);
		this.stableGuiManager = new StableGUIManager(this, this.horseOwnerManager, this.rpgHorseManager, this.itemUtil, this.messagingUtil);
		this.marketGUIManager = new MarketGUIManager(this, this.horseOwnerManager, this.marketConfig, this.itemUtil);
		this.horseGUIManager = new HorseGUIManager(this, this.horseOwnerManager, this.stableGuiManager);
		this.messageQueuer = new MessageQueuer(this.playerConfigs, this.messagingUtil);
		this.horseDespawner = new HorseDespawner(this, this.horseOwnerManager, this.rpgHorseManager);
		this.particleManager = new ParticleManager(this, this.horseOwnerManager);
		this.trailGUIManager = new TrailGUIManager(this, particleManager, this.horseOwnerManager);
		this.sellGUIManager = new SellGUIManager(this, stableGuiManager, this.horseOwnerManager);
		this.xpManager = new XPManager(this, rpgHorseManager, messagingUtil);
	}

	public void registerEvents() {
		new ChunkUnloadListener(this);
		new EntityDamageByEntityListener(this, this.rpgHorseManager, this.stableGuiManager);
		new EntityDeathListener(this, this.rpgHorseManager, this.stableGuiManager, this.horseOwnerManager, xpManager, messagingUtil);
		new EntitySpawnListener(this, this.rpgHorseManager, this.horseOwnerManager);
		new InventoryClickListener(this, this.horseOwnerManager, this.rpgHorseManager, this.stableGuiManager, this.marketGUIManager, this.horseGUIManager, this.trailGUIManager, this.sellGUIManager, this.messageQueuer, this.messagingUtil);
		new InventoryCloseListener(this, this.horseOwnerManager, this.stableGuiManager);
		new PlayerChatListener(this, this.rpgHorseManager, this.stableGuiManager, this.messagingUtil);
		new PlayerInteractEntityListener(this, this.rpgHorseManager, this.messagingUtil);
		new PlayerJoinListener(this, this.horseOwnerManager, this.stableGuiManager, this.marketGUIManager, this.messagingUtil, this.messageQueuer, updateNotifier);
		new PlayerQuitListener(this, this.horseOwnerManager, xpManager);
		new PlayerTeleportListener(this, this.horseOwnerManager, this.messagingUtil);
		new PreCommandListener(this, this.rpgHorseManager, this.stableGuiManager, this.marketGUIManager, this.messagingUtil);
		new VehicleListener(this, this.rpgHorseManager, this.xpManager, this.messagingUtil);
	}

	public void loadCommands() {
		this.getCommand("rpghorses").setExecutor(new RPGHorsesCommand(this, this.horseOwnerManager, this.rpgHorseManager, this.stableGuiManager, this.marketGUIManager, this.horseCrateManager, this.particleManager, this.messagingUtil));
		this.getCommand("rpghorsesadmin").setExecutor(new RPGHorsesAdminCommand(this, this.horseOwnerManager, this.rpgHorseManager, this.stableGuiManager, this.horseGUIManager, this.sellGUIManager, this.trailGUIManager, this.horseDespawner, this.horseCrateManager, this.marketGUIManager, this.particleManager, this.messageQueuer, this.messagingUtil));
	}

	public void setupHelpMessage() {
		helpMessages.put("                                &6&lRPGHorses", "rpghorses.help");
		helpMessages.put("&6/rpghorses &8- &7Opens the stable gui", "rpghorses.stable");
		helpMessages.put("&6/{LABEL} help &8- &7Displays this message", "rpghorses.help");
		helpMessages.put("&6/rpghorses claim &8- &7Claims a wild horse as an RPG horse", "rpghorses.claim");
		helpMessages.put("&6/rpghorses market &8- &7Opens the market", "rpghorses.market");
		helpMessages.put("&6/rpghorses sell <horse-number> <> &8- &7Sells a horse to the market", "rpghorses.sell");
		helpMessages.put("&6/rpghorses buy <horse-crate> &8- &7Buys a new horse", "rpghorses.buy");
		helpMessages.put("&6/rpghorses trail <particle> &8- &7Sets the trail of your current horse", "rpghorses.trail");
		helpMessages.put("&6/rpghorsesadmin give <horse-crate> <player> &8- &7Gives a player a horse crate", "rpghorses.give");
		helpMessages.put("&6/rpghorsesadmin give <health> <movement-speed> <jump-strength> <type> [color] [style] <player> &8- &7Gives a player a RPGHorse", "rpghorses.give");
		//helpMessage.put("&6/rpghorsesadmin set <health> <movement-speed> <jump-height> <type> <color> <style> <horse-number> <player> &8- &7Modifies an existing RPGHorse");
		helpMessages.put("&6/rpghorsesadmin remove <horse-number> <player> &8- &7Removes a RPGHorse from a player's stable", "rpghorses.remove");
		helpMessages.put("&6/rpghorsesadmin upgrade <horse-number> <player> &8- &7Upgrades a player's RPGHorse", "rpghorses.upgrade");
		helpMessages.put("&6/rpghorsesadmin listall &8- &7Lists all active RPGHorses", "rpghorses.listall");
		helpMessages.put("&6/rpghorsesadmin check <radius> &8- &7Lists all active RPGHorses within a certain radius", "rpghorses.check");
		helpMessages.put("&6/rpghorsesadmin removenear <radius> &8- &7Removes all RPGHorses within a certain radius", "rpghorses.removenear");
		helpMessages.put("&6/rpghorsesadmin togglehorse <horse-number> <player> &8- &7Toggles a players horse on/off", "rpghorses.togglehorse");
		helpMessages.put("&6/rpghorsesadmin removeall <player> &8- &7Removes all of players horses", "rpghorses.removeall");
		helpMessages.put("&6/rpghorsesadmin forcemenu <horse-number> <player> <menu> &8- &7Forces the player to open a specific menu", "rpghorses.forcemenu");
		helpMessages.put("&6/rpghorsesadmin reload &8- &7Reloads the configuration file", "rpghorses.reload");
	}

	@Override
	public void onDisable() {
		this.horseDespawner.despawnAllRPGHorses();

		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners().values()) {
			if (horseOwner.getGUILocation() != GUILocation.NONE) {
				horseOwner.getPlayer().closeInventory();
			}
		}

		this.horseOwnerManager.saveData();
		this.marketGUIManager.saveMarketHorses();

		try {
			if (sqlManager != null) sqlManager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendHelpMessage(CommandSender sender, String label) {
		label = label.toLowerCase();
		String rpgReplace = label.contains("rpg") ? "rpg" : "";
		String horsesReplace = label.contains("horses") ? "horses" : "horse";
		boolean allPerms = sender.hasPermission("rpghorses.*") || sender.isOp();
		for (String line : plugin.getHelpMessages().keySet()) {
			if (allPerms || sender.hasPermission(plugin.getHelpMessages().get(line))) {
				this.messagingUtil.sendMessage(sender, line.replace("horses", horsesReplace).replace("rpg", rpgReplace).replace("{LABEL}", label));
			}
		}
	}
}
