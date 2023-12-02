package org.plugins.rpghorses.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;

@AllArgsConstructor
@Getter
public enum HorseSkins {

	BROWN_HORSE("https://textures.minecraft.net/texture/bedf73ea12ce6bd90a4ae9a8d15096749cfe918230dc829b2581d223b1a2a8", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmVkZjczZWExMmNlNmJkOTBhNGFlOWE4ZDE1MDk2NzQ5Y2ZlOTE4MjMwZGM4MjliMjU4MWQyMjNiMWEyYTgifX19"), // https://minecraft-heads.com/custom-heads/animals/7280-brown-horse

	WHITE_HORSE("https://textures.minecraft.net/texture/60a2db2f1eb93e5978d2dc91a74df43d7b75d9ec0e694fd7f2a652fbd15", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjBhMmRiMmYxZWI5M2U1OTc4ZDJkYzkxYTc0ZGY0M2Q3Yjc1ZDllYzBlNjk0ZmQ3ZjJhNjUyZmJkMTUifX19"), // https://minecraft-heads.com/custom-heads/animals/3921-horse

	CREAMY_HORSE("http://textures.minecraft.net/texture/628d1ab4be1e28b7b461fdea46381ac363a7e5c3591c9e5d2683fbe1ec9fcd3", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjI4ZDFhYjRiZTFlMjhiN2I0NjFmZGVhNDYzODFhYzM2M2E3ZTVjMzU5MWM5ZTVkMjY4M2ZiZTFlYzlmY2QzIn19fQ=="), // https://minecraft-heads.com/custom-heads/animals/3920-horse

	CHESTNUT_HORSE("https://textures.minecraft.net/texture/b66b2b32d31539c7383d923bae4faaf65da6715cd526c35d2e4e6825da11fb", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjY2YjJiMzJkMzE1MzljNzM4M2Q5MjNiYWU0ZmFhZjY1ZGE2NzE1Y2Q1MjZjMzVkMmU0ZTY4MjVkYTExZmIifX19"), // https://minecraft-heads.com/custom-heads/animals/3919-horse

	BLACK_HORSE("https://textures.minecraft.net/texture/3efb0b9857d7c8d295f6df97b605f40b9d07ebe128a6783d1fa3e1bc6e44117", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjRiN2ZjNWY3YTlkZGZkZDFhYTc5MzE3NDEwZmMxOTI5ZjkxYmRkZjk4NTg1OTM4YTJhNTYxOTlhNjMzY2MifX19"), // https://minecraft-heads.com/custom-heads/animals/38013-horse-black

	GRAY_HORSE("https://textures.minecraft.net/texture/d6676c4d6f0f5ed606a356f3cc5a29d14aafe65721ba1a1a95c5ac4c5e239e5", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDY2NzZjNGQ2ZjBmNWVkNjA2YTM1NmYzY2M1YTI5ZDE0YWFmZTY1NzIxYmExYTFhOTVjNWFjNGM1ZTIzOWU1In19fQ=="), // https://minecraft-heads.com/custom-heads/animals/7278-gray-horse

	DARK_BROWN_HORSE("https://textures.minecraft.net/texture/2661f23fb76624ffbabbda31ca4a38b404fe63ef37d4ba4e4c5441a21e3a6", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjY2MWYyM2ZiNzY2MjRmZmJhYmJkYTMxY2E0YTM4YjQwNGZlNjNlZjM3ZDRiYTRlNGM1NDQxYTIxZTNhNiJ9fX0="), // https://minecraft-heads.com/custom-heads/animals/7279-dark-brown-horse

	DONKEY("https://textures.minecraft.net/texture/399bb50d1a214c394917e25bb3f2e20698bf98ca703e4cc08b42462df309d6e6", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGZiNmMzYzA1MmNmNzg3ZDIzNmEyOTE1ZjgwNzJiNzdjNTQ3NDk3NzE1ZDFkMmY4Y2JjOWQyNDFkODhhIn19fQ=="), // https://minecraft-heads.com/custom-heads/animals/38017-donkey

	MULE("https://textures.minecraft.net/texture/46dcda265e57e4f51b145aacbf5b59bdc6099ffd3cce0a661b2c0065d80930d8", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTA0ODZhNzQyZTdkZGEwYmFlNjFjZTJmNTVmYTEzNTI3ZjFjM2IzMzRjNTdjMDM0YmI0Y2YxMzJmYjVmNWYifX19"), // https://minecraft-heads.com/custom-heads/animals/38016-mule

	UNDEAD_HORSE("https://textures.minecraft.net/texture/d22950f2d3efddb18de86f8f55ac518dce73f12a6e0f8636d551d8eb480ceec", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDIyOTUwZjJkM2VmZGRiMThkZTg2ZjhmNTVhYzUxOGRjZTczZjEyYTZlMGY4NjM2ZDU1MWQ4ZWI0ODBjZWVjIn19fQ=="), // https://minecraft-heads.com/custom-heads/animals/2913-zombie-horse

	ZOMBIE_HORSE("https://textures.minecraft.net/texture/d22950f2d3efddb18de86f8f55ac518dce73f12a6e0f8636d551d8eb480ceec", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDIyOTUwZjJkM2VmZGRiMThkZTg2ZjhmNTVhYzUxOGRjZTczZjEyYTZlMGY4NjM2ZDU1MWQ4ZWI0ODBjZWVjIn19fQ=="), // https://minecraft-heads.com/custom-heads/animals/2913-zombie-horse

	SKELETON_HORSE("https://textures.minecraft.net/texture/47effce35132c86ff72bcae77dfbb1d22587e94df3cbc2570ed17cf8973a", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDdlZmZjZTM1MTMyYzg2ZmY3MmJjYWU3N2RmYmIxZDIyNTg3ZTk0ZGYzY2JjMjU3MGVkMTdjZjg5NzNhIn19fQ=="), // https://minecraft-heads.com/custom-heads/animals/6013-skeleton-horse

	BROWN_LLAMA("https://textures.minecraft.net/texture/818cd457fbaf327fa39f10b5b36166fd018264036865164c02d9e5ff53f45", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODE4Y2Q0NTdmYmFmMzI3ZmEzOWYxMGI1YjM2MTY2ZmQwMTgyNjQwMzY4NjUxNjRjMDJkOWU1ZmY1M2Y0NSJ9fX0="), // https://minecraft-heads.com/custom-heads/animals/6822-llama-brown

	CREAMY_LLAMA("https://textures.minecraft.net/texture/2a5f10e6e6232f182fe966f501f1c3799d45ae19031a1e4941b5dee0feff059b", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmE1ZjEwZTZlNjIzMmYxODJmZTk2NmY1MDFmMWMzNzk5ZDQ1YWUxOTAzMWExZTQ5NDFiNWRlZTBmZWZmMDU5YiJ9fX0="), // https://minecraft-heads.com/custom-heads/animals/26964-llama-creamy

	WHITE_LLAMA("https://textures.minecraft.net/texture/83d9b5915912ffc2b85761d6adcb428a812f9b83ff634e331162ce46c99e9", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNkOWI1OTE1OTEyZmZjMmI4NTc2MWQ2YWRjYjQyOGE4MTJmOWI4M2ZmNjM0ZTMzMTE2MmNlNDZjOTllOSJ9fX0="), // https://minecraft-heads.com/custom-heads/animals/3931-llama-white

	GRAY_LLAMA("https://textures.minecraft.net/texture/cf24e56fd9ffd7133da6d1f3e2f455952b1da462686f753c597ee82299a", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2YyNGU1NmZkOWZmZDcxMzNkYTZkMWYzZTJmNDU1OTUyYjFkYTQ2MjY4NmY3NTNjNTk3ZWU4MjI5OWEifX19"), // https://minecraft-heads.com/custom-heads/animals/3930-llama-light-gray

	HORSE_EGG("https://textures.minecraft.net/texture/5c6d5abbf68ccb2386bf16af25ac38d8b77bb0e043152461bd97f3f630dbb8bc", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWM2ZDVhYmJmNjhjY2IyMzg2YmYxNmFmMjVhYzM4ZDhiNzdiYjBlMDQzMTUyNDYxYmQ5N2YzZjYzMGRiYjhiYyJ9fX0="), // https://minecraft-heads.com/custom-heads/decoration/936-spawn-egg-horse

	DONKEY_EGG("https://textures.minecraft.net/texture/db522f6d77c0696c9d1f2ad49bfa3cb8205a5e623af1c420bd740dc471914e97", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGI1MjJmNmQ3N2MwNjk2YzlkMWYyYWQ0OWJmYTNjYjgyMDVhNWU2MjNhZjFjNDIwYmQ3NDBkYzQ3MTkxNGU5NyJ9fX0="), // https://minecraft-heads.com/custom-heads/decoration/23905-spawn-egg-donkey

	MULE_EGG("https://textures.minecraft.net/texture/e4ad78f7ada7c6376449ef949c9c87fdece882b5a2f14cfbf8eac6fea657f4c7", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTRhZDc4ZjdhZGE3YzYzNzY0NDllZjk0OWM5Yzg3ZmRlY2U4ODJiNWEyZjE0Y2ZiZjhlYWM2ZmVhNjU3ZjRjNyJ9fX0="), // https://minecraft-heads.com/custom-heads/decoration/23967-spawn-egg-mule

	UNDEAD_HORSE_EGG("https://textures.minecraft.net/texture/ec5b6f8ef1d75f73a5290c9367d2b9b823bc963de2a366fd6550bcace2751205", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWM1YjZmOGVmMWQ3NWY3M2E1MjkwYzkzNjdkMmI5YjgyM2JjOTYzZGUyYTM2NmZkNjU1MGJjYWNlMjc1MTIwNSJ9fX0="), // https://minecraft-heads.com/custom-heads/decoration/24661-spawn-egg-zombie-horse

	ZOMBIE_HORSE_EGG("https://textures.minecraft.net/texture/ec5b6f8ef1d75f73a5290c9367d2b9b823bc963de2a366fd6550bcace2751205", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWM1YjZmOGVmMWQ3NWY3M2E1MjkwYzkzNjdkMmI5YjgyM2JjOTYzZGUyYTM2NmZkNjU1MGJjYWNlMjc1MTIwNSJ9fX0="), // https://minecraft-heads.com/custom-heads/decoration/24661-spawn-egg-zombie-horse

	SKELETON_HORSE_EGG("https://textures.minecraft.net/texture/9dc084b7874268973006c897a03d8906cc9b3df8c39bce93d87ec0df507bbe0d", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWRjMDg0Yjc4NzQyNjg5NzMwMDZjODk3YTAzZDg5MDZjYzliM2RmOGMzOWJjZTkzZDg3ZWMwZGY1MDdiYmUwZCJ9fX0="), // https://minecraft-heads.com/custom-heads/decoration/24268-spawn-egg-skeleton-horse

	LLAMA_EGG("https://textures.minecraft.net/texture/5cbc6bd92728d79cfa6d8f23cbae9d912f495920b9e95ef691a1967fef8a4453", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWNiYzZiZDkyNzI4ZDc5Y2ZhNmQ4ZjIzY2JhZTlkOTEyZjQ5NTkyMGI5ZTk1ZWY2OTFhMTk2N2ZlZjhhNDQ1MyJ9fX0="); // https://minecraft-heads.com/custom-heads/decoration/23709-spawn-egg-llama

	private final String texturesURL, legacySkinValue;

	public static ItemStack applySkin(RPGHorse rpgHorse, ItemStack item) {
		String type;
		if (RPGHorsesMain.getVersion().getWeight() < 11) {
			type = ((LegacyHorseInfo) rpgHorse.getHorseInfo()).getVariant().name();
		} else {
			type = rpgHorse.getEntityType().name();
		}

		HorseSkins skinValue;
		if (rpgHorse.isDead()) {
			skinValue = getDeadSkin(type);
			if (skinValue == null) {
				skinValue = HorseSkins.HORSE_EGG;
			}
		} else {
			if (type.equals("HORSE") || type.equals("LLAMA")) {
				type = rpgHorse.getColor().name() + "_" + type;
			}
			skinValue = getAliveSkin(type);
			if (skinValue == null) {
				if (type.endsWith("HORSE")) {
					skinValue = HorseSkins.BROWN_HORSE;
				} else if (type.endsWith("LLAMA")) {
					skinValue = HorseSkins.WHITE_LLAMA;
				}
			}
		}

		item.setItemMeta(ItemUtil.applyCustomHead(item.getItemMeta(), skinValue.getTexturesURL(), skinValue.getLegacySkinValue()));
		return item;
	}

	public static HorseSkins getAliveSkin(String key) {
		try {
			return HorseSkins.valueOf(key);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static HorseSkins getDeadSkin(String key) {
		try {
			return HorseSkins.valueOf(key + "_EGG");
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

}
