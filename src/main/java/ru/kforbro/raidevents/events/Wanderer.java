package ru.kforbro.raidevents.events;


import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import lol.pyr.znpcsplus.util.NpcLocation;
import lol.pyr.znpcsplus.util.ParrotVariant;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.config.Loot;
import ru.kforbro.raidevents.gui.builder.item.ItemBuilder;
import ru.kforbro.raidevents.gui.guis.Gui;
import ru.kforbro.raidevents.utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@Getter
public class Wanderer extends Event {
    @Setter
    private Location location;
    @Setter
    private List<Location> npcLocations = new ArrayList<>();
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
    private Loot.LootContent lootContent;
    @Setter
    private EditSession editSession;
    @Setter
    private int expPerSecond;
    @Setter
    private int expInterval;
    private boolean expAvailable = false;
    @Setter
    private ItemStack itemStack;
    @Setter
    private String skinTexture;
    @Setter
    private String skinSignature;
    @Setter
    private Color color;
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

    public Wanderer(String name, String rarity, Loot.LootContent lootContent, int expPerSecond, int expInterval, String skinTexture, String skinSignature, Color color) {
        this.name = name;
        this.rarity = rarity;
        this.lootContent = lootContent;
        this.expPerSecond = expPerSecond;
        this.expInterval = expInterval;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;
        this.color = color;
        this.npcRegistry = NpcApiProvider.get().getNpcRegistry();
        this.npcTypeRegistry = NpcApiProvider.get().getNpcTypeRegistry();
        this.entityPropertyRegistry = NpcApiProvider.get().getPropertyRegistry();
        this.skinDescriptorFactory = NpcApiProvider.get().getSkinDescriptorFactory();
    }

    @Override
    public void start() {
        CompletableFuture.runAsync(this::run);
    }
    public void announce(Player player) {
        if (this.stopAt < System.currentTimeMillis()) {
            return;
        }
        Colorize.sendMessage(player, "&f");
        Colorize.sendMessage(player, "&9 &n│&r " + this.name +
                " &fактивен уже &x&f&e&c&2&2&3" +
                Time.prettyTime((System.currentTimeMillis() - this.spawnAt) / 1000L));
        Colorize.sendMessage(player, "&9 │&f Координаты: &x&f&e&c&2&2&3" +
                this.location.getBlockX() + ", " +
                this.location.getBlockY() + ", " +
                this.location.getBlockZ());
        Colorize.sendMessage(player, "&f");
    }

    public void spawnNpc() {
        this.currentNpcLocation = this.getLocationForNpc();
        List<ParrotVariant> parrotVariants = List.of(
                ParrotVariant.BLUE, ParrotVariant.GRAY, ParrotVariant.GREEN, ParrotVariant.RED_BLUE, ParrotVariant.YELLOW_BLUE
        );

        NpcEntry npcEntry = this.npcRegistry.create(
                "raidevents_" + this.uuid,
                this.currentNpcLocation.getWorld(),
                this.npcTypeRegistry.getByName("PLAYER"),
                new NpcLocation(this.currentNpcLocation)
        );

        this.npc = npcEntry.getNpc();
        this.npc.setProperty(this.entityPropertyRegistry.getByName("skin", SkinDescriptor.class),
                this.skinDescriptorFactory.createStaticDescriptor(this.skinTexture, this.skinSignature));
        Ship.setShipProperties(parrotVariants, this.npc, this.entityPropertyRegistry);

        this.npc.getHologram().addLine(this.name);
        this.npc.getHologram().addLine("§fНажмите, чтобы взять");
        npcEntry.setProcessed(true);
    }


    public void moveNpc() {
        this.currentNpcLocation = this.getLocationForNpc();
        this.npc.setLocation(new NpcLocation(this.currentNpcLocation));
    }

    public void setExpAvailable(boolean state) {
        if (state) {
            this.npc.setProperty(this.entityPropertyRegistry.getByName("hand", ItemStack.class), this.itemStack);
            this.expAvailable = true;
        } else {
            this.expAvailable = false;
            this.npc.setProperty(this.entityPropertyRegistry.getByName("hand", ItemStack.class), new ItemStack(Material.AIR));
        }
    }

