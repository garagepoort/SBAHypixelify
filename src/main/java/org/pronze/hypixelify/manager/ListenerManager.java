package org.pronze.hypixelify.manager;
import org.bukkit.plugin.java.JavaPlugin;
import org.pronze.hypixelify.SBAHypixelify;
import org.pronze.hypixelify.inventories.CustomShop;
import org.pronze.hypixelify.listener.*;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager {

    private final List<AbstractListener> listeners = new ArrayList<>();

    public ListenerManager(){
        listeners.add(new CustomShop());
        listeners.add(new PlayerListener());
        listeners.add(new BedwarsListener());
        if (SBAHypixelify.getConfigurator().config.getBoolean("party.enabled")) {
            listeners.add(new ChatListener());
            if(SBAHypixelify.getConfigurator().config.getBoolean("main-lobby.enabled", false)){
                listeners.add(new LobbyBoard());
            }
            if(SBAHypixelify.getConfigurator().config.getBoolean("party.leader-autojoin-autoleave", true))
                listeners.add(new PartyListener());
        }

        if(SBAHypixelify.getConfigurator().config.getBoolean("lobby-scoreboard.enabled", true))
            listeners.add(new LobbyScoreboard());

    }

    public void registerAll(JavaPlugin plugin){
        if(listeners.isEmpty()) return;

        for(AbstractListener listener : listeners){
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    public void unregisterAll(){
        if(listeners.isEmpty()) return;

        for(AbstractListener l : listeners){
            l.onDisable();
        }
        listeners.clear();
    }
}
