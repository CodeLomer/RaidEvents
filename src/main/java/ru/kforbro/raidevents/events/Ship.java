package ru.kforbro.raidevents.events;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lol.pyr.znpcsplus.api.NpcApiProvider;
import lol.pyr.znpcsplus.api.entity.EntityPropertyRegistry;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.api.npc.NpcEntry;
import lol.pyr.znpcsplus.api.npc.NpcRegistry;
import lol.pyr.znpcsplus.api.npc.NpcTypeRegistry;
import lol.pyr.znpcsplus.api.skin.SkinDescriptor;
import lol.pyr.znpcsplus.api.skin.SkinDescriptorFactory;
import lol.pyr.znpcsplus.util.LookType;
import lol.pyr.znpcsplus.util.NpcLocation;
import lol.pyr.znpcsplus.util.ParrotVariant;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.config.Loot;
import ru.kforbro.raidevents.gui.guis.Gui;
import ru.kforbro.raidevents.utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class Ship extends Event {
    @Setter
    private Location location;
    @Setter
    private Location currentNpcLocation = null;
    @Setter
    private Location chestLocation;
    @Setter
    private HashMap<Integer, ItemStack> chestContent = new HashMap<>();
    @Setter
    private long spawnAt;
    @Setter
    private long stopAt;
    @Setter
    private long spawnChestAt;
    @Setter
    private boolean chestSpawned;
    @Setter
    private Gui gui;
    @Setter
    private Map<String, Loot.LootContent> loot;
    @Setter
    private Loot.LootContent mythicLootContent;
    @Setter
    private EditSession editSession;
    @Setter
    private int keyInterval;
    @Setter
    private String schematic;
    private boolean keyAvailable = true;
    @Setter
    private List<ShipBarrel> barrels = new ArrayList<>();
    @Setter
    private List<ShipTreasure> treasures = new ArrayList<>();
    @Setter
    private Npc npc;
    @Setter
    public NpcRegistry npcRegistry;
    @Setter
    public NpcTypeRegistry npcTypeRegistry;
    @Setter
    public EntityPropertyRegistry entityPropertyRegistry;
    @Setter
    public SkinDescriptorFactory skinDescriptorFactory;
    @Setter
    private HashSet<Player> playersInGui = new HashSet<>();
    public final HashMap<UUID, Integer> leaveTime = new HashMap<>();
    @Setter
    private Map<String, Double> keyChances;
    private static final HashSet<Material> waterMaterials = new HashSet<>(List.of(Material.WATER, Material.ICE, Material.BLUE_ICE, Material.FROSTED_ICE, Material.PACKED_ICE));

    public Ship(String name, String rarity, String schematic, Map<String, Loot.LootContent> loot, Loot.LootContent mythicLootContent, int keyInterval, Map<String, Double> keyChances) {
        this.name = name;
        this.rarity = rarity;
        this.schematic = schematic;
        this.loot = loot;
        this.mythicLootContent = mythicLootContent;
        this.keyInterval = keyInterval;
        this.keyChances = keyChances;
        this.npcRegistry = NpcApiProvider.get().getNpcRegistry();
        this.npcTypeRegistry = NpcApiProvider.get().getNpcTypeRegistry();
        this.entityPropertyRegistry = NpcApiProvider.get().getPropertyRegistry();
        this.skinDescriptorFactory = NpcApiProvider.get().getSkinDescriptorFactory();
    }

    @Override
    public void start() {
        CompletableFuture.runAsync(() -> {
            World world = Bukkit.getWorld("world");
            this.spawnAt = System.currentTimeMillis();
            this.stopAt = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(10, 16) * 60L * 1000L;
            Clipboard clipboard = this.getClipboard();
            if (clipboard == null) {
                return;
            }
            assert world != null;
            RandomLocation.SafeLocation safeLocation = Ship.getRandomSafeLocation(world, RandomLocation.Algorithm.SQUARE, 500.0, world.getWorldBorder().getSize() / 2.0 - 50.0, 0, 0, clipboard);
            if(safeLocation == null){
                throw new RuntimeException("filed find safe location for event " + this.name);
            }
            RaidEvents.getInstance().getEventManager().setCurrentShip(this);
            this.location = safeLocation.location().add(0.0, 1.0, 0.0);
            this.pasteClipboard(this.location, clipboard);
            this.createRegion(clipboard);
            this.spawnNpc();
            new BukkitRunnable() {

                public void run() {
                    if (Ship.this.stopAt - System.currentTimeMillis() <= 0L) {
                        Bukkit.getScheduler().runTask(RaidEvents.getInstance(), Ship.this::stop);
                        this.cancel();
                        return;
                    }
                    for (ShipTreasure treasure : Ship.this.treasures) {
                        if (!treasure.isOpened() && treasure.getCurrentKeys() >= treasure.getNeedKeys() && treasure.getOpenAt() < System.currentTimeMillis()) {
                            treasure.setOpened(true);
                        }
                        treasure.createHologram();
                    }
                }
            }.runTaskTimerAsynchronously(RaidEvents.getInstance(), 0L, 20L);
            new BukkitRunnable() {

                public void run() {
                    if (Ship.this.stopAt - System.currentTimeMillis() <= 0L) {
                        this.cancel();
                        return;
                    }
                    Ship.this.setKeyAvailable(true);
                }
            }.runTaskTimer(RaidEvents.getInstance(), 0L, (long) this.keyInterval * 20L);
            new BukkitRunnable() {

                public void run() {
                    if (Ship.this.stopAt - System.currentTimeMillis() <= 0L) {
                        this.cancel();
                        return;
                    }
                    for (ShipBarrel barrel : Ship.this.barrels) {
                        barrel.despawn();
                        barrel.spawn();
                    }
                }
            }.runTaskTimer(RaidEvents.getInstance(), 0L, 2400L);
            Bukkit.getOnlinePlayers().forEach(player -> {
                Colorize.sendMessage(player, "&f");
                Colorize.sendMessage(player, "&9 &n┃&r " + this.name + " &fпоявился на карте.");
                Colorize.sendMessage(player, "&9 &n┃&f Редкость: &x&f&e&c&2&2&3" + this.rarity);
                Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3" + this.location.getBlockX() + ", " + this.location.getBlockY() + ", " + this.location.getBlockZ());
                Colorize.sendMessage(player, "&f");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);
            });

            RaidEvents.getInstance().getEventManager().setCurrentShip(this);
        });
    }

    public void announce(Player player) {
        if (this.stopAt < System.currentTimeMillis()) {
            return;
        }
        Colorize.sendMessage(player, "&f");
        Colorize.sendMessage(player, "&9 &n┃&r " + this.name + " &fактивен уже &x&f&e&c&2&2&3" + Time.prettyTime((System.currentTimeMillis() - this.spawnAt) / 1000L));
        Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3" + this.location.getBlockX() + ", " + this.location.getBlockY() + ", " + this.location.getBlockZ());
        Colorize.sendMessage(player, "&f");

    }

    public void spawnNpc() {
        List<ParrotVariant> parrotVariants = List.of(ParrotVariant.BLUE, ParrotVariant.GRAY, ParrotVariant.GREEN, ParrotVariant.RED_BLUE, ParrotVariant.YELLOW_BLUE);

        // Создание NPC
        NpcEntry npcEntry = this.npcRegistry.create("raidevents_" + this.uuid, this.currentNpcLocation.getWorld(), this.npcTypeRegistry.getByName("PLAYER"), new NpcLocation(this.currentNpcLocation));

        this.npc = npcEntry.getNpc();

        // Настройка NPC
        this.npc.setProperty(this.entityPropertyRegistry.getByName("skin", SkinDescriptor.class), this.skinDescriptorFactory.createStaticDescriptor("ewogICJ0aW1lc3RhbXAiIDogMTcxNjEwODY3MDQ1OCwKICAicHJvZmlsZUlkIiA6ICJkZTU3MWExMDJjYjg0ODgwOGZlN2M5ZjQ0OTZlY2RhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfTWluZXNraW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjc4ZTJkMDZiYjYyYWI0OTI5MjFlZTY4ZGU2MjRlZjRkYzA5N2ZjOTdiMGNhODY0MDlkOWY1MWZlMTVhNDdiYyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "Gt7hLgnmK8jMKr4MZ+5JiWaIxIUx+FRj0m+D9xz/ZkLH89+Fd8lCYq2oTWi1TfURTl0wcWzik7U9mi2+9G0FHfsxiVBC+2UEYS04tMUb3Kw1lZyzXUReutndcQ3NAb5+3FsC5mvqqb/ucdOjJawgKju9yrje+nsKZp/p5siKxP1uWLNketf2TkZ3t5BnisgoFvYbZ1LKtCLcuOn8WCKra68xyRoy8gjois7OuF+8Z/QrLiNYIQImgDlwWg/mMcgMINf6fyVBHe1naOWfiXgX0Vr1TaNs9awh3nFMi9i5i0gdWMe2ciqnYSxO5hKZMk8Xij/slfiGkjeKN5dOjSE6d8lF6kvc00Rx/uujRZzC0k3l99Q+iVJvzvXEF5+PiFwGHbLLVP9s1t4Rwj4+UoPGqS76/uacXM1Irjjj4R8Umth400C++p2zG1+wsmOf2xtsfVEJswr2uxHL+vpbE/oFRTHEaxXDTOIFfHhFjewQce0eNl2VW1zPXiLQGrlttSR2bJrd2GxkiE98FC3pOhdO1/4dz0goDYl9+qrb4MX4aj2DCsw5uGphlKQ8D3rrKz9MsY1jXYNZKZVwXIeyFmGwSAzrxPRHQdtv5ydVdanVvV82sg9ovaTbpDIqU1Z/obFnZjXJTQh3aPRqdAEOXBmJngFTNV7Y/3XQLhZiqsA7NRY="));

        setShipProperties(parrotVariants, this.npc, this.entityPropertyRegistry);

        // Добавление голограммы
        this.npc.getHologram().addLine("&x&C&9&9&8&4&3Пират");
        this.npc.getHologram().addLine("&fНажмите, чтобы взять");

        npcEntry.setProcessed(true);
    }

    static void setShipProperties(List<ParrotVariant> parrotVariants, Npc npc, EntityPropertyRegistry entityPropertyRegistry) {
        npc.setProperty(entityPropertyRegistry.getByName("look", LookType.class), LookType.PER_PLAYER);
        npc.setProperty(entityPropertyRegistry.getByName("look_distance", Double.class), 48.0);
        npc.setProperty(entityPropertyRegistry.getByName("shoulder_entity_left", ParrotVariant.class), parrotVariants.get(ThreadLocalRandom.current().nextInt(parrotVariants.size())));
    }


    public void setKeyAvailable(boolean state) {
        if (state) {
            // Создание предмета с зачарованием
            ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

            // Устанавливаем предмет в руку NPC и обновляем голограмму
            this.npc.setProperty(this.entityPropertyRegistry.getByName("hand", ItemStack.class), item);
            this.npc.getHologram().removeLine(1);
            this.npc.getHologram().addLine("&fНажмите, чтобы взять");

            // Устанавливаем флаг доступности ключа
            this.keyAvailable = true;

            // Проигрываем звук для близлежащих игроков
            this.currentNpcLocation.getNearbyPlayers(20.0).forEach(nearbyPlayer -> nearbyPlayer.playSound(this.currentNpcLocation, Sound.ENTITY_PIGLIN_ADMIRING_ITEM, 1.0f, 1.0f));
        } else {
            // Убираем предмет из руки NPC и обновляем голограмму
            this.keyAvailable = false;
            this.npc.setProperty(this.entityPropertyRegistry.getByName("hand", ItemStack.class), new ItemStack(Material.AIR));
            this.npc.getHologram().removeLine(1);
            this.npc.getHologram().addLine("&fОтметки пока нет");
        }
    }


    @Override
    public void stop() {
        RaidEvents.getInstance().getEventManager().setCurrentWanderer(null);
        this.location.getNearbyPlayers(30.0, 30.0, 30.0).forEach(p -> p.setVelocity(new Vector(0, 1, 0).multiply(2)));
        if (this.npcRegistry.getById("raidevents_" + this.uuid) != null) {
            this.npcRegistry.delete("raidevents_" + this.uuid);
        }
        for (ShipBarrel barrel : this.barrels) {
            barrel.despawn();
        }
        for (ShipTreasure treasure : this.treasures) {
            treasure.despawn();
        }
        if (RaidEvents.getInstance().isDisabling()) {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(this.location.getWorld()))) {
                this.editSession.undo(editSession);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(RaidEvents.getInstance(), () -> {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(this.location.getWorld()))) {
                    this.editSession.undo(editSession);
                }
            }, 20L);
        }
        this.gui.getGuiItems().forEach((integer, guiItem) -> this.gui.removeItem(integer));
        this.playersInGui.forEach(HumanEntity::closeInventory);
        this.removeRegion();
        RaidEvents.getInstance().getEventManager().setCurrentShip(null);
    }

    public void createRegion(Clipboard clipboard) {
        int additionalRadius = 15;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(this.location.getWorld()));

        Location cornerFirst = this.location.clone().add(Ship.getSchematicOffset(clipboard));
        Location cornerSecond = cornerFirst.clone().add(new Vector(clipboard.getRegion().getWidth() - 1, clipboard.getRegion().getHeight(), clipboard.getRegion().getLength() - 1));

        Location lowestCorner = new Location(cornerFirst.getWorld(), Math.min(cornerFirst.getX(), cornerSecond.getX()), Math.min(cornerFirst.getY(), cornerSecond.getY()), Math.min(cornerFirst.getZ(), cornerSecond.getZ())).subtract(additionalRadius, 0.0, additionalRadius);

        Location highestCorner = new Location(cornerFirst.getWorld(), Math.max(cornerFirst.getX(), cornerSecond.getX()), Math.max(cornerFirst.getY(), cornerSecond.getY()), Math.max(cornerFirst.getZ(), cornerSecond.getZ())).add(additionalRadius, 0.0, additionalRadius);

        BlockVector3 min = BlockVector3.at(lowestCorner.getX(), 0, lowestCorner.getZ());
        BlockVector3 max = BlockVector3.at(highestCorner.getX(), this.location.getWorld().getMaxHeight(), highestCorner.getZ());

        ProtectedCuboidRegion protectedRegion = new ProtectedCuboidRegion("raidevents_" + this.uuid, true, min, max);

        // Применение флагов через метод для упрощения
        applyRegionFlags(protectedRegion);
        if (regions == null) {
            throw new RuntimeException("filed load regions for event " + this.name);
        }

        regions.addRegion(protectedRegion);
    }

    private void applyRegionFlags(ProtectedCuboidRegion region) {
        // Флаги с разрешениями
        Map<StateFlag, StateFlag.State> allowFlags = Map.of(Flags.PVP, StateFlag.State.ALLOW, Flags.POTION_SPLASH, StateFlag.State.ALLOW, Flags.MOB_DAMAGE, StateFlag.State.ALLOW, Flags.DAMAGE_ANIMALS, StateFlag.State.ALLOW, Flags.DESTROY_VEHICLE, StateFlag.State.ALLOW, Flags.CHEST_ACCESS, StateFlag.State.ALLOW);

        // Флаги с запретами
        Map<StateFlag, StateFlag.State> denyFlags = Map.of(Flags.TNT, StateFlag.State.DENY, Flags.OTHER_EXPLOSION, StateFlag.State.DENY, Flags.FIRE_SPREAD, StateFlag.State.DENY, Flags.LIGHTER, StateFlag.State.DENY, Flags.ICE_FORM, StateFlag.State.DENY, Flags.SNOW_FALL, StateFlag.State.DENY);

        // Установка разрешающих флагов
        allowFlags.forEach(region::setFlag);

        // Установка запрещающих флагов
        denyFlags.forEach(region::setFlag);

        // Блокировка команд
        region.setFlag(Flags.BLOCKED_CMDS, Set.of("/gsit", "/sit", "/lay", "/crawl", "/bellyflop"));
    }


    public void removeRegion() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(this.location.getWorld()));
        if (regions != null) {
            regions.removeRegion("raidevents_" + this.uuid);
        }
    }

    public static Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(clipboard.getMinimumPoint().getX() - clipboard.getOrigin().getX(), clipboard.getMinimumPoint().getY() - clipboard.getOrigin().getY(), clipboard.getMinimumPoint().getZ() - clipboard.getOrigin().getZ());
    }

    public Clipboard getClipboard() {
        Clipboard clipboard;
        File file = new File("plugins/worldEdit/schematics/" + this.schematic + ".schem");
        if(!file.exists() || !file.getName().endsWith(".schem")){
            MyLogger.logError(this,"не удалось найти файл cхематики для данного ивента -> "+schematic+".schem");
            return null;
        }
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            throw new RuntimeException("filed load clipboard for event " + this.name);
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return clipboard;
    }

    public void pasteClipboard(Location location, Clipboard clipboard) {
        Location adjustedLocation = location.clone().add(Ship.getSchematicOffset(clipboard));
        for (int x = 0; x < clipboard.getDimensions().getX(); ++x) {
            for (int y = 0; y < clipboard.getDimensions().getY(); ++y) {
                for (int z = 0; z < clipboard.getDimensions().getZ(); ++z) {
                    BlockVector3 adjustedClipboardLocation = BlockVector3.at(x + clipboard.getMinimumPoint().getX(), y + clipboard.getMinimumPoint().getY(), z + clipboard.getMinimumPoint().getZ());
                    BlockState blockState = clipboard.getBlock(adjustedClipboardLocation);
                    Material material = BukkitAdapter.adapt(blockState.getBlockType());
                    Block worldBlock = adjustedLocation.clone().add(new Vector(x, y, z)).getBlock();
                    if (material == Material.BARRIER) {
                        try {
                            clipboard.setBlock(adjustedClipboardLocation, BukkitAdapter.adapt(worldBlock.getBlockData()));
                            continue;
                        } catch (WorldEditException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (material == Material.SMITHING_TABLE) {
                        this.currentNpcLocation = worldBlock.getLocation().clone().add(0.5, 1.0, 0.5);
                        continue;
                    }
                    if (material == Material.BARREL) {
                        this.barrels.add(new ShipBarrel(worldBlock.getLocation(), this));
                        continue;
                    }
                    if (material != Material.RESPAWN_ANCHOR) continue;
                    this.treasures.add(new ShipTreasure(worldBlock.getLocation(), this));
                }
            }
        }
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(BlockVector3.at(location.getX(), location.getY(), location.getZ())).build();
            Operations.complete(operation);
            this.editSession = editSession;
        } catch (WorldEditException e) {
            throw new RuntimeException("Failed to paste schematic", e);
        }
    }

    public ItemStack generateItemStack() {
        ItemStack itemStack = Utils.getCustomSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzk5YWQ3YTA0MzE2OTI5OTRiNmM0MTJjN2VhZmI5ZTBmYzQ5OTc1MjQwYjczYTI3ZDI0ZWQ3OTcwMzVmYjg5NCJ9fX0=");
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(Colorize.format("&x&7&e&f&c&2&0Экспириум"));

            List<String> lore = Arrays.asList(Colorize.format(" &f"), Colorize.format(" &fОбладая этим предметом, вы &x&7&e&f&c&2&0показываете секунду"), Colorize.format(" &x&7&e&f&c&2&0получаете опыт в 12 блоках"), Colorize.format(" &fот &x&7&e&f&c&2&0странника&f, но если вы окажетесь слишком"), Colorize.format(" &fдалеко от него, предмет исчезнет."), Colorize.format(""), Colorize.format(" &fЧем больше экспириума&f, тем больше опыта получаете."));

            itemMeta.setLore(lore);
            NamespacedKey namespacedKey = new NamespacedKey(RaidEvents.getInstance(), "wandereritem");
            itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, this.uuid.toString());

            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }


    public static RandomLocation.SafeLocation getRandomSafeLocation(World w, RandomLocation.Algorithm a, double startRadius, double endRadius, int originX, int originY, Clipboard clipboard) {
        Location loc;
        int tries = 0;
        while (true) {
            RandomLocation.ChunkLocationSnapshot cl = RandomLocation.getRandomLocation(w, a, startRadius, endRadius, originX, originY);
            if(cl == null) continue;
            loc = cl.location();
            if (++tries < 75) {
                loc.setY(62.0);
            }
            ChunkSnapshot cs = cl.chunkSnapshot();
            if (RandomLocation.isInRegion(loc)) continue;
            int chunkX = loc.getBlockX() & 0xF;
            int chunkZ = loc.getBlockZ() & 0xF;
            BlockData b = cs.getBlockData(chunkX, loc.getBlockY(), chunkZ);
            if (Ship.isWater(cs, loc, clipboard)) {
                return new RandomLocation.SafeLocation(tries, loc);
            }
            if (tries > 100 && waterMaterials.contains(b.getMaterial())) {
                return new RandomLocation.SafeLocation(tries, loc);
            }
            if (tries > 150 && b.getMaterial().isSolid()) break;
        }
        return new RandomLocation.SafeLocation(tries, loc);
    }

    private static boolean isWater(ChunkSnapshot cs, Location location, Clipboard clipboard) {
        Location cornerFirst = location.clone().add(Ship.getSchematicOffset(clipboard));
        Location cornerSecond = cornerFirst.clone().add(new Vector(clipboard.getRegion().getWidth() - 1, clipboard.getRegion().getHeight(), clipboard.getRegion().getLength() - 1));
        Location lowestCorner = new Location(cornerFirst.getWorld(), Math.min(cornerFirst.getX(), cornerSecond.getX()), Math.min(cornerFirst.getY(), cornerSecond.getY()), Math.min(cornerFirst.getZ(), cornerSecond.getZ()));
        Location highestCorner = new Location(cornerFirst.getWorld(), Math.max(cornerFirst.getX(), cornerSecond.getX()), Math.max(cornerFirst.getY(), cornerSecond.getY()), Math.max(cornerFirst.getZ(), cornerSecond.getZ()));
        Location corner_1 = new Location(location.getWorld(), highestCorner.getBlockX(), 62.0, highestCorner.getBlockZ());
        Location corner_2 = new Location(location.getWorld(), highestCorner.getBlockX(), 62.0, lowestCorner.getBlockZ());
        Location corner_3 = new Location(location.getWorld(), lowestCorner.getBlockX(), 62.0, highestCorner.getBlockZ());
        Location corner_4 = new Location(location.getWorld(), lowestCorner.getBlockX(), 62.0, lowestCorner.getBlockZ());
        BlockData b_1 = cs.getBlockData(corner_1.getBlockX() & 0xF, 62, corner_1.getBlockZ() & 0xF);
        BlockData b_2 = cs.getBlockData(corner_2.getBlockX() & 0xF, 62, corner_2.getBlockZ() & 0xF);
        BlockData b_3 = cs.getBlockData(corner_3.getBlockX() & 0xF, 62, corner_3.getBlockZ() & 0xF);
        BlockData b_4 = cs.getBlockData(corner_4.getBlockX() & 0xF, 62, corner_4.getBlockZ() & 0xF);
        return waterMaterials.containsAll(List.of(b_1.getMaterial(), b_2.getMaterial(), b_3.getMaterial(),  b_4.getMaterial()));
    }

    public ShipBarrel getBarrel(Location location) {
        for (ShipBarrel barrel : this.barrels) {
            if (!barrel.getLocation().equals(location)) continue;
            return barrel;
        }
        return null;
    }

    public ShipTreasure getTreasure(Location location) {
        for (ShipTreasure treasure : this.treasures) {
            if (!treasure.getLocation().equals(location)) continue;
            return treasure;
        }
        return null;
    }

    private String getRandomKeyName() {
        return WeighedProbability.pickStringDouble(this.keyChances);
    }

    public ItemStack getRandomKeyItemStack() {
        String keyName = this.getRandomKeyName();
        ItemStack itemStack = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            if (keyName.equalsIgnoreCase("common")) {
                itemMeta.setDisplayName(Colorize.format("&x&a&8&e&b&f&cПиратская отмычка"));
                itemMeta.setLore(List.of(Colorize.format(""), Colorize.format("&8 • &fПозволяет украсть сокровища пиратов с таинственного корабля."), Colorize.format(""), Colorize.format("&8 • &fРедкость: &x&a&8&e&b&f&cОбычная")));
            } else if (keyName.equalsIgnoreCase("rare")) {
                itemMeta.setDisplayName(Colorize.format("&x&3&e&d&8&3&eПиратская отмычка"));
                itemMeta.setLore(List.of(Colorize.format(""), Colorize.format("&8 • &fПозволяет украсть сокровища пиратов с таинственного корабля."), Colorize.format(""), Colorize.format("&8 • &fРедкость: &x&3&e&d&8&3&eРедкая")));
            } else if (keyName.equalsIgnoreCase("unique")) {
                itemMeta.setDisplayName(Colorize.format("&x&d&1&4&8&e&eПиратская отмычка"));
                itemMeta.setLore(List.of(Colorize.format(""), Colorize.format("&8 • &fПозволяет украсть сокровища пиратов с таинственного корабля."), Colorize.format(""), Colorize.format("&8 • &fРедкость: &x&d&1&4&8&e&eУникальная")));
            } else if (keyName.equalsIgnoreCase("secret")) {
                itemMeta.setDisplayName(Colorize.format("&x&f&a&5&d&5&dПиратская отмычка"));
                itemMeta.setLore(List.of(Colorize.format(""), Colorize.format("&8 • &fПозволяет украсть сокровища пиратов с таинственного корабля."), Colorize.format(""), Colorize.format("&8 • &fРедкость: &x&f&a&5&d&5&dСекретная")));
            }

            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(RaidEvents.getInstance(), "ship_key"), PersistentDataType.STRING, keyName);

            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Ship other)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (this.getSpawnAt() != other.getSpawnAt()) {
            return false;
        }
        if (this.getStopAt() != other.getStopAt()) {
            return false;
        }
        if (this.getSpawnChestAt() != other.getSpawnChestAt()) {
            return false;
        }
        if (this.isChestSpawned() != other.isChestSpawned()) {
            return false;
        }
        if (this.getKeyInterval() != other.getKeyInterval()) {
            return false;
        }
        if (this.isKeyAvailable() != other.isKeyAvailable()) {
            return false;
        }
        Location this$location = this.getLocation();
        Location other$location = other.getLocation();
        if (!Objects.equals(this$location, other$location)) {
            return false;
        }
        Location this$currentNpcLocation = this.getCurrentNpcLocation();
        Location other$currentNpcLocation = other.getCurrentNpcLocation();
        if (!Objects.equals(this$currentNpcLocation, other$currentNpcLocation)) {
            return false;
        }
        Location this$chestLocation = this.getChestLocation();
        Location other$chestLocation = other.getChestLocation();
        if (!Objects.equals(this$chestLocation, other$chestLocation)) {
            return false;
        }
        HashMap<Integer, ItemStack> this$chestContent = this.getChestContent();
        HashMap<Integer, ItemStack> other$chestContent = other.getChestContent();
        if (!Objects.equals(this$chestContent, other$chestContent)) {
            return false;
        }
        Gui this$gui = this.getGui();
        Gui other$gui = other.getGui();
        if (!Objects.equals(this$gui, other$gui)) {
            return false;
        }
        Map<String, Loot.LootContent> this$loot = this.getLoot();
        Map<String, Loot.LootContent> other$loot = other.getLoot();
        if (!Objects.equals(this$loot, other$loot)) {
            return false;
        }
        Loot.LootContent this$mythicLootContent = this.getMythicLootContent();
        Loot.LootContent other$mythicLootContent = other.getMythicLootContent();
        if (!Objects.equals(this$mythicLootContent, other$mythicLootContent)) {
            return false;
        }
        EditSession this$editSession = this.getEditSession();
        EditSession other$editSession = other.getEditSession();
        if (!Objects.equals(this$editSession, other$editSession)) {
            return false;
        }
        String this$schematic = this.getSchematic();
        String other$schematic = other.getSchematic();
        if (!Objects.equals(this$schematic, other$schematic)) {
            return false;
        }
        List<ShipBarrel> this$barrels = this.getBarrels();
        List<ShipBarrel> other$barrels = other.getBarrels();
        if (!Objects.equals(this$barrels, other$barrels)) {
            return false;
        }
        List<ShipTreasure> this$treasures = this.getTreasures();
        List<ShipTreasure> other$treasures = other.getTreasures();
        if (!Objects.equals(this$treasures, other$treasures)) {
            return false;
        }
        Npc this$npc = this.getNpc();
        Npc other$npc = other.getNpc();
        if (!Objects.equals(this$npc, other$npc)) {
            return false;
        }
        NpcRegistry this$npcRegistry = this.getNpcRegistry();
        NpcRegistry other$npcRegistry = other.getNpcRegistry();
        if (!Objects.equals(this$npcRegistry, other$npcRegistry)) {
            return false;
        }
        NpcTypeRegistry this$npcTypeRegistry = this.getNpcTypeRegistry();
        NpcTypeRegistry other$npcTypeRegistry = other.getNpcTypeRegistry();
        if (!Objects.equals(this$npcTypeRegistry, other$npcTypeRegistry)) {
            return false;
        }
        EntityPropertyRegistry this$entityPropertyRegistry = this.getEntityPropertyRegistry();
        EntityPropertyRegistry other$entityPropertyRegistry = other.getEntityPropertyRegistry();
        if (!Objects.equals(this$entityPropertyRegistry, other$entityPropertyRegistry)) {
            return false;
        }
        SkinDescriptorFactory this$skinDescriptorFactory = this.getSkinDescriptorFactory();
        SkinDescriptorFactory other$skinDescriptorFactory = other.getSkinDescriptorFactory();
        if (!Objects.equals(this$skinDescriptorFactory, other$skinDescriptorFactory)) {
            return false;
        }
        HashSet<Player> this$playersInGui = this.getPlayersInGui();
        HashSet<Player> other$playersInGui = other.getPlayersInGui();
        if (!Objects.equals(this$playersInGui, other$playersInGui)) {
            return false;
        }
        HashMap<UUID, Integer> this$leaveTime = this.getLeaveTime();
        HashMap<UUID, Integer> other$leaveTime = other.getLeaveTime();
        if (!Objects.equals(this$leaveTime, other$leaveTime)) {
            return false;
        }
        Map<String, Double> this$keyChances = this.getKeyChances();
        Map<String, Double> other$keyChances = other.getKeyChances();
        return Objects.equals(this$keyChances, other$keyChances);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long $spawnAt = this.getSpawnAt();
        result = result * 59 + Long.hashCode($spawnAt);
        long $stopAt = this.getStopAt();
        result = result * 59 + Long.hashCode($stopAt);
        long $spawnChestAt = this.getSpawnChestAt();
        result = result * 59 + Long.hashCode($spawnChestAt);
        result = result * 59 + (this.isChestSpawned() ? 79 : 97);
        result = result * 59 + this.getKeyInterval();
        result = result * 59 + (this.isKeyAvailable() ? 79 : 97);
        Location $location = this.getLocation();
        result = result * 59 + ($location == null ? 43 : $location.hashCode());
        Location $currentNpcLocation = this.getCurrentNpcLocation();
        result = result * 59 + ($currentNpcLocation == null ? 43 : $currentNpcLocation.hashCode());
        Location $chestLocation = this.getChestLocation();
        result = result * 59 + ($chestLocation == null ? 43 : $chestLocation.hashCode());
        HashMap<Integer, ItemStack> $chestContent = this.getChestContent();
        result = result * 59 + ($chestContent == null ? 43 : $chestContent.hashCode());
        Gui $gui = this.getGui();
        result = result * 59 + ($gui == null ? 43 : $gui.hashCode());
        Map<String, Loot.LootContent> $loot = this.getLoot();
        result = result * 59 + ($loot == null ? 43 : $loot.hashCode());
        Loot.LootContent $mythicLootContent = this.getMythicLootContent();
        result = result * 59 + ($mythicLootContent == null ? 43 : $mythicLootContent.hashCode());
        EditSession $editSession = this.getEditSession();
        result = result * 59 + ($editSession == null ? 43 : $editSession.hashCode());
        String $schematic = this.getSchematic();
        result = result * 59 + ($schematic == null ? 43 : $schematic.hashCode());
        List<ShipBarrel> $barrels = this.getBarrels();
        result = result * 59 + ($barrels == null ? 43 : $barrels.hashCode());
        List<ShipTreasure> $treasures = this.getTreasures();
        result = result * 59 + ($treasures == null ? 43 : $treasures.hashCode());
        Npc $npc = this.getNpc();
        result = result * 59 + ($npc == null ? 43 : $npc.hashCode());
        NpcRegistry $npcRegistry = this.getNpcRegistry();
        result = result * 59 + ($npcRegistry == null ? 43 : $npcRegistry.hashCode());
        NpcTypeRegistry $npcTypeRegistry = this.getNpcTypeRegistry();
        result = result * 59 + ($npcTypeRegistry == null ? 43 : $npcTypeRegistry.hashCode());
        EntityPropertyRegistry $entityPropertyRegistry = this.getEntityPropertyRegistry();
        result = result * 59 + ($entityPropertyRegistry == null ? 43 : $entityPropertyRegistry.hashCode());
        SkinDescriptorFactory $skinDescriptorFactory = this.getSkinDescriptorFactory();
        result = result * 59 + ($skinDescriptorFactory == null ? 43 : $skinDescriptorFactory.hashCode());
        HashSet<Player> $playersInGui = this.getPlayersInGui();
        result = result * 59 + ($playersInGui == null ? 43 : $playersInGui.hashCode());
        HashMap<UUID, Integer> $leaveTime = this.getLeaveTime();
        result = result * 59 + ($leaveTime == null ? 43 : $leaveTime.hashCode());
        Map<String, Double> $keyChances = this.getKeyChances();
        result = result * 59 + ($keyChances == null ? 43 : $keyChances.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Ship(location=" + this.getLocation() + ", currentNpcLocation=" + this.getCurrentNpcLocation() + ", chestLocation=" + this.getChestLocation() + ", chestContent=" + this.getChestContent() + ", spawnAt=" + this.getSpawnAt() + ", stopAt=" + this.getStopAt() + ", spawnChestAt=" + this.getSpawnChestAt() + ", chestSpawned=" + this.isChestSpawned() + ", gui=" + this.getGui() + ", loot=" + this.getLoot() + ", mythicLootContent=" + this.getMythicLootContent() + ", editSession=" + this.getEditSession() + ", keyInterval=" + this.getKeyInterval() + ", schematic=" + this.getSchematic() + ", keyAvailable=" + this.isKeyAvailable() + ", barrels=" + this.getBarrels() + ", treasures=" + this.getTreasures() + ", npc=" + this.getNpc() + ", npcRegistry=" + this.getNpcRegistry() + ", npcTypeRegistry=" + this.getNpcTypeRegistry() + ", entityPropertyRegistry=" + this.getEntityPropertyRegistry() + ", skinDescriptorFactory=" + this.getSkinDescriptorFactory() + ", playersInGui=" + this.getPlayersInGui() + ", leaveTime=" + this.getLeaveTime() + ", keyChances=" + this.getKeyChances() + ")";
    }
}
