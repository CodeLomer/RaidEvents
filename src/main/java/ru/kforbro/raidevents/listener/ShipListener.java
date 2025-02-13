package ru.kforbro.raidevents.listener;

import lol.pyr.znpcsplus.api.event.NpcInteractEvent;
import lol.pyr.znpcsplus.api.npc.Npc;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.events.EventManager;
import ru.kforbro.raidevents.events.Ship;
import ru.kforbro.raidevents.events.ShipBarrel;
import ru.kforbro.raidevents.events.ShipTreasure;
import ru.kforbro.raidevents.utils.Colorize;
import ru.kforbro.raidevents.utils.Time;
import ru.kforbro.raidevents.utils.Utils;

public class ShipListener implements Listener {
    private final RaidEvents plugin;
    private final EventManager eventManager;

    public ShipListener(RaidEvents plugin) {
        this.plugin = plugin;
        this.eventManager = this.plugin.getEventManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractBarrel(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || this.eventManager.getCurrentShip() == null) return;

        Ship ship = this.eventManager.getCurrentShip();
        ShipBarrel barrel = ship.getBarrel(block.getLocation());
        if (barrel == null || barrel.isOpened()) return;

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.TRIPWIRE_HOOK) {
            PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
            String keyName = pdc.getOrDefault(new NamespacedKey(this.plugin, "ship_key"), PersistentDataType.STRING, "");

            if (keyName.isEmpty()) {
                Colorize.sendActionBar(player, "&x&f&a&5&d&5&d Вам нужна отмычка, чтобы открыть ящик.");
                event.setCancelled(true);
                return;
            }

            itemStack.setAmount(itemStack.getAmount() - 1);
            Colorize.sendActionBar(player, "&x&a&c&f&a&5&d Теперь ящик можно открыть.");
            barrel.populate(keyName);
            barrel.setOpened(true);

            new BukkitRunnable() {
                int seconds = 90;

                @Override
                public void run() {
                    barrel.createHologram(this.seconds);
                    if (--this.seconds < 0) {
                        this.cancel();
                        barrel.setTaskId(-1);
                        barrel.despawn();
                    }
                }
            }.runTaskTimer(this.plugin, 0L, 20L);
        } else {
            Colorize.sendActionBar(player, "&x&f&a&5&d&5&d Вам нужна отмычка, чтобы открыть ящик.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractTreasure(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || this.eventManager.getCurrentShip() == null) return;

        Ship ship = this.eventManager.getCurrentShip();
        ShipTreasure treasure = ship.getTreasure(block.getLocation());
        if (treasure == null) return;

        event.setCancelled(true);

        if (treasure.getCurrentKeys() < treasure.getNeedKeys()) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack.getType() == Material.TRIPWIRE_HOOK) {
                PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
                String keyName = pdc.getOrDefault(new NamespacedKey(this.plugin, "ship_key"), PersistentDataType.STRING, "");

                if (!keyName.equalsIgnoreCase("unique")) {
                    Colorize.sendActionBar(player, "&x&f&a&5&d&5&d Для сокровища требуются уникальные отмычки.");
                    return;
                }

                itemStack.setAmount(itemStack.getAmount() - 1);
                treasure.setCurrentKeys(treasure.getCurrentKeys() + 1);
                treasure.createHologram();

                if (treasure.getCurrentKeys() >= treasure.getNeedKeys()) {
                    treasure.setOpenAt(System.currentTimeMillis() + 60000L);
                    Colorize.sendActionBar(player, "&x&a&c&f&a&5&d Сокровище откроется через 60 секунд.");
                } else {
                    Colorize.sendActionBar(player, "&x&a&c&f&a&5&d Необходимо еще " + (treasure.getNeedKeys() - treasure.getCurrentKeys()) + " отмычек.");
                }
            }
        } else if (treasure.isOpened() && player.getGameMode() != GameMode.SPECTATOR) {
            treasure.getGui().open(player);
        } else if (!treasure.isOpened()) {
            Colorize.sendActionBar(player, "&cСокровище откроется через " + Time.prettyTime((treasure.getOpenAt() - System.currentTimeMillis() + 150L) / 1000L));
        }
    }

    @EventHandler
    public void onInteract(NpcInteractEvent event) {
        Player player = event.getPlayer();
        if (this.eventManager.getCurrentShip() == null) return;

        Ship ship = this.eventManager.getCurrentShip();
        Npc npc = event.getNpc();
        if (!npc.getUuid().equals(ship.getNpc().getUuid())) return;

        if (!ship.isKeyAvailable()) {
            Vector direction = player.getLocation().toVector().subtract(npc.getLocation().toBukkitLocation(player.getWorld()).toVector()).normalize().multiply(2);
            player.setVelocity(direction);
            Colorize.sendActionBar(player, "&cОтмычка появится через " + ship.getKeyInterval() + " секунд.");
            return;
        }

        ship.setKeyAvailable(false);
        Utils.giveOrDrop(player, ship.getRandomKeyItemStack());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) return;

            Ship ship = this.eventManager.getCurrentShip();
            if (ship == null) return;

            ship.announce(player);
        }, 20L);
    }
}