    public void spawnChest() {
        Barrel barrel;
        this.chestSpawned = true;
        this.currentNpcLocation = this.getLocationForNpc();
        this.chestLocation = this.currentNpcLocation.toBlockLocation();
        Block block = this.chestLocation.getBlock();
        block.setType(Material.BARREL, false);
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Barrel) {
            barrel = (Barrel) blockData;
            barrel.getBlock().setBlockData(Bukkit.createBlockData("minecraft:barrel[facing=up]"));
            barrel.update();
        }
        this.lootContent.populateContainer(this.chestContent, 54);
        this.gui = (((Gui.gui()
                .title(Component.text(Colorize.format("&8" + ChatColor.stripColor(Colorize.format(this.name))))))
                .rows(6))
                .disableAllInteractions())
                .create();
        this.gui.setOpenGuiAction(event -> this.playersInGui.add((Player) event.getPlayer()));
        this.gui.setCloseGuiAction(event -> this.playersInGui.remove((Player) event.getPlayer()));

        blockData = (BlockData) block.getState();
        if (blockData instanceof org.bukkit.block.Barrel) {
            barrel = (org.bukkit.block.Barrel) blockData;
            barrel.open();
        }

        final List<Material> randomMaterials = List.of(
                Material.CLAY_BALL, Material.NAUTILUS_SHELL, Material.BONE_MEAL,
                Material.GRAY_DYE, Material.FIREWORK_STAR, Material.GUNPOWDER,
                Material.PHANTOM_MEMBRANE, Material.QUARTZ);
        final Cache<UUID, Boolean> clickItemCooldown = CacheBuilder.newBuilder()
                .expireAfterWrite(150L, TimeUnit.MILLISECONDS)
                .build();

