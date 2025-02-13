package ru.kforbro.raidevents.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.events.EventManager;
import ru.kforbro.raidevents.events.Mine;
import ru.kforbro.raidevents.utils.Colorize;
import ru.kforbro.raidevents.utils.Utils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MineListener implements Listener {
    private final RaidEvents plugin;
    private final EventManager eventManager;
    private final Cache<UUID, Boolean> breakCooldown = CacheBuilder.newBuilder()
            .expireAfterWrite(250L, TimeUnit.MILLISECONDS)
            .build();

    private final List<PotionEffect> potionEffects = List.of(
            new PotionEffect(PotionEffectType.SPEED, 100, 2),
            new PotionEffect(PotionEffectType.FAST_DIGGING, 100, 2),
            new PotionEffect(PotionEffectType.GLOWING, 100, 0),
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 3),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 3),
            new PotionEffect(PotionEffectType.BLINDNESS, 100, 0)
    );

    public MineListener(RaidEvents plugin) {
        this.plugin = plugin;
        this.eventManager = this.plugin.getEventManager();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Mine mine = eventManager.getCurrentMine();

        if (mine == null || !mine.getCuboid().contains(block.getLocation()) || mine.getStopAt() - System.currentTimeMillis() <= 0L) {
            return;
        }

        if (breakCooldown.getIfPresent(player.getUniqueId()) != null) {
            Colorize.sendActionBar(player, "&cВы слишком быстро копаете.");
            event.setCancelled(true);
            return;
        }

        breakCooldown.put(player.getUniqueId(), true);
        swapMainHandItem(player);
        event.setDropItems(false);

        ItemStack reward = mine.getBlocks().remove(block.getLocation());

        int random = ThreadLocalRandom.current().nextInt(100);
        if (random < 10) {
            player.damage(10.0);
        } else if (random < 15) {
            player.addPotionEffect(potionEffects.get(ThreadLocalRandom.current().nextInt(potionEffects.size())));
        } else if (random < 20) {
            int money = ThreadLocalRandom.current().nextInt(5000, 10000);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "money " + player.getName() + " vault give " + money);
            Colorize.sendActionBar(player, "&fВы получили &x&8&5&d&2&6&4" + money + " &f\ue23a\uf801 &fза добычу блока.");
        } else if (random < 50) {
            int rubles = ThreadLocalRandom.current().nextInt(1, 6);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rubles add " + player.getName() + " " + rubles);
            Colorize.sendMessage(player, "&9 ┃&f Вы получили &x&f&d&b&e&3&9" + rubles + " рейдов за добычу блока.");
        }

        Utils.giveOrDrop(player, reward);
    }

    private void swapMainHandItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHandItem = inventory.getItemInMainHand();
        int randomSlot = ThreadLocalRandom.current().nextInt(inventory.getSize());
        ItemStack randomItem = inventory.getItem(randomSlot);

        inventory.setItemInMainHand(randomItem);
        inventory.setItem(randomSlot, mainHandItem);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Mine mine = eventManager.getCurrentMine();
            if (mine != null) mine.announce(player);
        }, 20L);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Mine mine = eventManager.getCurrentMine();

        if (mine != null && mine.getCuboid().contains(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.ENDER_PEARL) {
            Mine mine = eventManager.getCurrentMine();
            if (mine != null && mine.getStanCuboid().contains(player.getLocation())) {
                event.setCancelled(true);
                Colorize.sendActionBar(player, "&cЭндер-жемчуг недоступен здесь.");
            }
        }
    }

    @EventHandler
    public void onChorus(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item.getType() == Material.CHORUS_FRUIT) {
            Mine mine = eventManager.getCurrentMine();
            if (mine != null && mine.getStanCuboid().contains(player.getLocation())) {
                event.setCancelled(true);
                Colorize.sendActionBar(player, "&cПлод хоруса недоступен здесь.");
            }
        }
    }
}
