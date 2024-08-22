package org.plugins.rpghorses.version;

import org.bukkit.Bukkit;

public enum Version {
	
	v1_8("1.8", 8, ""),
	v1_9("1.9", 9, ""),
	v1_10("1.10", 10, ""),
	v1_11("1.11", 11, "v1_11_2"),
	v1_12("1.12", 12, "v1_12_2"),
	v1_13("1.13", 13, "v1_13_2"),
	v1_14("1.14", 14, "v1_14_4"),
	v1_15("1.15", 15, "v1_15_2"),
	v1_16_1("1.16.1", 16, "v1_16_3"),
	v1_16_2("1.16.2", 16, "v1_16_3"),
	v1_16_3("1.16.3", 16, "v1_16_3"),
	v1_16_4("1.16.4", 17, "v1_16_4"),
	v1_16_5("1.16.5", 18, "v1_16_4"),
	v1_17("1.17", 19, "v1_17"),
	v1_17_1("1.17.1", 20, "v1_17"),
	v1_18("1.18", 21, "v1_18"),
	v1_18_1("1.18.1", 22, "v1_18"),
	v1_18_2("1.18.2", 23, "v1_18_2"),
	v1_19("1.19", 24, "v1_19"),
	v1_20("1.20", 25, "v1_20"),
	v1_20_1("1.20.1", 26, "v1_20"),
	v1_20_2("1.20.2", 27, "v1_20_2"),
	v1_20_3("1.20.3", 28, "v1_20_2"),
	v1_20_4("1.20.4", 29, "v1_20_4"),
	v1_21("1.21", 30, "v1_21"),
	LATEST("", 100, "");
	
	private String name;
	private int weight;
	private String abstractName;
	
	Version(String name, int weight, String abstractName) {
		this.name = name;
		this.weight = weight;
		this.abstractName = abstractName;
	}
	
	public String getName() {
		return name;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public String getAbstractName() {
		return abstractName;
	}
	
	public static Version getByName(String name) {
		Version highestMatch = Version.LATEST;

		for (Version version : values())
			if (version != Version.LATEST && name.startsWith(version.name))
				highestMatch = version;
		
		return highestMatch;
	}
	
	public static Version getVersion() {
		return Version.getByName(Bukkit.getBukkitVersion().split("-")[0]);
	}
	
}

