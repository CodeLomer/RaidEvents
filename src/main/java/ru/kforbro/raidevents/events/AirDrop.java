package ru.kforbro.raidevents.events;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.config.Loot;
import ru.kforbro.raidevents.gui.builder.item.ItemBuilder;
import ru.kforbro.raidevents.gui.guis.Gui;
import ru.kforbro.raidevents.utils.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import net.kyori.adventure.text.Component;

@Getter
public class AirDrop extends Event {
    private final Cache<UUID, Boolean> clickItemCooldown = CacheBuilder.newBuilder().expireAfterWrite(150L, TimeUnit.MILLISECONDS).build();
    @Setter
    private List<Location> chestLocations = new ArrayList<>();
    @Setter
    private HashMap<Integer, ItemStack> chestContent = new HashMap<>();
    @Setter
    private int chestCount;
    @Setter
    private long spawnAt;
    @Setter
    private long openAt;
    @Setter
    private long stopAt;
    @Setter
    private boolean opened;
    @Setter
    private Inventory inventory;
    @Setter
    private ru.kforbro.raidevents.gui.guis.Gui gui;
    @Setter
    private Loot.LootContent lootContent;
    @Setter
    private Hologram decentHologram;
    @Setter
    private Material material;
    @Setter
    private Color color;
    @Setter
    private boolean explode;
    @Setter
    private boolean allowPvP;
    @Setter
    private HashSet<Player> playersInGui = new HashSet<>();
    @Setter
    private List<Material> randomMaterials = List.of(Material.CLAY_BALL, Material.NAUTILUS_SHELL, Material.BONE_MEAL,
            Material.GRAY_DYE, Material.FIREWORK_STAR, Material.GUNPOWDER, Material.PHANTOM_MEMBRANE, Material.QUARTZ);
    private final EventManager eventManager = RaidEvents.getInstance().getEventManager();

    public AirDrop(String name, String rarity, int chestCount, Loot.LootContent lootContent, Material material, Color color, boolean explode, boolean allowPvP) {
        this.name = name;
        this.rarity = rarity;
        this.chestCount = chestCount;
        this.lootContent = lootContent;
        this.material = material;
        this.color = color;
        this.explode = explode;
        this.allowPvP = allowPvP;
    }

    public static boolean isOutsideCircle(int x, int z, int centerX, int centerZ, int radius) {
        int dz;
        int dx = Math.abs(x - centerX);
        return dx * dx + (dz = Math.abs(z - centerZ)) * dz > radius * radius;
    }

