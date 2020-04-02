package org.plugins.rpghorses.guis.instances;

import org.plugins.rpghorses.players.HorseOwner;

import java.util.List;

public class YourHorsesGUI {

    private HorseOwner horseOwner;

    private List<YourHorsesGUIPage> yourHorsesGUIPages;

    public YourHorsesGUI(HorseOwner horseOwner, List<YourHorsesGUIPage> yourHorsesGUIPages) {
        this.horseOwner = horseOwner;
        this.yourHorsesGUIPages = yourHorsesGUIPages;
    }

    public HorseOwner getHorseOwner() {
        return horseOwner;
    }

    public List<YourHorsesGUIPage> getYourHorsesGUIPages() {
        return yourHorsesGUIPages;
    }

    public void setYourHorsesGUIPages(List<YourHorsesGUIPage> yourHorsesGUIPages) {
        this.yourHorsesGUIPages = yourHorsesGUIPages;
    }

    public YourHorsesGUIPage getPage(int page) {
        if (page >= 1 && this.yourHorsesGUIPages.size() >= page) {
            return this.yourHorsesGUIPages.get(page - 1);
        }
        return null;
    }
}
