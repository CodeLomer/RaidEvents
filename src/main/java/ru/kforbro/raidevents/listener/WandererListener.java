package ru.kforbro.raidevents.listener;

import lol.pyr.znpcsplus.api.event.NpcInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.events.EventManager;
import ru.kforbro.raidevents.events.Wanderer;
import ru.kforbro.raidevents.utils.Colorize;
import ru.kforbro.raidevents.utils.Utils;

public class WandererListener implements Listener {
    private final RaidEvents plugin;
    private final EventManager eventManager;

    public WandererListener(RaidEvents plugin) {
        this.plugin = plugin;
        this.eventManager = plugin.getEventManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        Wanderer wanderer = eventManager.getCurrentWanderer();
        if (wanderer == null || !wanderer.isChestSpawned()) return;

        if (!wanderer.getChestLocation().equals(block.getLocation())) return;

        event.setCancelled(true);
        wanderer.getGui().open(player);
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        Player player = event.getPlayer();
        Wanderer wanderer = eventManager.getCurrentWanderer();
        if (wanderer == null) return;

        if (!event.getNpc().getUuid().equals(wanderer.getNpc().getUuid())) return;

        if (!wanderer.isExpAvailable()) {
            Colorize.sendActionBar(player, "&cЭкспедиция появится через " + wanderer.getExpInterval() + " секунд.");
            return;
        }

        wanderer.setExpAvailable(false);
        Utils.giveOrDrop(player, wanderer.getItemStack());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Wanderer wanderer = eventManager.getCurrentWanderer();
            if (wanderer == null || wanderer.getSpawnChestAt() - System.currentTimeMillis() <= 0) return;
            wanderer.announce(player);
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Wanderer.removeItems(event.getPlayer().getInventory());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        if (itemStack.getType() == Material.PLAYER_HEAD) {
            PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
            if (container.has(new NamespacedKey(plugin, "wandereritem"), PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Player player)) return;

        ItemStack itemStack = event.getItem().getItemStack();
        PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();

        if (!container.has(new NamespacedKey(plugin, "wandereritem"), PersistentDataType.STRING)) return;

        if (eventManager.getCurrentWanderer() == null) {
            Wanderer.removeItems(player.getInventory());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        checkItem(event, itemStack);

        if (event.getClick() == ClickType.NUMBER_KEY) {
            itemStack = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            checkItem(event, itemStack);
        }
    }

    private void checkItem(InventoryClickEvent event, ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() == Material.PLAYER_HEAD) {
            PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
            if (container.has(new NamespacedKey(plugin, "wandereritem"), PersistentDataType.STRING)) {
                if (eventManager.getCurrentWanderer() != null) {
                    event.setCancelled(true);
                } else {
                    Wanderer.removeItems(event.getWhoClicked().getInventory());
                }
            }
        }
    }
}
