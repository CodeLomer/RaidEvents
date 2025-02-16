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
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.config.MineSettings;
import ru.kforbro.raidevents.utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Getter
public class Mine extends Event {
    @Setter
    private Location location;
    @Setter
    private long spawnAt;
    @Setter
    private long stopAt;
    @Setter
    private EditSession editSession;
    private final HashMap<Location, ItemStack> blocks = new HashMap<>();
    @Setter
    private Cuboid cuboid;
    @Setter
    private Cuboid stanCuboid;
    @Setter
    private MineSettings.MineData mineData;

    public Mine(MineSettings.MineData mineData) {
        this.mineData = mineData;
        this.setName("&x&F&F&4&1&6&0Смертельная шахта");
        this.setRarity(this.mineData.name());
    }

    @Override
    public void start() {
        CompletableFuture.runAsync(() -> {
                World world = Bukkit.getWorld("world");
                if(world == null){
                    throw new RuntimeException("not found default world: world");
                }
                Clipboard clipboard = this.getClipboard();
                if(clipboard == null){
                    return;
                }
                this.spawnAt = System.currentTimeMillis();
                this.stopAt = this.spawnAt + 1500000L;
                RandomLocation.SafeLocation safeLocation = RandomLocation.getRandomSafeLocation(world, RandomLocation.Algorithm.SQUARE, 500.0, world.getWorldBorder().getSize() / 2.0 - 50.0, 0, 0);
                if (safeLocation == null) {
                    MyLogger.logError(this, "Failed to find safe location for mine");
                    return;
                }
                RaidEvents.getInstance().getEventManager().setCurrentMine(this);
                this.location = safeLocation.location().add(0.0, 1.0, 0.0);
                int yNPC = world.getHighestBlockYAt(location);
                Location npcLocation = this.location.clone().add(0.0, yNPC+1, 0.0);
                npcLocation.add(npcLocation);
                this.pasteClipboard(this.location, clipboard);
                this.setBlocks();
                this.createRegion(clipboard);
                this.createRegionMineable();
                new BukkitRunnable(){

                    public void run() {
                        if (Mine.this.stopAt - System.currentTimeMillis() <= 0L) {
                            Bukkit.getScheduler().runTask(RaidEvents.getInstance(), Mine.this::stop);
                            this.cancel();
                        }
                    }
                }.runTaskTimerAsynchronously(RaidEvents.getInstance(), 0L, 20L);
                new BukkitRunnable(){

                    public void run() {
                        if (Mine.this.stopAt - System.currentTimeMillis() <= 0L) {
                            Bukkit.getScheduler().runTask(RaidEvents.getInstance(), Mine.this::stop);
                            this.cancel();
                            return;
                        }
                        Mine.this.stanCuboid.drawCuboidParticles();
                    }
                }.runTaskTimerAsynchronously(RaidEvents.getInstance(), 0L, 7L);
                Bukkit.getOnlinePlayers().forEach(player -> {
                    Colorize.sendMessage(player, "&f");
                    Colorize.sendMessage(player, "&9 &n┃&r " + this.getName() + " &fпо̂явился на карте.");
                    Colorize.sendMessage(player, "&9 ┃&f Местонахождение: &x&f&e&c&2&2&3" + this.location.getBlockX() + ", " + this.location.getBlockY() + ", " + this.location.getBlockZ());
                    Colorize.sendMessage(player, "&f");
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);
                });
                RaidEvents.getInstance().getEventManager().setCurrentMine(this);
        });
    }

    public void announce(CommandSender player) {
        if (this.stopAt < System.currentTimeMillis()) {
            return;
        }
        Colorize.sendMessage(player, "&f");
        Colorize.sendMessage(player, "&9 &n┃&r " + this.name + " &fактивна уже &x&f&e&c&2&2&3" + Time.prettyTime((System.currentTimeMillis() - this.spawnAt) / 1000L));
        Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3" + this.location.getBlockX() + ", " + this.location.getBlockY() + ", " + this.location.getBlockZ());
        Colorize.sendMessage(player, "&f");
    }

    public void createHolograms() {
        this.createDecentHologram();
    }

    public void createDecentHologram() {
    }

    public void setBlocks() {
        Bukkit.getScheduler().runTask(RaidEvents.getInstance(), () -> {
            Iterator<Block> blockIterator = this.cuboid.blockList();
            while (blockIterator.hasNext()) {
                Block block = blockIterator.next();
                block.setType(this.mineData.getRandomMaterial(), false);
                this.blocks.put(block.getLocation(), this.mineData.rollLoot());
            }
        });
    }

    @Override
    public void stop() {
        RaidEvents.getInstance().getEventManager().setCurrentMine(null);
        this.location.getNearbyPlayers(30.0, 30.0, 30.0).forEach(p -> p.setVelocity(new Vector(0, 1, 0).multiply(2)));
        Wanderer.undoSession(this.location, this.editSession);
        this.removeRegion("raidevents_" + this.uuid);
        this.removeRegion("raidevents_mineable_" + this.uuid);
        RaidEvents.getInstance().getEventManager().setCurrentMine(null);
    }

    public void createRegion(Clipboard clipboard) {
        int additionalRadius = 15;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(this.location.getWorld()));
        if(regions == null){
            throw new RuntimeException("filed load regions for event "+this.name);
        }
        Location cornerFirst = this.location.clone().add(Mine.getSchematicOffset(clipboard));
        Location cornerSecond = cornerFirst.clone().add(new Vector(clipboard.getRegion().getWidth() - 1, clipboard.getRegion().getHeight(), clipboard.getRegion().getLength() - 1));
        Location lowestCorner = new Location(cornerFirst.getWorld(), Math.min(cornerFirst.getX(), cornerSecond.getX()), Math.min(cornerFirst.getY(), cornerSecond.getY()), Math.min(cornerFirst.getZ(), cornerSecond.getZ())).subtract(additionalRadius, 0.0, additionalRadius);
        Location highestCorner = new Location(cornerFirst.getWorld(), Math.max(cornerFirst.getX(), cornerSecond.getX()), Math.max(cornerFirst.getY(), cornerSecond.getY()), Math.max(cornerFirst.getZ(), cornerSecond.getZ())).add(additionalRadius, 0.0, additionalRadius);
        BlockVector3 min = BlockVector3.at(lowestCorner.getX(), 0, lowestCorner.getZ());
        BlockVector3 max = BlockVector3.at(highestCorner.getX(), this.location.getWorld().getMaxHeight(), highestCorner.getZ());
        ProtectedCuboidRegion protectedRegion = new ProtectedCuboidRegion("raidevents_" + this.uuid, true, min, max);
        protectedRegion.setFlag(Flags.PVP, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.POTION_SPLASH, StateFlag.State.ALLOW);
        setFlagRegions(protectedRegion);
        protectedRegion.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
        protectedRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.LIGHTER, StateFlag.State.DENY);
        protectedRegion.setFlag(Flags.BLOCKED_CMDS, Set.of("/gsit", "/sit", "/lay", "/crawl", "/bellyflop"));
        regions.addRegion(protectedRegion);
    }

    public void createRegionMineable() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(this.location.getWorld()));
        if(regions == null){
            throw new RuntimeException("filed load regions for event "+this.name);
        }
        BlockVector3 min = BlockVector3.at(this.cuboid.getXMin(), this.cuboid.getYMin(), this.cuboid.getZMin());
        BlockVector3 max = BlockVector3.at(this.cuboid.getXMax(), this.cuboid.getYMax(), this.cuboid.getZMax());
        ProtectedCuboidRegion protectedRegion = new ProtectedCuboidRegion("raidevents_mineable_" + this.uuid, true, min, max);
        protectedRegion.setFlag(Flags.PVP, StateFlag.State.ALLOW);
        setFlagRegions(protectedRegion);
        protectedRegion.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
        protectedRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.LIGHTNING, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
        protectedRegion.setPriority(1);
        regions.addRegion(protectedRegion);
        this.stanCuboid = new Cuboid(new Location(this.cuboid.getWorld(), this.cuboid.getXMin() - 7, this.cuboid.getYMin() - 7, this.cuboid.getZMin() - 7), new Location(this.cuboid.getWorld(), this.cuboid.getXMax() + 7, this.cuboid.getYMax() + 15, this.cuboid.getZMax() + 7));
    }

    static void setFlagRegions(ProtectedCuboidRegion protectedRegion) {
        protectedRegion.setFlag(Flags.MOB_DAMAGE, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.DAMAGE_ANIMALS, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.ALLOW);
        protectedRegion.setFlag(Flags.TNT, StateFlag.State.DENY);
        protectedRegion.setFlag(Flags.OTHER_EXPLOSION, StateFlag.State.DENY);
    }

    public void removeRegion(String name) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(this.location.getWorld()));
        if(regions == null){
            return;
        }
        regions.removeRegion(name);
    }

    public static Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(clipboard.getMinimumPoint().getX() - clipboard.getOrigin().getX(), clipboard.getMinimumPoint().getY() - clipboard.getOrigin().getY(), clipboard.getMinimumPoint().getZ() - clipboard.getOrigin().getZ());
    }

    public Clipboard getClipboard() {
        Clipboard clipboard;
        File file = new File("plugins/FastAsyncWorldEdit/schematics/raidevents_mine.schem");
        if(!file.exists() || !file.getName().endsWith(".schem")){
            MyLogger.logError(this,"не удалось найти файл cхематики для данного ивента -> raidevents_mine_schem");
            return null;
        }
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if(format == null){
            throw new RuntimeException("filed load clipboard for event "+this.name);
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))){
            clipboard = reader.read();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return clipboard;
    }

    public void pasteClipboard(Location location, Clipboard clipboard) {
        Location adjustedLocation = location.clone().add(Mine.getSchematicOffset(clipboard));
        ArrayList<Location> commandBlocks = new ArrayList<>();
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
                    if (material != Material.COMMAND_BLOCK) continue;
                    commandBlocks.add(worldBlock.getLocation());
                }
            }
        }
        this.cuboid = new Cuboid(commandBlocks.get(0), commandBlocks.get(1));
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))){
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(BlockVector3.at(location.getX(), location.getY(), location.getZ())).build();
            Operations.complete(operation);
            this.editSession = editSession;
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Mine other)) {
            return false;
        }
        if (!other.canEqual(this)) {
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
        Location this$location = this.getLocation();
        Location other$location = other.getLocation();
        if (!Objects.equals(this$location, other$location)) {
            return false;
        }
        EditSession this$editSession = this.getEditSession();
        EditSession other$editSession = other.getEditSession();
        if (!Objects.equals(this$editSession, other$editSession)) {
            return false;
        }
        HashMap<Location, ItemStack> this$blocks = this.getBlocks();
        HashMap<Location, ItemStack> other$blocks = other.getBlocks();
        if (!Objects.equals(this$blocks, other$blocks)) {
            return false;
        }
        Cuboid this$cuboid = this.getCuboid();
        Cuboid other$cuboid = other.getCuboid();
        if (!Objects.equals(this$cuboid, other$cuboid)) {
            return false;
        }
        Cuboid this$stanCuboid = this.getStanCuboid();
        Cuboid other$stanCuboid = other.getStanCuboid();
        if (!Objects.equals(this$stanCuboid, other$stanCuboid)) {
            return false;
        }
        MineSettings.MineData this$mineData = this.getMineData();
        MineSettings.MineData other$mineData = other.getMineData();
        return Objects.equals(this$mineData, other$mineData);
    }

    protected boolean canEqual(Object other) {
        return other instanceof Mine;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long $spawnAt = this.getSpawnAt();
        result = result * 59 + Long.hashCode($spawnAt);
        long $stopAt = this.getStopAt();
        result = result * 59 + Long.hashCode($stopAt);
        Location $location = this.getLocation();
        result = result * 59 + ($location == null ? 43 : $location.hashCode());
        EditSession $editSession = this.getEditSession();
        result = result * 59 + ($editSession == null ? 43 : $editSession.hashCode());
        HashMap<Location, ItemStack> $blocks = this.getBlocks();
        result = result * 59 + ($blocks == null ? 43 : $blocks.hashCode());
        Cuboid $cuboid = this.getCuboid();
        result = result * 59 + ($cuboid == null ? 43 : $cuboid.hashCode());
        Cuboid $stanCuboid = this.getStanCuboid();
        result = result * 59 + ($stanCuboid == null ? 43 : $stanCuboid.hashCode());
        MineSettings.MineData $mineData = this.getMineData();
        result = result * 59 + ($mineData == null ? 43 : $mineData.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Mine(location=" + this.getLocation() + ", spawnAt=" + this.getSpawnAt() + ", stopAt=" + this.getStopAt() + ", editSession=" + this.getEditSession() + ", blocks=" + this.getBlocks() + ", cuboid=" + this.getCuboid() + ", stanCuboid=" + this.getStanCuboid() + ", mineData=" + this.getMineData() + ")";
    }
}

