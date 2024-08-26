package org.plugins.rpghorses;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
		int latestVersion = 4;

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
		new EntityDeathListener(this, this.rpgHorseManager, this.stableGuiManager, xpManager, messagingUtil);
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
