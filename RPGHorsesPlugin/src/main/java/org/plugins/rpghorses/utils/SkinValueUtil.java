package org.plugins.rpghorses.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import roryslibrary.util.ItemUtil;

import java.util.HashMap;

public class SkinValueUtil {
	
	private static HashMap<String, String> aliveSkullMap = new HashMap<String, String>() {{
		put("BROWN_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmVkZjczZWExMmNlNmJkOTBhNGFlOWE4ZDE1MDk2NzQ5Y2ZlOTE4MjMwZGM4MjliMjU4MWQyMjNiMWEyYTgifX19");
		put("WHITE_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjBhMmRiMmYxZWI5M2U1OTc4ZDJkYzkxYTc0ZGY0M2Q3Yjc1ZDllYzBlNjk0ZmQ3ZjJhNjUyZmJkMTUifX19");
		put("CREAMY_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjI4ZDFhYjRiZTFlMjhiN2I0NjFmZGVhNDYzODFhYzM2M2E3ZTVjMzU5MWM5ZTVkMjY4M2ZiZTFlYzlmY2QzIn19fQ==");
		put("CHESTNUT_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjY2YjJiMzJkMzE1MzljNzM4M2Q5MjNiYWU0ZmFhZjY1ZGE2NzE1Y2Q1MjZjMzVkMmU0ZTY4MjVkYTExZmIifX19");
		put("BLACK_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjRiN2ZjNWY3YTlkZGZkZDFhYTc5MzE3NDEwZmMxOTI5ZjkxYmRkZjk4NTg1OTM4YTJhNTYxOTlhNjMzY2MifX19");
		put("GRAY_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDY2NzZjNGQ2ZjBmNWVkNjA2YTM1NmYzY2M1YTI5ZDE0YWFmZTY1NzIxYmExYTFhOTVjNWFjNGM1ZTIzOWU1In19fQ==");
		put("DARK_BROWN_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjY2MWYyM2ZiNzY2MjRmZmJhYmJkYTMxY2E0YTM4YjQwNGZlNjNlZjM3ZDRiYTRlNGM1NDQxYTIxZTNhNiJ9fX0=");
		
		put("DONKEY", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGZiNmMzYzA1MmNmNzg3ZDIzNmEyOTE1ZjgwNzJiNzdjNTQ3NDk3NzE1ZDFkMmY4Y2JjOWQyNDFkODhhIn19fQ==");
		
		put("MULE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTA0ODZhNzQyZTdkZGEwYmFlNjFjZTJmNTVmYTEzNTI3ZjFjM2IzMzRjNTdjMDM0YmI0Y2YxMzJmYjVmNWYifX19");
		
		put("UNDEAD_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDIyOTUwZjJkM2VmZGRiMThkZTg2ZjhmNTVhYzUxOGRjZTczZjEyYTZlMGY4NjM2ZDU1MWQ4ZWI0ODBjZWVjIn19fQ==");
		
		put("ZOMBIE_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDIyOTUwZjJkM2VmZGRiMThkZTg2ZjhmNTVhYzUxOGRjZTczZjEyYTZlMGY4NjM2ZDU1MWQ4ZWI0ODBjZWVjIn19fQ==");
		
		put("SKELETON_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDdlZmZjZTM1MTMyYzg2ZmY3MmJjYWU3N2RmYmIxZDIyNTg3ZTk0ZGYzY2JjMjU3MGVkMTdjZjg5NzNhIn19fQ==");
		
		put("BROWN_LLAMA", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODE4Y2Q0NTdmYmFmMzI3ZmEzOWYxMGI1YjM2MTY2ZmQwMTgyNjQwMzY4NjUxNjRjMDJkOWU1ZmY1M2Y0NSJ9fX0="); // https://minecraft-heads.com/custom-heads/animals/6822-llama-brown
		put("CREAMY_LLAMA", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmE1ZjEwZTZlNjIzMmYxODJmZTk2NmY1MDFmMWMzNzk5ZDQ1YWUxOTAzMWExZTQ5NDFiNWRlZTBmZWZmMDU5YiJ9fX0="); // https://minecraft-heads.com/custom-heads/animals/26964-llama-creamy
		put("WHITE_LLAMA", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNkOWI1OTE1OTEyZmZjMmI4NTc2MWQ2YWRjYjQyOGE4MTJmOWI4M2ZmNjM0ZTMzMTE2MmNlNDZjOTllOSJ9fX0="); // https://minecraft-heads.com/custom-heads/animals/3931-llama-white
		put("GRAY_LLAMA", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2YyNGU1NmZkOWZmZDcxMzNkYTZkMWYzZTJmNDU1OTUyYjFkYTQ2MjY4NmY3NTNjNTk3ZWU4MjI5OWEifX19"); // https://minecraft-heads.com/custom-heads/animals/3930-llama-light-gray
	}}, deadSkullMap = new HashMap<String, String>() {{
		put("HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWM2ZDVhYmJmNjhjY2IyMzg2YmYxNmFmMjVhYzM4ZDhiNzdiYjBlMDQzMTUyNDYxYmQ5N2YzZjYzMGRiYjhiYyJ9fX0=");
		put("DONKEY", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGI1MjJmNmQ3N2MwNjk2YzlkMWYyYWQ0OWJmYTNjYjgyMDVhNWU2MjNhZjFjNDIwYmQ3NDBkYzQ3MTkxNGU5NyJ9fX0=");
		put("MULE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTRhZDc4ZjdhZGE3YzYzNzY0NDllZjk0OWM5Yzg3ZmRlY2U4ODJiNWEyZjE0Y2ZiZjhlYWM2ZmVhNjU3ZjRjNyJ9fX0=");
		put("UNDEAD_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWM1YjZmOGVmMWQ3NWY3M2E1MjkwYzkzNjdkMmI5YjgyM2JjOTYzZGUyYTM2NmZkNjU1MGJjYWNlMjc1MTIwNSJ9fX0=");
		put("ZOMBIE_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWM1YjZmOGVmMWQ3NWY3M2E1MjkwYzkzNjdkMmI5YjgyM2JjOTYzZGUyYTM2NmZkNjU1MGJjYWNlMjc1MTIwNSJ9fX0=");
		put("SKELETON_HORSE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWRjMDg0Yjc4NzQyNjg5NzMwMDZjODk3YTAzZDg5MDZjYzliM2RmOGMzOWJjZTkzZDg3ZWMwZGY1MDdiYmUwZCJ9fX0=");
		put("LLAMA", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWNiYzZiZDkyNzI4ZDc5Y2ZhNmQ4ZjIzY2JhZTlkOTEyZjQ5NTkyMGI5ZTk1ZWY2OTFhMTk2N2ZlZjhhNDQ1MyJ9fX0=");
	}};
	
	public static ItemStack applySkin(RPGHorse rpgHorse, ItemStack item) {
		String type;
		if (RPGHorsesMain.getVersion().getWeight() < 11) {
			type = ((LegacyHorseInfo) rpgHorse.getHorseInfo()).getVariant().name();
		} else {
			type = rpgHorse.getEntityType().name();
		}
		
		String skinValue;
		if (rpgHorse.isDead()) {
			skinValue = deadSkullMap.get(type);
			if (skinValue == null) {
				skinValue = deadSkullMap.get("HORSE");
			}
		} else {
			if (type.equals("HORSE") || type.equals("LLAMA")) {
				type = rpgHorse.getColor().name() + "_" + type;
			}
			skinValue = aliveSkullMap.get(type);
			if (skinValue == null) {
				if (type.endsWith("HORSE")) {
					skinValue = aliveSkullMap.get("BROWN_HORSE");
				} else if (type.endsWith("LLAMA")) {
					skinValue = aliveSkullMap.get("WHITE_LLAMA");
				}
			}
		}
		
		item.setItemMeta(ItemUtil.applyCustomHead(item.getItemMeta(), skinValue));
		return item;
	}
	
	public static String getAliveSkin(String key) {
		return aliveSkullMap.get(key);
	}
	
	public static String getDeadSkin(String key) {
		return aliveSkullMap.get(key);
	}
	
}
