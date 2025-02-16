package ru.kforbro.raidevents.events;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.gui.builder.item.ItemBuilder;
import ru.kforbro.raidevents.gui.guis.Gui;
import ru.kforbro.raidevents.utils.Colorize;
import ru.kforbro.raidevents.utils.Time;
import ru.kforbro.raidevents.utils.Utils;
import net.kyori.adventure.text.Component;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Getter
public class ShipTreasure {
    @Setter
    private Location location;
    @Setter
    private Ship ship;
    @Setter
    private String hologramName;
    private int currentKeys = 0;
    @Setter
    private int needKeys = 4;
    @Setter
    private long openAt = 0L;
    private boolean opened;
    @Setter
    private HashMap<Integer, ItemStack> chestContent = new HashMap<>();
    @Setter
    private Gui gui;
    @Setter
    private HashSet<Player> playersInGui = new HashSet<>();
    @Setter
    private List<Material> randomMaterials = List.of(Material.CLAY_BALL, Material.NAUTILUS_SHELL,
            Material.BONE_MEAL, Material.GRAY_DYE, Material.FIREWORK_STAR, Material.GUNPOWDER, Material.PHANTOM_MEMBRANE, Material.QUARTZ);
    private final Cache<UUID, Boolean> clickItemCooldown = CacheBuilder.newBuilder().expireAfterWrite(150L, TimeUnit.MILLISECONDS).build();

    public ShipTreasure(Location location, Ship ship) {
        this.location = location;
        this.ship = ship;
        this.hologramName = "shiptreasure_" + this.serializeLocation(location);
        this.gui = Gui.gui().title(Component.text(Colorize.format("&8Сокровища пиратов"))).rows(6).disableAllInteractions().create();
        this.ship.getMythicLootContent().populateContainer(this.chestContent, 54);
    }

    public void setOpened(boolean state) {
        this.opened = state;
        if (state) {
            Bukkit.getScheduler().runTask(RaidEvents.getInstance(), () -> {
                for (Player nearbyPlayer : this.location.getNearbyPlayers(20.0)) {
                    nearbyPlayer.playSound(this.location, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);
                }
            });

            new BukkitRunnable() {
                public void run() {
                    if (chestContent.isEmpty()) {
                        this.cancel();
                        return;
                    }

                    ArrayList<Integer> keysAsArray = new ArrayList<>(chestContent.keySet());
                    int randomKey = keysAsArray.get(ThreadLocalRandom.current().nextInt(keysAsArray.size()));
                    ItemStack itemStack = chestContent.getOrDefault(randomKey, null);
                    if (itemStack != null) {
                        ArrayList<Integer> emptySlots = new ArrayList<>();
                        for (int i = 0; i < gui.getRows() * 9; ++i) {
                            if (!gui.getGuiItems().containsKey(i)) emptySlots.add(i);
                        }
                        if (!emptySlots.isEmpty()) {
                            int randomSlot = emptySlots.get(ThreadLocalRandom.current().nextInt(emptySlots.size()));
                            gui.updateItem(randomSlot, ItemBuilder.from(
                                    randomMaterials.get(ThreadLocalRandom.current().nextInt(randomMaterials.size()))
                                    )
                                    .setName(Colorize.format("&x&f&8&9&d&5&7Секретный предмет"))
                                            .asGuiItem(event -> {
                                                Player player = (Player) event.getWhoClicked();
                                                if (clickItemCooldown.getIfPresent(player.getUniqueId()) != null) return;
                                                clickItemCooldown.put(player.getUniqueId(), true);
                                                randomMaterials.forEach(m -> player.setCooldown(m, 3));
                                                gui.removeItem(randomSlot);
                                                Utils.giveOrDrop(player, itemStack);
                                            }));
                        }
                    }
                    chestContent.remove(randomKey);
                }
            }.runTaskTimer(RaidEvents.getInstance(), 0L, 2L);
        }
    }


