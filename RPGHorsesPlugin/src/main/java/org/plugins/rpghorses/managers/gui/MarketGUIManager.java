package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.instances.MarketGUIPage;
import org.plugins.rpghorses.guis.instances.YourHorsesGUI;
import org.plugins.rpghorses.guis.instances.YourHorsesGUIPage;
import org.plugins.rpghorses.horses.MarketHorse;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.SkinValueUtil;
import rorys.library.configs.CustomConfig;
import rorys.library.util.ItemUtil;
import rorys.library.util.MessagingUtil;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class MarketGUIManager {

    private final RPGHorsesMain     plugin;
    private final HorseOwnerManager horseOwnerManager;
    private final RPGHorseManager   rpgHorseManager;
    private final CustomConfig      marketConfig;
    private final ItemUtil        itemUtil;

    private ItemStack marketFillItem, horseItem, yourHorsesItem, backItem, previousPageItem, nextPageItem;
    private int marketRows, yourHorsesSlot, backSlot, previousPageSlot, nextPageSlot;
    private List<MarketGUIPage> marketGUIPages = new ArrayList<>();
    private List<MarketHorse> marketHorses = new ArrayList<>();

    public MarketGUIManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager, CustomConfig marketConfig, ItemUtil itemUtil) {
        this.plugin = plugin;
        this.horseOwnerManager = horseOwnerManager;
        this.rpgHorseManager = rpgHorseManager;
        this.marketConfig = marketConfig;
        this.itemUtil = itemUtil;

        this.reload();
    }

    public int getPreviousPageSlot() {
        return previousPageSlot;
    }

    public int getNextPageSlot() {
        return nextPageSlot;
    }

    public int getYourHorsesSlot() {
        return this.yourHorsesSlot;
    }

    public int getBackSlot() {
        return this.backSlot;
    }

    public ItemStack getHorseItem(RPGHorse rpgHorse) {
        ItemStack item = horseItem.clone();
        return SkinValueUtil.applySkin(rpgHorse, item);
    }

    public void setupMarketGUI() {
        this.loadMarketHorses();

        int rpgHorsesAdded = -1;
        String title = MessagingUtil.format(this.plugin.getConfig().getString("market-options.title"));
        Inventory gui = Bukkit.createInventory(null, this.marketRows * 9, title);
        int slot = 10, pageNum = 1;
        List<MarketGUIPage> marketGUIPages = new ArrayList<>();
        HashMap<Integer, RPGHorse> rpgHorseSlots = new HashMap<>();

        for (MarketHorse marketHorse : this.marketHorses) {
            rpgHorsesAdded++;
            RPGHorse rpgHorse = marketHorse.getRPGHorse();
            ItemStack item = getHorseItem(rpgHorse);
            item = fillPlaceholders(item, marketHorse);
            gui.setItem(slot, item);
            rpgHorseSlots.put(slot, rpgHorse);
            if (slot++ == (this.marketRows - 1) * 9 - 2 || rpgHorsesAdded == this.marketHorses.size() - 1) {
                for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
                    if (fillSlot == this.nextPageSlot && rpgHorsesAdded < this.marketHorses.size() - 1) {
                        gui.setItem(fillSlot, this.nextPageItem);
                    } else if (fillSlot == this.previousPageSlot && pageNum > 1) {
                        gui.setItem(fillSlot, this.previousPageItem);
                    } else if (fillSlot == this.yourHorsesSlot) {
                        gui.setItem(fillSlot, this.yourHorsesItem);
                    } else if (gui.getItem(fillSlot) == null) {
                        gui.setItem(fillSlot, this.marketFillItem);
                    }
                }
                marketGUIPages.add(new MarketGUIPage(pageNum, gui, rpgHorseSlots));
                slot = 10;
                rpgHorseSlots = new HashMap<>();
                gui = Bukkit.createInventory(null, this.marketRows * 9, title);
            }
        }

        if (rpgHorsesAdded == -1) {
            for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
                if (fillSlot == this.nextPageSlot && rpgHorsesAdded < this.marketHorses.size() - 1) {
                    gui.setItem(fillSlot, this.nextPageItem);
                } else if (fillSlot == this.previousPageSlot && pageNum > 1) {
                    gui.setItem(fillSlot, this.previousPageItem);
                } else if (fillSlot == this.yourHorsesSlot) {
                    gui.setItem(fillSlot, this.yourHorsesItem);
                } else if (gui.getItem(fillSlot) == null) {
                    gui.setItem(fillSlot, this.marketFillItem);
                }
            }
            marketGUIPages.add(new MarketGUIPage(pageNum++, gui, rpgHorseSlots));
        }
        this.marketGUIPages = marketGUIPages;
    }

    public void reloadMarketGUI() {
        this.setupMarketGUI();

        mainloop:
        for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners()) {
            if (horseOwner.isInGUI(GUILocation.MARKET_GUI)) {
                MarketGUIPage marketGUIPage = horseOwner.getCurrentMarketGUIPage();
                int pageNum = marketGUIPage.getPageNum();
                while (pageNum > 0) {
                    MarketGUIPage newMarketGUIPage = this.getPage(pageNum--);
                    if (newMarketGUIPage != null) {
                        horseOwner.openMarketGUIPage(newMarketGUIPage);
                        continue mainloop;
                    }
                }
                Bukkit.getPlayer(horseOwner.getUUID()).closeInventory();
            }
        }
    }

    public void loadMarketHorses() {
        this.marketHorses.clear();

        FileConfiguration config = this.marketConfig.getConfig();
        Set<String> ids = config.getConfigurationSection("market").getKeys(false);

        for (String id : ids) {
            RPGHorse rpgHorse = this.getRPGHorse(id);
            double price = config.getDouble("market." + id + ".price");
            this.marketHorses.add(new MarketHorse(Integer.valueOf(id), rpgHorse, price));
        }
    }

    public void addHorse(RPGHorse rpgHorse, double price, int index) {
        String id = "" + this.marketConfig.getConfig().getConfigurationSection("market").getKeys(false).size();
        FileConfiguration config = this.marketConfig.getConfig();
        String path = "market." + id + ".";
        config.set(path + "horse-owner", rpgHorse.getHorseOwner().getUUID().toString());
        config.set(path + "price", price);
        config.set(path + "index", index);
        this.marketConfig.saveConfig();
        this.marketConfig.reloadConfig();
        this.reloadMarketGUI();

        this.setupYourHorsesGUI(rpgHorse.getHorseOwner());
    }

    public void removeHorse(MarketGUIPage marketGUIPage, RPGHorse rpgHorse, boolean horseRemoved) {
        int id = this.getID(marketGUIPage, rpgHorse);
        HorseOwner horseOwner = rpgHorse.getHorseOwner();
        int index = horseOwner.getHorseNumber(rpgHorse) - 1;
        UUID uuid = horseOwner.getUUID();
        FileConfiguration config = this.marketConfig.getConfig();
        boolean afterID = false;
        for (String loopID : config.getConfigurationSection("market").getKeys(false)) {
            if (afterID) {
                int newID = Integer.valueOf(loopID) - 1;
                String uuidStr = config.getString("market." + loopID + ".horse-owner");
                config.set("market." + newID + ".horse-owner", uuidStr);
                config.set("market." + newID + ".price", config.getString("market." + loopID + ".price"));
                int newIndex = Integer.valueOf(config.getString("market." + loopID + ".index"));
                if (horseRemoved && uuidStr.equals(uuid.toString()) && newIndex > index) {
                    newID -= 1;
                }
                config.set("market." + newID + ".index", newIndex);
            }
            if (loopID.equalsIgnoreCase("" + id)) {
                afterID = true;
            }
        }
        config.set("market." + (config.getConfigurationSection("market").getKeys(false).size() - 1), null);
        this.marketConfig.saveConfig();
        this.marketConfig.reloadConfig();

        this.reloadMarketGUI();
        this.setupYourHorsesGUI(rpgHorse.getHorseOwner());
    }

    public double getPrice(MarketGUIPage marketGUIPage, RPGHorse rpgHorse) {
        FileConfiguration config = this.marketConfig.getConfig();
        return config.getDouble("market." + this.getID(marketGUIPage, rpgHorse) + ".price");
    }

    public int getID(MarketGUIPage marketGUIPage, RPGHorse rpgHorse) {
        int id = 0;
        for (RPGHorse rpgHorse1 : marketGUIPage.getHorseSlots().values()) {
            if (rpgHorse1.equals(rpgHorse)) {
                break;
            }
            id++;
        }
        id += (marketGUIPage.getGUI().getSize() - (18 + (this.marketRows - 2) * 2)) * (marketGUIPage.getPageNum() - 1);
        return id;
    }

    public MarketGUIPage getPage(int page) {
        if (page >= 1 && this.marketGUIPages.size() >= page) {
            return this.marketGUIPages.get(page - 1);
        }
        return null;
    }

    public RPGHorse getRPGHorse(String id) {
        String path = "market." + id + ".";
        HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(Bukkit.getOfflinePlayer(UUID.fromString(this.marketConfig.getConfig().getString(path + "horse-owner"))));
        RPGHorse rpgHorse = horseOwner.getRPGHorse(this.marketConfig.getConfig().getInt(path + ".index"));
        if (Bukkit.getPlayer(horseOwner.getUUID()) == null) {
            this.horseOwnerManager.flushHorseOwner(horseOwner);
        }
        return rpgHorse;
    }

    public MarketGUIPage getPage(RPGHorse rpgHorse) {
        if (rpgHorse.isInMarket()) {
            int pageNum = 1;
            MarketGUIPage page;
            do
            {
                page = this.getPage(pageNum++);
                for (RPGHorse rpgHorse1 : page.getHorseSlots().values()) {
                    if (rpgHorse1.equals(rpgHorse)) {
                        return page;
                    }
                }
            }
            while (page != null);
        }
        return null;
    }

    public ItemStack fillPlaceholders(ItemStack item, MarketHorse marketHorse) {
        if (item != null && marketHorse != null) {
            ItemMeta itemMeta = item.getItemMeta();

            RPGHorse rpgHorse = marketHorse.getRPGHorse();

            NumberFormat formatter = new DecimalFormat("#0.0##");
            String name = MessagingUtil.format(rpgHorse.getName()), tier = "" + rpgHorse.getTier(), movementSpeed = formatter.format(rpgHorse.getMovementSpeed()), jumpStrength = formatter.format(rpgHorse.getJumpStrength()), health = formatter.format(rpgHorse.getHealth()), maxHealth = formatter.format(rpgHorse.getMaxHealth()), price = formatter.format(marketHorse.getPrice());

            String ownerName = "null";
            UUID uuid = rpgHorse.getHorseOwner().getUUID();
            OfflinePlayer p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                ownerName = p.getName();
            } else {
                p = Bukkit.getOfflinePlayer(uuid);
                if (p != null && p.hasPlayedBefore()) {
                    ownerName = p.getName();
                }
            }

            if (itemMeta.hasDisplayName()) {
                itemMeta.setDisplayName(itemMeta.getDisplayName().replace("{HEALTH}", health).replace("{MAX-HEALTH}", maxHealth).replace("{TIER}", tier).replace("{HORSE-NAME}", name).replace("{MOVEMENT-SPEED}", movementSpeed).replace("{JUMP-STRENGTH}", jumpStrength).replace("{PRICE}", "" + price).replace("{HORSE-OWNER}", ownerName));
            }

            if (itemMeta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String line : itemMeta.getLore()) {
                    newLore.add(line.replace("{HEALTH}", health).replace("{MAX-HEALTH}", maxHealth).replace("{TIER}", tier).replace("{HORSE-NAME}", name).replace("{MOVEMENT-SPEED}", movementSpeed).replace("{JUMP-STRENGTH}", jumpStrength).replace("{PRICE}", "" + price).replace("{HORSE-OWNER}", ownerName));
                }
                itemMeta.setLore(newLore);
            }

            item.setItemMeta(itemMeta);
        }
        return item;
    }

    public void setupYourHorsesGUIS() {
        for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners()) {
            this.setupYourHorsesGUI(horseOwner);
        }
    }

    public void setupYourHorsesGUI(HorseOwner horseOwner) {
        List<RPGHorse> rpgHorses = horseOwner.getRPGHorses();
        int horseCount = -1, slot = 10, pageNum = 1;
        String title = MessagingUtil.format(this.plugin.getConfig().getString("market-options.title"));
        Inventory gui = Bukkit.createInventory(null, this.marketRows * 9, title);
        List<YourHorsesGUIPage> yourHorsesGUIPages = new ArrayList<>();
        HashMap<Integer, MarketHorse> horseSlots = new HashMap<>();

        for (RPGHorse rpgHorse : rpgHorses) {
            horseCount++;
            if (rpgHorse.isInMarket()) {
                MarketHorse marketHorse = this.getMarketHorse(rpgHorse);
                ItemStack item = getHorseItem(rpgHorse);
                item = fillPlaceholders(item, marketHorse);
                gui.setItem(slot, item);
                horseSlots.put(slot, marketHorse);
            }
            if (slot++ == (this.marketRows - 1) * 9 - 2 || horseCount == rpgHorses.size() - 1) {
                for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
                    if (fillSlot == (this.marketRows * 9) - 1 && horseCount < rpgHorses.size() - 1) {
                        gui.setItem(fillSlot, this.nextPageItem);
                    } else if (fillSlot == (this.marketRows * 9) - 9 && pageNum > 1) {
                        gui.setItem(fillSlot, this.previousPageItem);
                    } else if (fillSlot == this.getBackSlot()) {
                        gui.setItem(fillSlot, this.backItem);
                    } else if (gui.getItem(fillSlot) == null) {
                        gui.setItem(fillSlot, this.marketFillItem);
                    }
                }
                yourHorsesGUIPages.add(new YourHorsesGUIPage(pageNum++, gui, horseSlots));
                slot = 10;
                horseSlots = new HashMap<>();
                gui = Bukkit.createInventory(null, this.marketRows * 9, title);
            }
        }

        if (yourHorsesGUIPages.size() == 0) {
            for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
                if (fillSlot == this.getBackSlot()) {
                    gui.setItem(fillSlot, this.backItem);
                } else if (gui.getItem(fillSlot) == null) {
                    gui.setItem(fillSlot, this.marketFillItem);
                }
            }
            yourHorsesGUIPages.add(new YourHorsesGUIPage(pageNum++, gui, horseSlots));
        }

        YourHorsesGUI yourHorsesGUI = new YourHorsesGUI(horseOwner, yourHorsesGUIPages);
        horseOwner.setYourHorsesGUI(yourHorsesGUI);
    }

    public MarketHorse getMarketHorse(RPGHorse rpgHorse) {
        for (MarketHorse marketHorse : this.marketHorses) {
            if (marketHorse.getRPGHorse().equals(rpgHorse)) {
                return marketHorse;
            }
        }
        return null;
    }

    public boolean reload() {
        boolean reloadItems = false;
        reloadItems = this.reloadPreviousPageItem() || reloadItems;
        reloadItems = this.reloadNextPageItem() || reloadItems;
        reloadItems = this.reloadMarketFillItem() || reloadItems;
        reloadItems = this.reloadHorseItem() || reloadItems;
        reloadItems = this.reloadYourHorsesItem() || reloadItems;
        reloadItems = this.reloadBackItem() || reloadItems;
        reloadItems = this.reloadMarketNumbers() || reloadItems;

        if (reloadItems) {
            this.setupMarketGUI();
            this.setupYourHorsesGUIS();
        }

        return reloadItems;
    }

    private boolean reloadMarketFillItem() {
        ItemStack oldMarketFillItem = this.marketFillItem;
        this.marketFillItem = this.itemUtil.getItemStack("market-options.fill-item");
        return ItemUtil.isSimilar(oldMarketFillItem, marketFillItem);
    }

    private boolean reloadHorseItem() {
        ItemStack oldHorseItem = this.horseItem;
        this.horseItem = this.itemUtil.getItemStack("market-options.horse-item");
        return ItemUtil.isSimilar(oldHorseItem, horseItem);
    }

    private boolean reloadYourHorsesItem() {
        ItemStack oldYourHorsesItem = this.yourHorsesItem;
        this.yourHorsesItem = this.itemUtil.getItemStack("market-options.your-horses-item");
        return ItemUtil.isSimilar(oldYourHorsesItem, yourHorsesItem);
    }

    private boolean reloadBackItem() {
        ItemStack oldBackItem = this.backItem;
        this.backItem = this.itemUtil.getItemStack("market-options.back-item");
        return ItemUtil.isSimilar(oldBackItem, backItem);
    }

    private boolean reloadPreviousPageItem() {
        ItemStack oldPreviousPageItem = this.previousPageItem;
        this.previousPageItem = this.itemUtil.getItemStack("market-options.previous-page-item");
        return ItemUtil.isSimilar(oldPreviousPageItem, previousPageItem);
    }

    private boolean reloadNextPageItem() {
        ItemStack oldNextPageItem = this.nextPageItem;
        this.nextPageItem = this.itemUtil.getItemStack("market-options.next-page-item");
        return ItemUtil.isSimilar(oldNextPageItem, nextPageItem);
    }

    private boolean reloadMarketNumbers() {
        int oldMarketRows = this.marketRows;
        this.marketRows = this.plugin.getConfig().getInt("market-options.rows");

        int oldYourHorsesSlot = this.yourHorsesSlot;
        this.yourHorsesSlot = this.itemUtil.getSlot("market-options.your-horses-item");

        int oldBackSlot = this.backSlot;
        this.backSlot = this.itemUtil.getSlot("market-options.back-item");

        this.nextPageSlot = this.marketRows * 9 - 1;
        this.previousPageSlot = this.nextPageSlot - 8;

        return this.marketRows != oldMarketRows || this.yourHorsesSlot != oldYourHorsesSlot || this.backSlot != oldBackSlot;
    }
}