    @Override
    public void start() {
        CompletableFuture.runAsync(() -> {
            final World world = Bukkit.getWorld("world");
            this.spawnAt = System.currentTimeMillis();
            this.openAt = this.spawnAt + 600000L;
            this.stopAt = this.openAt + 300000L;
            RandomLocation.SafeLocation safeLocation = null;
            if (world != null) {
                safeLocation = RandomLocation.getRandomSafeLocation(world, RandomLocation.Algorithm.SQUARE,
                        500.0, world.getWorldBorder().getSize() / 2.0 - 50.0, 0, 0);
            }
            if (safeLocation == null) {
                MyLogger.logError(this, "Failed to find safe location for air drop");
                return;
            }
            eventManager.getCurrentAirDrops().put(this.uuid, this);
            final Location location = Objects.requireNonNull(safeLocation).location().add(0.0, 1.0, 0.0);
            Bukkit.getScheduler().runTask(RaidEvents.getInstance(), () -> {
                int radius = 12;
                int xStart = location.getBlockX() - radius;
                int xEnd = location.getBlockX() + radius;
                int zStart = location.getBlockZ() - radius;
                int zEnd = location.getBlockZ() + radius;
                for (int x = xStart; x <= xEnd; ++x) {
                    for (int z = zStart; z <= zEnd; ++z) {
                        for (int y = location.getBlockY(); y <= location.getBlockY() + 40; ++y) {
                            Block block;
                            if (AirDrop.isOutsideCircle(x, z, location.getBlockX(), location.getBlockZ(), radius) || (block = new Location(world, x, y, z).getBlock()).getType().equals(Material.AIR))
                                continue;
                            block.setType(Material.AIR, false);
                        }
                    }
                }
                location.getBlock().setType(this.material, false);
                this.inventory = Bukkit.createInventory(null, 54, Colorize.format("&8" + ChatColor.stripColor(Colorize.format(this.name))));
                this.lootContent.populateContainer(this.chestContent, 54);
                this.lootContent.populateContainer(this.inventory);
                this.gui =  Gui.gui().title(Component.text(Colorize.format("&8" + ChatColor.stripColor(Colorize.format(this.name))))).rows(6).disableAllInteractions().create();
                this.gui.setOpenGuiAction(event -> this.playersInGui.add((Player) event.getPlayer()));
                this.gui.setCloseGuiAction(event -> this.playersInGui.remove((Player) event.getPlayer()));
                BlockData patt1$temp = location.getBlock().getBlockData();
                if (patt1$temp instanceof RespawnAnchor anchor) {
                    Bukkit.getScheduler().runTask(RaidEvents.getInstance(), () -> {
                        anchor.setCharges(1);
                        location.getBlock().setBlockData(anchor);
                    });
                    new BukkitRunnable() {
                        int charges = 1;

                        public void run() {
                            int seconds = (int) ((System.currentTimeMillis() - AirDrop.this.spawnAt) / 1000L) + 1;
                            int newCharges = seconds / 150;
                            if (this.charges < newCharges) {
                                this.charges = newCharges;
                                anchor.setCharges(this.charges);
                                location.getBlock().setBlockData(anchor);
                                location.getNearbyPlayers(35.0).forEach(player -> player.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f));
                                if (this.charges >= 4) {
                                    this.cancel();
                                }
                            }
                        }
                    }.runTaskTimer(RaidEvents.getInstance(), 20L, 20L);
                }
            });
            this.chestLocations.add(location);
            this.createRegion();
            new BukkitRunnable() {

                public void run() {
                    if (!AirDrop.this.opened && AirDrop.this.openAt - System.currentTimeMillis() <= 0L) {
                        Bukkit.getScheduler().runTask(RaidEvents.getInstance(), AirDrop.this::openAirDrop);
                    } else if (AirDrop.this.stopAt - System.currentTimeMillis() <= 0L) {
                        Bukkit.getScheduler().runTask(RaidEvents.getInstance(), AirDrop.this::stop);
                        this.cancel();
                    }
                    //AirDrop.this.createHolograms();
                }
            }.runTaskTimerAsynchronously(RaidEvents.getInstance(), 0L, 20L);
            new BukkitRunnable() {

                public void run() {
                    double radius = 1.0;
                    for (double angle = 0.0; angle <= Math.PI * 2; angle += 0.20943951023931953) {
                        double x = location.clone().add(0.5, 0.5, 0.5).getX() + radius * Math.cos(angle);
                        double z = location.clone().add(0.5, 0.5, 0.5).getZ() + radius * Math.sin(angle);
                        new ParticleBuilder(Particle.REDSTONE).location(new Location(world, x, location.getY(), z)).count(1).offset(0.0, 0.0, 0.0).data((Object) new Particle.DustOptions(AirDrop.this.color, 1.0f)).spawn();
                    }
                    if (AirDrop.this.stopAt - System.currentTimeMillis() <= 0L) {
                        this.cancel();
                    }
                }
            }.runTaskTimerAsynchronously(RaidEvents.getInstance(), 0L, 3L);
            Bukkit.getOnlinePlayers().forEach(player -> {
                Colorize.sendMessage(player, "&f");
                Colorize.sendMessage(player, "&9 &n┃&r " + this.name + " &fбудет открыта через &x&f&e&c&2&2&310 минут.");
                Colorize.sendMessage(player, "&9 &n┃&f Редкость: &x&f&e&c&2&2&3" + this.rarity);
                Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                Colorize.sendMessage(player, "&f");

                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
            });
            RaidEvents.getInstance().getEventManager().getCurrentAirDrops().put(this.uuid, this);
        });
    }

    public void announce(Player player) {
        if (this.openAt < System.currentTimeMillis()) {
            return;
        }
        Colorize.sendMessage(player, "&f");
        Colorize.sendMessage(player, "&9 &n┃&r " + this.name + " &fбудет открыта через &x&f&e&c&2&2&3" + Time.prettyTime((this.openAt - System.currentTimeMillis()) / 1000L));
        Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3" + this.chestLocations.get(0).getBlockX() + ", " + this.chestLocations.get(0).getBlockY() + ", " + this.chestLocations.get(0).getBlockZ());
        Colorize.sendMessage(player, "&f");

    }

    public void openAirDrop() {
        Location location = this.chestLocations.get(0);
        Bukkit.getScheduler().runTaskLater(RaidEvents.getInstance(), () -> {
            this.opened = true;
            BlockState patt0$temp = location.getBlock().getState();
            if (patt0$temp instanceof Barrel barrel) {
                barrel.open();
            }
            new BukkitRunnable() {

                public void run() {
                    if (AirDrop.this.chestContent.isEmpty()) {
                        this.cancel();
                        return;
                    }
                    ArrayList<Integer> keysAsArray = new ArrayList<>(AirDrop.this.chestContent.keySet());
                    int randomKey = keysAsArray.get(ThreadLocalRandom.current().nextInt(keysAsArray.size()));
                    ItemStack itemStack = AirDrop.this.chestContent.getOrDefault(randomKey, null);
                    if (itemStack != null) {
                        AirDrop.this.inventory.setItem(randomKey, itemStack);
                        ArrayList<Integer> emptySlots = new ArrayList<>();
                        for (int i = 0; i < AirDrop.this.gui.getRows() * 9; ++i) {
                            if (AirDrop.this.gui.getGuiItems().containsKey(i)) continue;
                            emptySlots.add(i);
                        }
                        int randomSlot = emptySlots.get(ThreadLocalRandom.current().nextInt(emptySlots.size()));
                        AirDrop.this.gui.updateItem(randomSlot,ItemBuilder.from(AirDrop.this.randomMaterials
                                .get(ThreadLocalRandom.current().nextInt(AirDrop.this.randomMaterials.size())))
                                .setName(Colorize.format("&x&f&8&9&d&5&7Секретный предмет"))
                                .asGuiItem(event ->{
                                    Player player = (Player) event.getWhoClicked();
                                    if (AirDrop.this.clickItemCooldown.getIfPresent(player.getUniqueId()) != null) {
                                        return;
                                    }
                                    AirDrop.this.clickItemCooldown.put(player.getUniqueId(), true);
                                    AirDrop.this.randomMaterials.forEach(m -> player.setCooldown(m, 3));
                                    AirDrop.this.gui.removeItem(randomSlot);
                                    Utils.giveOrDrop(player, itemStack);
                                        }));
                    }
                    AirDrop.this.chestContent.remove(randomKey);
                }
            }.runTaskTimer(RaidEvents.getInstance(), 0L, 2L);
        }, 3L);
        for (Player nearbyPlayer : location.getNearbyPlayers(15.0)) {
            Vector direction = nearbyPlayer.getLocation().toVector().subtract(location.clone().subtract(0.0, 1.0, 0.0).toVector());
            direction.normalize();
            nearbyPlayer.setVelocity(direction);
        }
        if (this.explode) {
            for (Player player : location.getWorld().getPlayers()) {
                double distance = player.getLocation().distance(location);
                if (distance > 50.0) continue;
                double baseDamage = (800.0 * Math.pow(1.0 - distance / 30.0, 2.0) + 1.0) / 2.0;
                player.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                player.damage(baseDamage);
            }
            location.getNearbyEntities(15.0, 15.0, 15.0).forEach(entity -> {
                if (entity instanceof Player nearbyPlayer) {
                    nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
                }
            });
            int radius = 11;
            int xStart = location.getBlockX() - radius;
            int xEnd = location.getBlockX() + radius;
            int zStart = location.getBlockZ() - radius;
            int zEnd = location.getBlockZ() + radius;
            for (int x = xStart; x <= xEnd; ++x) {
                for (int z = zStart; z <= zEnd; ++z) {
                    Location loc;
                    if (AirDrop.isOutsideCircle(x, z, location.getBlockX(), location.getBlockZ(), radius) || ThreadLocalRandom.current().nextInt(0, 5) != 0 || (loc = new Location(location.getWorld(), x, location.getWorld().getHighestBlockYAt(x, z), z)).equals(location))
                        continue;
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc.clone().add(0.0, 1.0, 0.0), 1);
                    if (Tag.LEAVES.isTagged(loc.getBlock().getType()) || !loc.getBlock().getType().isSolid()) continue;
                    int blockType = ThreadLocalRandom.current().nextInt(0, 5);
                    if (blockType == 0) {
                        loc.getBlock().setType(Material.OBSIDIAN);
                        continue;
                    }
                    if (blockType == 1) {
                        loc.getBlock().setType(Material.CRYING_OBSIDIAN);
                        continue;
                    }
                    if (blockType == 2) {
                        loc.getBlock().setType(Material.MAGMA_BLOCK);
                        continue;
                    }
                    if (blockType != 3 && blockType != 4) continue;
                    loc.getBlock().setType(Material.BLACKSTONE);
                }
            }
        }
    }

    /*
    public void createHolograms() {
        this.createDecentHologram();
    }*/


    public void createDecentHologram() {
        List<String> lines = List.of(
                Colorize.format(this.name + " &7/ " + this.rarity),
                Colorize.format(this.opened
                        ? (this.openAt - System.currentTimeMillis() >= -15000L
                        ? "&x&a&c&f&a&5&dУже открыто!"
                        : "&fДо исчезновения: &x&f&e&c&2&2&3" + Time.prettyTime((this.stopAt - System.currentTimeMillis()) / 1000L))
                        : "&fДо открытия: &x&f&e&c&2&2&3" + Time.prettyTime((this.openAt - System.currentTimeMillis() + 150L) / 1000L)
                )
        );

       try {
           if (DHAPI.getHologram("raidevents_" + this.uuid) == null) {
               this.decentHologram = HologramUtils.createHologram("raidevents_" + this.uuid, this.chestLocations.get(0).clone().add(0.5, 1.5, 0.5), false, lines);
               return;
           }
       }catch (Exception e){
           throw new RuntimeException(e);
       }
        DHAPI.setHologramLines(this.decentHologram, lines);
    }


    @Override
    public void stop() {
        RaidEvents.getInstance().getEventManager().getCurrentAirDrops().remove(this.uuid);
        DHAPI.removeHologram("raidevents_" + this.uuid);
        this.chestLocations.forEach(location -> location.getBlock().setType(Material.AIR));
        this.inventory.clear();
        this.gui.getGuiItems().forEach((integer, guiItem) -> this.gui.removeItem(integer));
        this.playersInGui.forEach(HumanEntity::closeInventory);
        this.removeRegion();
    }

    public void createRegion() {
        Location location = this.chestLocations.get(0);
        int radius = 20;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

        Location lowestCorner = location.clone().subtract(radius, 0.0, radius);
        Location highestCorner = location.clone().add(radius, 0.0, radius);

        BlockVector3 min = BlockVector3.at(lowestCorner.getX(), 0, lowestCorner.getZ());
        BlockVector3 max = BlockVector3.at(highestCorner.getX(), location.getWorld().getMaxHeight(), highestCorner.getZ());

        ProtectedCuboidRegion protectedRegion = new ProtectedCuboidRegion("raidevents_" + this.uuid, true, min, max);

        // Set PVP flag based on allowPvP
        protectedRegion.setFlag(Flags.PVP, this.allowPvP ? StateFlag.State.ALLOW : StateFlag.State.DENY);
        if (!this.allowPvP) {
            protectedRegion.setFlag(Flags.INVINCIBILITY, StateFlag.State.ALLOW);
        }

        // Set other flags
        setFlagsTORegion(protectedRegion);

        if (regions != null) {
            regions.addRegion(protectedRegion);
        }
    }

    static void setFlagsTORegion(ProtectedCuboidRegion protectedRegion) {
        protectedRegion.setFlag(Flags.POTION_SPLASH, StateFlag.State.ALLOW);
        Mine.setFlagRegions(protectedRegion);
        protectedRegion.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
        protectedRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.LIGHTER, StateFlag.State.DENY);
        protectedRegion.setFlag(Flags.BLOCKED_CMDS, Set.of("/gsit", "/sit", "/lay", "/crawl", "/bellyflop"));
    }

    public void removeRegion() {
        Location location = this.chestLocations.get(0);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regions != null) {
            regions.removeRegion("raidevents_" + this.uuid);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AirDrop other)) return false;
        return other.equals(this) && super.equals(o) && chestCount == other.chestCount && spawnAt == other.spawnAt &&
                openAt == other.openAt && stopAt == other.stopAt && opened == other.opened && explode == other.explode &&
                allowPvP == other.allowPvP && Objects.equals(chestLocations, other.chestLocations) && Objects.equals(chestContent, other.chestContent) &&
                Objects.equals(inventory, other.inventory) && Objects.equals(gui, other.gui) && Objects.equals(lootContent, other.lootContent) && Objects.equals(decentHologram, other.decentHologram) &&
                Objects.equals(material, other.material) &&
                Objects.equals(color, other.color) &&
                Objects.equals(playersInGui, other.playersInGui) &&
                Objects.equals(clickItemCooldown, other.clickItemCooldown) &&
                Objects.equals(randomMaterials, other.randomMaterials);
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), chestCount, spawnAt, openAt, stopAt, opened, explode, allowPvP,
                chestLocations, chestContent, inventory, gui, lootContent, decentHologram,
                material, color, playersInGui, clickItemCooldown, randomMaterials);
    }

    @Override
    public String toString() {
        return String.format("AirDrop(chestCount=%d, spawnAt=%d, openAt=%d, stopAt=%d, opened=%b, explode=%b, allowPvP=%b, " +
                        "chestLocations=%s, chestContent=%s, inventory=%s, gui=%s, lootContent=%s, " +
                        "decentHologram=%s, material=%s, color=%s, playersInGui=%s, clickItemCooldown=%s, " +
                        "randomMaterials=%s)",
                chestCount, spawnAt, openAt, stopAt, opened, explode, allowPvP,
                chestLocations, chestContent, inventory, gui, lootContent,
                decentHologram, material, color, playersInGui, clickItemCooldown,
                randomMaterials);
    }
}
