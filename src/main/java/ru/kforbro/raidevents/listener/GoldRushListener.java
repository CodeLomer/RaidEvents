package ru.kforbro.raidevents.listener;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.events.EventManager;
import ru.kforbro.raidevents.events.GoldRush;

public class GoldRushListener
        implements Listener {
    private final RaidEvents plugin;
    private final EventManager eventManager;

    public GoldRushListener(RaidEvents plugin) {
        this.plugin = plugin;
        this.eventManager = this.plugin.getEventManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            GoldRush goldRush = this.eventManager.getCurrentGoldRush();
            if (goldRush == null) {
                return;
            }
            goldRush.announce(player);
        }, 20L);
    }
}