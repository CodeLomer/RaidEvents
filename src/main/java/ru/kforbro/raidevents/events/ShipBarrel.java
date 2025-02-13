package ru.kforbro.raidevents.events;

import de.exlll.configlib.Ignore;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import ru.kforbro.raidevents.config.Loot;
import ru.kforbro.raidevents.utils.HologramUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Setter
@Getter
public class ShipBarrel {
    private Location location;
    @Ignore
    private int taskId = -1;
    private String hologramName;
    private boolean opened = false;
    private BlockFace blockFace;
    private Ship ship;

    public ShipBarrel(Location location, Ship ship) {
        this.location = location;
        this.ship = ship;
        this.hologramName = "shiptreasure_" + this.serializeLocation(location);
        BlockData blockData = this.location.getBlock().getBlockData();
        if (blockData instanceof Barrel barrel) {
            this.blockFace = barrel.getFacing();
        }
    }

    public void spawn() {
        Block block = this.location.getBlock();
        block.setType(Material.BARREL);
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Barrel barrel) {
            barrel.setFacing(BlockFace.UP);
            block.setBlockData((BlockData)barrel);
        }
    }

    public void despawn() {
        this.location.getBlock().setType(Material.AIR);
        this.removeHologram();
        Bukkit.getScheduler().cancelTask(this.taskId);
        this.taskId = -1;
        this.opened = false;
    }

    public void populate(String keyName) {
        org.bukkit.block.BlockState blockState = this.location.getBlock().getState();
        if (blockState instanceof Container container) {
            int randomValue;
            Loot.LootContent lootContent;
            lootContent = keyName.equalsIgnoreCase("secret") ? ((randomValue = ThreadLocalRandom.current().nextInt(101)) > 85 ? this.ship.getLoot().get("unique") : (randomValue > 60 ? this.ship.getLoot().get("rare") : this.ship.getLoot().get("common"))) : this.ship.getLoot().get(keyName);
            lootContent.populateContainer(container);
            container.update(true, false);
        }
    }

    public void createHologram(int seconds) {
        Hologram hologram = DHAPI.getHologram(this.hologramName);
        List<String> lines = List.of("Ящик пропадет", "через &x&f&e&c&2&2&3" + seconds + " сек.");
        if (hologram == null) {
            Location center = this.location.clone().add(0.5, 1.3, 0.5);
            hologram = HologramUtils.createHologram(this.hologramName, center, false, lines);
            hologram.setDisplayRange(5);
        } else {
            hologram.getPage(0).setLine(0, lines.get(0));
            hologram.getPage(0).setLine(1, lines.get(1));
        }
    }

    public void removeHologram() {
        this.removeDecentHologram();
    }


    private void removeDecentHologram() {
        DHAPI.removeHologram(this.hologramName);
    }


    public String serializeLocation(Location location) {
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();
        return blockX + "_" + blockY + "_" + blockZ;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ShipBarrel other)) {
            return false;
        }
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getTaskId() != other.getTaskId()) {
            return false;
        }
        if (this.isOpened() != other.isOpened()) {
            return false;
        }
        Location this$location = this.getLocation();
        Location other$location = other.getLocation();
        if (!Objects.equals(this$location, other$location)) {
            return false;
        }
        String this$hologramName = this.getHologramName();
        String other$hologramName = other.getHologramName();
        if (!Objects.equals(this$hologramName, other$hologramName)) {
            return false;
        }
        BlockFace this$blockFace = this.getBlockFace();
        BlockFace other$blockFace = other.getBlockFace();
        if (!Objects.equals(this$blockFace, other$blockFace)) {
            return false;
        }
        Ship this$ship = this.getShip();
        Ship other$ship = other.getShip();
        return Objects.equals(this$ship, other$ship);
    }

    protected boolean canEqual(Object other) {
        return other instanceof ShipBarrel;
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + this.getTaskId();
        result = result * 59 + (this.isOpened() ? 79 : 97);
        Location $location = this.getLocation();
        result = result * 59 + ($location == null ? 43 : $location.hashCode());
        String $hologramName = this.getHologramName();
        result = result * 59 + ($hologramName == null ? 43 : $hologramName.hashCode());
        BlockFace $blockFace = this.getBlockFace();
        result = result * 59 + ($blockFace == null ? 43 : $blockFace.hashCode());
        Ship $ship = this.getShip();
        result = result * 59 + ($ship == null ? 43 : $ship.hashCode());
        return result;
    }

    public String toString() {
        return "Ship.ShipBarrel(location=" + this.getLocation() + ", taskId=" + this.getTaskId() + ", hologramName=" + this.getHologramName() + ", opened=" + this.isOpened() + ", blockFace=" + this.getBlockFace() + ", ship=" + this.getShip() + ")";
    }
}