        new BukkitRunnable() {
            public void run() {
                if (Wanderer.this.chestContent.isEmpty()) {
                    this.cancel();
                    return;
                }
                ArrayList<Integer> keysAsArray = new ArrayList<>(Wanderer.this.chestContent.keySet());
                int randomKey = keysAsArray.get(ThreadLocalRandom.current().nextInt(keysAsArray.size()));
                ItemStack itemStack = Wanderer.this.chestContent.getOrDefault(randomKey, null);

                if (itemStack != null) {
                    ArrayList<Integer> emptySlots = new ArrayList<>();
                    for (int i = 0; i < Wanderer.this.gui.getRows() * 9; ++i) {
                        if (Wanderer.this.gui.getGuiItems().containsKey(i)) continue;
                        emptySlots.add(i);
                    }

                    if (!emptySlots.isEmpty()) {
                        int randomSlot = emptySlots.get(ThreadLocalRandom.current().nextInt(emptySlots.size()));

                        Wanderer.this.gui.setItem(randomSlot, ItemBuilder.from(
                                        randomMaterials.get(ThreadLocalRandom.current().nextInt(randomMaterials.size())))
                                .setName(Colorize.format("&x&f&8&9&d&5&7Секретный предмет"))
                                .asGuiItem(event -> {
                                    Player player = (Player) event.getWhoClicked();
                                    if (clickItemCooldown.getIfPresent(player.getUniqueId()) != null) {
                                        return;
                                    }
                                    clickItemCooldown.put(player.getUniqueId(), true);
                                    randomMaterials.forEach(m -> player.setCooldown(m, 3));
                                    Wanderer.this.gui.removeItem(randomSlot);
                                    Utils.giveOrDrop(player, itemStack);
                                }));
                    }

                    Wanderer.this.chestContent.remove(randomKey);
                }
            }
        }.runTaskTimer(RaidEvents.getInstance(), 0L, 2L);
    }


    public Location getLocationForNpc() {
        return this.npcLocations.get(ThreadLocalRandom.current().nextInt(this.npcLocations.size()));
    }

    @Override
    public void stop() {
        stopEvent(this.location, this.npcRegistry, "raidevents_" + this.uuid);
        undoSession(this.location, this.editSession);
        this.removeRegion();
        Bukkit.getOnlinePlayers().forEach(player -> Wanderer.removeItems(player.getInventory()));
        RaidEvents.getInstance().getEventManager().setCurrentShip(null);
    }

    static void undoSession(Location location, EditSession editSession2) {
        if (RaidEvents.getInstance().isDisabling()) {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))){
                editSession2.undo(editSession);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(RaidEvents.getInstance(), () -> {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))){
                    editSession2.undo(editSession);
                }
            }, 20L);
        }
    }

    static void stopEvent(Location location, NpcRegistry npcRegistry, String s) {
        RaidEvents.getInstance().getEventManager().setCurrentWanderer(null);
        location.getNearbyPlayers(30.0, 30.0, 30.0).forEach(p -> p.setVelocity(new Vector(0, 1, 0).multiply(2)));
        if (npcRegistry.getById(s) != null) {
            npcRegistry.delete(s);
        }
    }

    public static void removeItems(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || !itemStack.getType().equals(Material.PLAYER_HEAD)) {
                continue;
            }
            PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
            if (data.has(new NamespacedKey(RaidEvents.getInstance(),"wandereritem"),PersistentDataType.STRING)) {
                inventory.setItem(i, null);
            }
        }
    }


    public void createRegion(Clipboard clipboard) {
        Location location = this.npcLocations.get(0);
        int additionalRadius = 15;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        if(regions == null){
            throw new RuntimeException("filed load regions for event "+this.name);
        }

        Location cornerFirst = location.clone().add(Wanderer.getSchematicOffset(clipboard));
        Location cornerSecond = cornerFirst.clone().add(new Vector(clipboard.getRegion().getWidth() - 1, clipboard.getRegion().getHeight(), clipboard.getRegion().getLength() - 1));

        Location lowestCorner = new Location(cornerFirst.getWorld(),
                Math.min(cornerFirst.getX(), cornerSecond.getX()) - additionalRadius,
                Math.min(cornerFirst.getY(), cornerSecond.getY()),
                Math.min(cornerFirst.getZ(), cornerSecond.getZ()) - additionalRadius);

        Location highestCorner = new Location(cornerFirst.getWorld(),
                Math.max(cornerFirst.getX(), cornerSecond.getX()) + additionalRadius,
                Math.max(cornerFirst.getY(), cornerSecond.getY()),
                Math.max(cornerFirst.getZ(), cornerSecond.getZ()) + additionalRadius);

        BlockVector3 min = BlockVector3.at(lowestCorner.getX(), lowestCorner.getY(), lowestCorner.getZ());
        BlockVector3 max = BlockVector3.at(highestCorner.getX(), highestCorner.getY(), highestCorner.getZ());

        ProtectedCuboidRegion protectedRegion = new ProtectedCuboidRegion("raidevents_" + this.uuid, true, min, max);
        setRegionFlags(protectedRegion);
        regions.addRegion(protectedRegion);
    }

    private void setRegionFlags(ProtectedCuboidRegion region) {
        region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
        AirDrop.setFlagsTORegion(region);
    }


    public void removeRegion() {
        Location location = this.npcLocations.get(0);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        if(regions == null){
            throw new RuntimeException("filed load regions for event "+this.name);
        }
        regions.removeRegion("raidevents_" + this.uuid);
    }

    public static Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(clipboard.getMinimumPoint().getX() - clipboard.getOrigin().getX(), clipboard.getMinimumPoint().getY() - clipboard.getOrigin().getY(), clipboard.getMinimumPoint().getZ() - clipboard.getOrigin().getZ());
    }

    public Clipboard getClipboard() {
        File file = new File("plugins/worldEdit/schematics/raidevents_wanderer.schem");
        if(!file.exists() || !file.getName().endsWith(".schem")){
            MyLogger.logError(this,"не удалось найти файл cхематики для данного ивента -> raidevents_wanderer_schem");
        }
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if(format == null){
            throw new RuntimeException("filed load clipboard for event "+this.name);
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))){
            return reader.read();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pasteClipboard(Location location, Clipboard clipboard) {
        Location adjustedLocation = location.clone().add(Wanderer.getSchematicOffset(clipboard));
        HashSet<Location> anchorBlocks = new HashSet<>();
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
                            throw new RuntimeException("Failed to paste schematic",e);
                        }
                    }
                    if (material != Material.RESPAWN_ANCHOR) continue;
                    anchorBlocks.add(worldBlock.getLocation());
                }
            }
        }
        anchorBlocks.forEach(anchorLocation -> this.npcLocations.add(anchorLocation.clone().add(0.5, 1.0, 0.5)));
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))){
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(BlockVector3.at(location.getX(), location.getY(), location.getZ())).build();
            Operations.complete(operation);
            this.editSession = editSession;
        } catch (WorldEditException e) {
            throw new RuntimeException("Failed to paste schematic",e);
        }
    }

    public ItemStack generateItemStack() {
        ItemStack itemStack = Utils.getCustomSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzk5YWQ3YTA0MzE2OTI5OTRiNmM0MTJjN2VhZmI5ZTBmYzQ5OTc1MjQwYjczYTI3ZDI0ZWQ3OTcwMzVmYjg5NCJ9fX0=");
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(Colorize.format("&x&7&e&f&c&2&0Экспириум"));
        itemMeta.setLore(Arrays.asList(
                Colorize.format(" &f"),
                Colorize.format(" &fОбладая этим предметом, Вы &x&7&e&f&c&2&0каждую секунду"),
                Colorize.format(" &x&7&e&f&c&2&0получаете опыт &fпри нахождении в 12 блоках"),
                Colorize.format(" &fот &x&7&e&f&c&2&0странника&f, но если Вы окажетесь слишком"),
                Colorize.format(" &fдалеко от него, предмет исчезнет."),
                Colorize.format(""),
                Colorize.format(" &fЧем &x&7&e&f&c&2&0больше экспириума&f, тем &x&7&e&f&c&2&0больше опыта &fполучаете.")
        ));
        NamespacedKey namespacedKey = new NamespacedKey(RaidEvents.getInstance(), "wandereritem");
        itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, this.uuid.toString());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Wanderer other)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return this.getSpawnAt() == other.getSpawnAt() &&
                this.getStopAt() == other.getStopAt() &&
                this.getSpawnChestAt() == other.getSpawnChestAt() &&
                this.isChestSpawned() == other.isChestSpawned() &&
                this.getExpPerSecond() == other.getExpPerSecond() &&
                this.getExpInterval() == other.getExpInterval() &&
                this.isExpAvailable() == other.isExpAvailable() &&
                Objects.equals(this.getLocation(), other.getLocation()) &&
                Objects.equals(this.getNpcLocations(), other.getNpcLocations()) &&
                Objects.equals(this.getCurrentNpcLocation(), other.getCurrentNpcLocation()) &&
                Objects.equals(this.getChestLocation(), other.getChestLocation()) &&
                Objects.equals(this.getChestContent(), other.getChestContent()) &&
                Objects.equals(this.getGui(), other.getGui()) &&
                Objects.equals(this.getLootContent(), other.getLootContent()) &&
                Objects.equals(this.getEditSession(), other.getEditSession()) &&
                Objects.equals(this.getItemStack(), other.getItemStack()) &&
                Objects.equals(this.getSkinTexture(), other.getSkinTexture()) &&
                Objects.equals(this.getSkinSignature(), other.getSkinSignature()) &&
                Objects.equals(this.getColor(), other.getColor()) &&
                Objects.equals(this.getNpc(), other.getNpc()) &&
                Objects.equals(this.getNpcRegistry(), other.getNpcRegistry()) &&
                Objects.equals(this.getNpcTypeRegistry(), other.getNpcTypeRegistry()) &&
                Objects.equals(this.getEntityPropertyRegistry(), other.getEntityPropertyRegistry()) &&
                Objects.equals(this.getSkinDescriptorFactory(), other.getSkinDescriptorFactory()) &&
                Objects.equals(this.getPlayersInGui(), other.getPlayersInGui()) &&
                Objects.equals(this.getLeaveTime(), other.getLeaveTime());
    }



    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = result * 59 + Long.hashCode(this.getSpawnAt());
        result = result * 59 + Long.hashCode(this.getStopAt());
        result = result * 59 + hashCode(this.getSpawnChestAt());
        result = result * 59 + (this.isChestSpawned() ? 79 : 97);
        result = result * 59 + this.getExpPerSecond();
        result = result * 59 + this.getExpInterval();
        result = result * 59 + (this.isExpAvailable() ? 79 : 97);
        result = result * 59 + hashCode(this.getLocation());
        result = result * 59 + hashCode(this.getNpcLocations());
        result = result * 59 + hashCode(this.getCurrentNpcLocation());
        result = result * 59 + hashCode(this.getChestLocation());
        result = result * 59 + hashCode(this.getChestContent());
        result = result * 59 + hashCode(this.getGui());
        result = result * 59 + hashCode(this.getLootContent());
        result = result * 59 + hashCode(this.getEditSession());
        result = result * 59 + hashCode(this.getItemStack());
        result = result * 59 + hashCode(this.getSkinTexture());
        result = result * 59 + hashCode(this.getSkinSignature());
        result = result * 59 + hashCode(this.getColor());
        result = result * 59 + hashCode(this.getNpc());
        result = result * 59 + hashCode(this.getNpcRegistry());
        result = result * 59 + hashCode(this.getNpcTypeRegistry());
        result = result * 59 + hashCode(this.getEntityPropertyRegistry());
        result = result * 59 + hashCode(this.getSkinDescriptorFactory());
        result = result * 59 + hashCode(this.getPlayersInGui());
        result = result * 59 + hashCode(this.getLeaveTime());
        return result;
    }

    private int hashCode(Object obj) {
        return obj == null ? 43 : obj.hashCode();
    }

    @Override
    public String toString() {
        return "Wanderer(location=" + this.getLocation() + ", npcLocations=" + this.getNpcLocations() + ", currentNpcLocation=" + this.getCurrentNpcLocation() + ", chestLocation=" + this.getChestLocation() + ", chestContent=" + this.getChestContent() + ", spawnAt=" + this.getSpawnAt() + ", stopAt=" + this.getStopAt() + ", spawnChestAt=" + this.getSpawnChestAt() + ", chestSpawned=" + this.isChestSpawned() + ", gui=" + this.getGui() + ", lootContent=" + this.getLootContent() + ", editSession=" + this.getEditSession() + ", expPerSecond=" + this.getExpPerSecond() + ", expInterval=" + this.getExpInterval() + ", expAvailable=" + this.isExpAvailable() + ", itemStack=" + this.getItemStack() + ", skinTexture=" + this.getSkinTexture() + ", skinSignature=" + this.getSkinSignature() + ", color=" + this.getColor() + ", npc=" + this.getNpc() + ", npcRegistry=" + this.getNpcRegistry() + ", npcTypeRegistry=" + this.getNpcTypeRegistry() + ", entityPropertyRegistry=" + this.getEntityPropertyRegistry() + ", skinDescriptorFactory=" + this.getSkinDescriptorFactory() + ", playersInGui=" + this.getPlayersInGui() + ", leaveTime=" + this.getLeaveTime() + ")";
    }

    private void run() {
        final World world = Bukkit.getWorld("world");
        if (world == null) {
            throw new RuntimeException("not found default world: world");
        }
        this.spawnAt = System.currentTimeMillis();
        this.spawnChestAt = this.spawnAt + ThreadLocalRandom.current().nextInt(15, 26) * 60L * 1000L;
        this.stopAt = this.spawnChestAt + 120000L;
        RandomLocation.SafeLocation safeLocation =
                RandomLocation.getRandomSafeLocation(world, RandomLocation.Algorithm.SQUARE, 500.0,
                        world.getWorldBorder().getSize() / 2.0 - 50.0, 0, 0);
        if (safeLocation == null) {
            MyLogger.logError(this, "Failed to find safe location for wanderer");
            return;
        }
        RaidEvents.getInstance().getEventManager().setCurrentWanderer(this);
        this.location = safeLocation.location().add(0.0, 1.0, 0.0);
        Clipboard clipboard = this.getClipboard();
        if(clipboard == null){
            return;
        }
        this.pasteClipboard(this.location, clipboard);
        this.createRegion(clipboard);
        this.itemStack = this.generateItemStack();
        this.spawnNpc();
        new BukkitRunnable() {

            public void run() {
                if (!Wanderer.this.chestSpawned && Wanderer.this.spawnChestAt - System.currentTimeMillis() <= 0L) {
                    if (Wanderer.this.npcRegistry.getById("raidevents_" + Wanderer.this.uuid) != null) {
                        Wanderer.this.npcRegistry.delete("raidevents_" + Wanderer.this.uuid);
                    }
                    Bukkit.getScheduler().runTask(RaidEvents.getInstance(), Wanderer.this::spawnChest);
                } else if (Wanderer.this.stopAt - System.currentTimeMillis() <= 0L) {
                    Bukkit.getScheduler().runTask(RaidEvents.getInstance(), Wanderer.this::stop);
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(RaidEvents.getInstance(), 0L, 20L);
        new BukkitRunnable() {

            public void run() {
                if (Wanderer.this.spawnChestAt - System.currentTimeMillis() <= 0L) {
                    this.cancel();
                    return;
                }
                Wanderer.this.moveNpc();
                Wanderer.this.setExpAvailable(true);
            }
        }.runTaskTimer(RaidEvents.getInstance(), 0L, (long) this.expInterval * 20L);
        new BukkitRunnable() {

            public void run() {
                double radius = 1.0;
                for (double angle = 0.0; angle <= Math.PI * 2; angle += 0.20943951023931953) {
                    double x = Wanderer.this.currentNpcLocation.clone().getX() + radius * Math.cos(angle);
                    double z = Wanderer.this.currentNpcLocation.clone().getZ() + radius * Math.sin(angle);
                    new ParticleBuilder(Particle.REDSTONE).location(new Location(world, x, Wanderer.this.currentNpcLocation.getY(), z)).count(1).offset(0.0, 0.0, 0.0).data((Object) new Particle.DustOptions(Wanderer.this.color, 1.0f)).spawn();
                }
                if (Wanderer.this.spawnChestAt - System.currentTimeMillis() <= 0L) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(RaidEvents.getInstance(), 0L, 3L);
        new BukkitRunnable() {

            public void run() {
                if (Wanderer.this.stopAt - System.currentTimeMillis() <= 0L) {
                    this.cancel();
                    return;
                }
                Bukkit.getOnlinePlayers().forEach(player -> {
                    int amount = 0;
                    for (ItemStack item : player.getInventory()) {
                        String itemUuid;
                        if (item == null || !item.isSimilar(item) || (itemUuid = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(RaidEvents.getInstance(), "wandereritem"), PersistentDataType.STRING)) == null || !UUID.fromString(itemUuid).equals(Wanderer.this.uuid))
                            continue;
                        amount += item.getAmount();
                    }
                    if (amount > 0) {
                        if (player.getAllowFlight()) {
                            player.setFlying(false);
                            player.setAllowFlight(false);
                        }
                        if (Wanderer.this.currentNpcLocation.getWorld().equals(player.getWorld()) && Wanderer.this.currentNpcLocation.distance(player.getLocation()) <= 12.0) {
                            int exp = Wanderer.this.expPerSecond * amount;
                            player.giveExp(exp, false);
                            Colorize.sendActionBar(player, "&fВы получили &x&7&e&f&c&2&0" + exp + " опыта&f.");
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 50, 1));
                            Wanderer.this.leaveTime.remove(player.getUniqueId());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bp addprogress " + player.getName() + " WANDERER_GET_EXP " + exp);
                        }
                    } else {
                        Wanderer.this.leaveTime.put(player.getUniqueId(), Wanderer.this.leaveTime.getOrDefault(player.getUniqueId(), 0) + 1);
                        Colorize.sendActionBar(player, "&cВернитесь к страннику в течение &x&f&e&c&2&2&3" + (13 - Wanderer.this.leaveTime.get(player.getUniqueId())) + " сек.");
                        if (Wanderer.this.leaveTime.get(player.getUniqueId()) > 12) {
                            Wanderer.removeItems(player.getInventory());
                            Wanderer.this.leaveTime.remove(player.getUniqueId());
                        }
                    }
                });
            }
        }.runTaskTimer(RaidEvents.getInstance(), 0L, 20L);
        Bukkit.getOnlinePlayers().forEach(player -> {
            Colorize.sendMessage(player, "&f");
            Colorize.sendMessage(player, "&9 &n┃&r " + this.name + " &fначал раздавать экспириум.");
            Colorize.sendMessage(player, "&9 &n┃&f Редкость: &x&f&e&c&2&2&3" + this.rarity);
            Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3" + this.location.getBlockX() + ", " + this.location.getBlockY() + ", " + this.location.getBlockZ());
            Colorize.sendMessage(player, "&f");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
        });
        RaidEvents.getInstance().getEventManager().setCurrentWanderer(this);

    }
}

