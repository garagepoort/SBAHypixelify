package org.pronze.hypixelify.inventories;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.pronze.hypixelify.SBAHypixelify;
import org.pronze.hypixelify.api.events.GameSelectorOpenEvent;
import org.pronze.hypixelify.utils.ShopUtil;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.lib.sgui.SimpleInventories;
import org.screamingsandals.bedwars.lib.sgui.builder.FormatBuilder;
import org.screamingsandals.bedwars.lib.sgui.events.PostActionEvent;
import org.screamingsandals.bedwars.lib.sgui.inventory.GuiHolder;
import org.screamingsandals.bedwars.lib.sgui.inventory.Options;
import org.screamingsandals.bedwars.lib.sgui.utils.MapReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GamesInventory implements Listener {
    private final HashMap<Integer, SimpleInventories> menu = new HashMap<>();
    private final HashMap<Integer, Options> option = new HashMap<>();
    private final HashMap<Integer, List<Player>> players = new HashMap<>();
    private final HashMap<Integer, String> labels = new HashMap<>();
    private final String bed_name, oak_name;
    private final List<String> bed_lore, stack_lore;

    public GamesInventory() {
        stack_lore = SBAHypixelify.getConfigurator().config.getStringList("games-inventory.stack-lore");
        bed_lore = SBAHypixelify.getConfigurator().config.getStringList("games-inventory.bed-lore");
        bed_name = SBAHypixelify.getConfigurator().config.getString("games-inventory.bed-name", "§aBed Wars ({mode})");
        oak_name = SBAHypixelify.getConfigurator().config.getString("games-inventory.oak_sign-name", "§aMap Selector ({mode})");
        String soloprefix, doubleprefix, tripleprefix, squadprefix;

        soloprefix = SBAHypixelify.getConfigurator().config.getString("games-inventory.gui.solo-prefix");
        doubleprefix = SBAHypixelify.getConfigurator().config.getString("games-inventory.gui.double-prefix");
        tripleprefix = SBAHypixelify.getConfigurator().config.getString("games-inventory.gui.triple-prefix");
        squadprefix = SBAHypixelify.getConfigurator().config.getString("games-inventory.gui.squad-prefix");

        Options option1 = ShopUtil.generateOptions();
        option1.setPrefix(soloprefix);
        option.put(1, option1);
        Options option2 = ShopUtil.generateOptions();
        option2.setPrefix(doubleprefix);
        option.put(2, option2);
        Options option3 = ShopUtil.generateOptions();
        option3.setPrefix(tripleprefix);
        option.put(3, option3);
        Options option4 = ShopUtil.generateOptions();
        option4.setPrefix(squadprefix);
        option.put(4, option4);

        labels.put(1, "Solo");
        labels.put(2, "Double");
        labels.put(3, "Triple");
        labels.put(4, "Squad");

        Bukkit.getServer().getPluginManager().registerEvents(this, SBAHypixelify.getInstance());
        createData();
    }

    private void createData() {
        SimpleInventories soloMenu = new SimpleInventories(option.get(1));
        SimpleInventories doubleMenu = new SimpleInventories(option.get(2));
        SimpleInventories tripleMenu = new SimpleInventories(option.get(3));
        SimpleInventories squadMenu = new SimpleInventories(option.get(4));


        for (int i = 1; i <= 4; i++) {

            List<String> bLore = new ArrayList<>();
            for (String st : bed_lore) {
                st = st
                        .replace("{mode}", labels.get(i));
                bLore.add(st);
            }

            List<String> sLore = new ArrayList<>();
            for (String st : stack_lore) {
                st = st.replace("{mode}", labels.get(i));
                sLore.add(st);
            }

            List<ItemStack> myCategories = ShopUtil.createCategories(bLore,
                    bed_name.replace("{mode}", labels.get(i)), oak_name.replace("{mode}", labels.get(i)));
            ItemStack category = myCategories.get(0);
            ItemStack category2 = myCategories.get(1);
            ItemStack category3 = myCategories.get(2);
            ItemStack category4 = myCategories.get(3);

            ArrayList<Object> Games = ShopUtil.createGamesGUI(i, sLore);
            FormatBuilder builder = ShopUtil.createBuilder(Games, category, category2, category3, category4);
            switch (i) {
                case 1:
                    soloMenu.load(builder);
                    soloMenu.generateData();
                    menu.put(1, soloMenu);
                    break;
                case 2:
                    doubleMenu.load(builder);
                    doubleMenu.generateData();
                    menu.put(2, doubleMenu);
                    break;
                case 3:
                    tripleMenu.load(builder);
                    tripleMenu.generateData();
                    menu.put(3, tripleMenu);
                    break;
                case 4:
                    squadMenu.load(builder);
                    squadMenu.generateData();
                    menu.put(4, squadMenu);
                    break;
            }


        }


    }

    public void destroy() {
        players.clear();
        HandlerList.unregisterAll(this);
    }

    public void openForPlayer(Player player, int mode) {
        GameSelectorOpenEvent event = new GameSelectorOpenEvent(player, mode);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        createData();
        if (menu.get(mode) == null)
            return;
        menu.get(mode).openForPlayer(player);
        players.computeIfAbsent(mode, k -> new ArrayList<>());
        players.get(mode).add(player);
    }

    public void repaint(int mode) {
        for (Player player : players.get(mode)) {
            GuiHolder guiHolder = menu.get(mode).getCurrentGuiHolder(player);
            if (guiHolder == null) {
                return;
            }

            createData();
            guiHolder.setFormat(menu.get(mode));
            guiHolder.repaint();
        }
    }

    @EventHandler
    public void onPostAction(PostActionEvent event) {
        if (event.getFormat() != menu.get(1) &&
                event.getFormat() != menu.get(2) &&
                event.getFormat() != menu.get(3) &&
                event.getFormat() != menu.get(4)) {
            return;
        }

        int mode = event.getFormat() == menu.get(1) ? 1 : event.getFormat() == menu.get(2) ? 2 : event.getFormat() == menu.get(3) ? 3 :
                event.getFormat() == menu.get(4) ? 4 : 1;

        ItemStack stack = event.getItem().getStack();
        Player player = event.getPlayer();
        if (stack != null) {
            if (stack.getType() == Material.BARRIER) {
                players.get(mode).remove(player);
                player.closeInventory();
            } else if (stack.getType().equals(ShopUtil.BED.getType())
                    || stack.getType().equals(ShopUtil.FireWorks.getType())
                    || stack.getType() == Material.DIAMOND) {
                player.closeInventory();
                repaint(mode);
                players.get(mode).remove(player);
                List<Game> games = ShopUtil.getGamesWithSize(mode);
                if (games == null || games.isEmpty())
                    return;
                for (Game game : games) {
                    if (game.getStatus().equals(GameStatus.WAITING)) {
                        game.joinToGame(player);
                        break;
                    }
                }
            } else if (stack.getType() == Material.ENDER_PEARL) {
                player.closeInventory();
                repaint(mode);
                players.get(mode).remove(player);
                player.performCommand("bw rejoin");
            }
        }

        MapReader reader = event.getItem().getReader();
        if (reader.containsKey("game")) {
            Game game = (Game) reader.get("game");
            Main.getGame(game.getName()).joinToGame(player);
            player.closeInventory();
            repaint(mode);
            players.get(mode).remove(player);
        }
    }


}