    public void setCurrentKeys(int keys) {
        this.currentKeys = keys;
        Block block = this.location.getBlock();
        BlockData blockData = block.getBlockData();
        if (blockData instanceof RespawnAnchor anchor) {
            anchor.setCharges(Math.min(this.currentKeys, 4));
            block.setBlockData(anchor, false);
            for (Player nearbyPlayer : this.location.getNearbyPlayers(20.0)) {
                nearbyPlayer.playSound(this.location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
            }
        }
    }

    public void despawn() {
        this.location.getBlock().setType(Material.AIR);
        this.removeHologram();
        this.opened = false;
    }

    public void createHologram() {
        this.createDecentHologram();
        this.createFancyHologram();
    }

    public void removeHologram() {
        this.removeDecentHologram();
        this.removeFancyHologram();
    }

    private void createDecentHologram() {
       /* Hologram hologram = DHAPI.getHologram(this.hologramName);
        List<String> lines = getHologramLines();

        if (hologram == null) {
            Location center = this.location.clone().add(0.5, 1.3, 0.5);
            HologramUtils.createHologram(this.hologramName, center, false, lines);
        } else {
            hologram.getPage(0).setLine(0, lines.get(0));
            hologram.getPage(0).setLine(1, lines.get(1));
        }

        */
    }

    private void createFancyHologram() {
        /*HologramManager hologramManager = FancyHolograms.get().getHologramManager();
        de.oliver.fancyholograms.api.hologram.Hologram hologram = hologramManager.getHologram(this.hologramName).orElse(null);
        List<String> lines = getHologramLines();

        if (hologram == null) {
            Location center = this.location.clone().add(0.5, 1.3, 0.5);
            TextHologramData hologramData = new TextHologramData(this.hologramName, center);
            hologramData.setBillboard(Display.Billboard.VERTICAL);
            hologramData.setBackground(de.oliver.fancyholograms.api.hologram.Hologram.TRANSPARENT);
            hologramData.setScale(new Vector3f(0.75f, 0.75f, 0.75f));
            hologramData.setPersistent(false);
            hologramData.setTextShadow(true);
            hologramData.setText(lines);
            hologram = hologramManager.create(hologramData);
            hologramManager.addHologram(hologram);
        } else {
            HologramData hologramData = hologram.getData();
            if (hologramData instanceof TextHologramData textData) {
                textData.setText(lines);
                if (hologramData.hasChanges()) {
                    hologram.refreshForViewers();
                }
            }
        }

        FancyHolograms.get().getHologramThread().submit(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                hologramManager.getHologram(this.hologramName).ifPresent(h -> h.forceUpdateShownStateFor(player));
            }
        });*/
    }

    private List<String> getHologramLines() {
        if (this.currentKeys < this.needKeys) {
            return List.of("Сокровища пиратов", "Использовано ключей: " + this.currentKeys + " из " + this.needKeys);
        } else if (this.openAt >= System.currentTimeMillis()) {
            return List.of("Сокровища пиратов", "До открытия: " + Time.prettyTime((this.openAt - System.currentTimeMillis() + 150L) / 1000L));
        } else {
            return List.of("Сокровища пиратов", "Нажмите, чтобы открыть.");
        }
    }

    private void removeDecentHologram() {
        //DHAPI.removeHologram(this.hologramName);
    }

    private void removeFancyHologram() {
       /* HologramManager fancyManager = FancyHolograms.get().getHologramManager();
        de.oliver.fancyholograms.api.hologram.Hologram hologram = fancyManager.getHologram(this.hologramName).orElse(null);
        if (hologram != null) {
            fancyManager.removeHologram(hologram);
        }*/
    }

    public String serializeLocation(Location location) {
        return location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShipTreasure other)) return false;
        return currentKeys == other.currentKeys &&
                needKeys == other.needKeys &&
                openAt == other.openAt &&
                opened == other.opened &&
                Objects.equals(location, other.location) &&
                Objects.equals(ship, other.ship) &&
                Objects.equals(hologramName, other.hologramName) &&
                Objects.equals(chestContent, other.chestContent) &&
                Objects.equals(gui, other.gui) &&
                Objects.equals(playersInGui, other.playersInGui) &&
                Objects.equals(randomMaterials, other.randomMaterials) &&
                Objects.equals(clickItemCooldown, other.clickItemCooldown);
    }

    public int hashCode() {
        return Objects.hash(location, ship, hologramName, currentKeys, needKeys, openAt, opened, chestContent, gui, playersInGui, randomMaterials, clickItemCooldown);
    }

    @Override
    public String toString() {
        return "ShipTreasure{" +
                "location=" + location +
                ", ship=" + ship +
                ", hologramName='" + hologramName + '\'' +
                ", currentKeys=" + currentKeys +
                ", needKeys=" + needKeys +
                ", openAt=" + openAt +
                ", opened=" + opened +
                ", chestContent=" + chestContent +
                ", gui=" + gui +
                ", playersInGui=" + playersInGui +
                ", randomMaterials=" + randomMaterials +
                ", clickItemCooldown=" + clickItemCooldown +
                '}';
    }
}
