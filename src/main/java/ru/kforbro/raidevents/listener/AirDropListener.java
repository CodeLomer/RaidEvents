package ru.kforbro.raidevents.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.events.AirDrop;
import ru.kforbro.raidevents.events.EventManager;
import ru.kforbro.raidevents.utils.Colorize;
import ru.kforbro.raidevents.utils.Time;

public class AirDropListener implements Listener {
    private final RaidEvents plugin;
    private final EventManager eventManager;

    public AirDropListener(RaidEvents plugin) {
        this.plugin = plugin;
        this.eventManager = this.plugin.getEventManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        AirDrop airDrop = eventManager.getAirDrop(block.getLocation());
        if (airDrop == null) {
            return;
        }

        event.setCancelled(true);

        if (!airDrop.isOpened() && !player.getGameMode().equals(GameMode.SPECTATOR)) {
            String timeRemaining = Time.prettyTime((airDrop.getOpenAt() - System.currentTimeMillis() + 150L) / 1000L);
            Colorize.sendActionBar(player, "&cAirDrop opens in " + timeRemaining);
            return;
        }

        if (player.getGameMode().equals(GameMode.SPECTATOR)) {
            player.openInventory(airDrop.getInventory());
        } else {
            airDrop.getGui().open((HumanEntity) player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            eventManager.getCurrentAirDrops().values().forEach(airDrop -> {
                if (airDrop.getOpenAt() - System.currentTimeMillis() > 0L) {
                    airDrop.announce(player);
                }
            });
        }, 20L);
    }
}
