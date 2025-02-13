package ru.kforbro.raidevents.utils;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Getter
@Setter
public class Cuboid {
    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;
    private int zMin;
    private int zMax;
    private double xMinCentered;
    private double xMaxCentered;
    private double yMinCentered;
    private double yMaxCentered;
    private double zMinCentered;
    private double zMaxCentered;
    private World world;

    public Cuboid(Location point1, Location point2) {
        this.xMin = Math.min(point1.getBlockX(), point2.getBlockX());
        this.xMax = Math.max(point1.getBlockX(), point2.getBlockX());
        this.yMin = Math.min(point1.getBlockY(), point2.getBlockY());
        this.yMax = Math.max(point1.getBlockY(), point2.getBlockY());
        this.zMin = Math.min(point1.getBlockZ(), point2.getBlockZ());
        this.zMax = Math.max(point1.getBlockZ(), point2.getBlockZ());
        this.world = point1.getWorld();
        this.xMinCentered = this.xMin + 0.5;
        this.xMaxCentered = this.xMax + 0.5;
        this.yMinCentered = this.yMin + 0.5;
        this.yMaxCentered = this.yMax + 0.5;
        this.zMinCentered = this.zMin + 0.5;
        this.zMaxCentered = this.zMax + 0.5;
    }

    public Iterator<Block> blockList() {
        ArrayList<Block> blocks = new ArrayList<>(this.getTotalBlockSize());
        for (int x = this.xMin; x <= this.xMax; x++) {
            for (int y = this.yMin; y <= this.yMax; y++) {
                for (int z = this.zMin; z <= this.zMax; z++) {
                    Location location = new Location(this.world, x, y, z);
                    Block block = location.getBlock();
                    blocks.add(block);
                }
            }
        }
        return blocks.iterator();
    }

    public Location getCenter() {
        return new Location(this.world,
                (this.xMax - this.xMin) / 2.0 + this.xMin,
                (this.yMax - this.yMin) / 2.0 + this.yMin,
                (this.zMax - this.zMin) / 2.0 + this.zMin);
    }

    public double getDistance() {
        return this.getPoint1().distance(this.getPoint2());
    }

    public double getDistanceSquared() {
        return this.getPoint1().distanceSquared(this.getPoint2());
    }

    public int getHeight() {
        return this.yMax - this.yMin + 1;
    }

    public Location getPoint1() {
        return new Location(this.world, this.xMin, this.yMin, this.zMin);
    }

    public Location getPoint2() {
        return new Location(this.world, this.xMax, this.yMax, this.zMax);
    }

    public Location getRandomLocation() {
        Random rand = new Random();
        int x = rand.nextInt(Math.abs(this.xMax - this.xMin) + 1) + this.xMin;
        int y = rand.nextInt(Math.abs(this.yMax - this.yMin) + 1) + this.yMin;
        int z = rand.nextInt(Math.abs(this.zMax - this.zMin) + 1) + this.zMin;
        return new Location(this.world, x, y, z);
    }

    public int getTotalBlockSize() {
        return this.getHeight() * this.getXWidth() * this.getZWidth();
    }

    public int getXWidth() {
        return this.xMax - this.xMin + 1;
    }

    public int getZWidth() {
        return this.zMax - this.zMin + 1;
    }

    public boolean contains(Location loc) {
        return loc.getWorld() == this.world &&
                loc.getBlockX() >= this.xMin && loc.getBlockX() <= this.xMax &&
                loc.getBlockY() >= this.yMin && loc.getBlockY() <= this.yMax &&
                loc.getBlockZ() >= this.zMin && loc.getBlockZ() <= this.zMax;
    }

    public boolean contains(Player player) {
        return this.contains(player.getLocation());
    }

    public boolean containsWithMarge(Location loc, double marge) {
        return loc.getWorld() == this.world &&
                loc.getX() >= this.xMinCentered - marge && loc.getX() <= this.xMaxCentered + marge &&
                loc.getY() >= this.yMinCentered - marge && loc.getY() <= this.yMaxCentered + marge &&
                loc.getZ() >= this.zMinCentered - marge && loc.getZ() <= this.zMaxCentered + marge;
    }

    public List<Entity> getEntitiesInside() {
        List<Entity> entitiesInside = new ArrayList<>();
        for (Entity entity : this.world.getEntities()) {
            if (this.contains(entity.getLocation())) {
                entitiesInside.add(entity);
            }
        }
        return entitiesInside;
    }

    public void drawCuboidParticles() {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getUniqueId().getMostSignificantBits() != 0L)
                .collect(Collectors.toList());
        int tempMaxX = this.xMax + 1;
        int tempMaxY = this.yMax + 1;
        int tempMaxZ = this.zMax + 1;

        for (double x : getNumbersInRange(this.xMin, tempMaxX, 7)) {
            for (double y : getNumbersInRange(this.yMin, tempMaxY, 7)) {
                for (double z : getNumbersInRange(this.zMin, tempMaxZ, 7)) {
                    if (x == this.xMin || x == tempMaxX || y == this.yMin || y == tempMaxY || z == this.zMin || z == tempMaxZ) {
                        new ParticleBuilder(Particle.REDSTONE)
                                .receivers(players)
                                .location(new Location(this.world, x, y, z))
                                .count(1)
                                .offset(0.0, 0.0, 0.0)
                                .data(new Particle.DustOptions(Color.fromBGR(255, 97, 186), 1.0f))
                                .spawn();
                    }
                }
            }
        }

        for (double x : getNumbersInRange(this.xMin, tempMaxX, 15)) {
            for (double y : getNumbersInRange(this.yMin, tempMaxY, 15)) {
                for (double z : getNumbersInRange(this.zMin, tempMaxZ, 15)) {
                    if ((x == this.xMin || x == tempMaxX) &&
                            (y == this.yMin || y == tempMaxY) &&
                            (z == this.zMin || z == tempMaxZ)) {
                        new ParticleBuilder(Particle.REDSTONE)
                                .receivers(players)
                                .location(new Location(this.world, x, y, z))
                                .count(1)
                                .offset(0.0, 0.0, 0.0)
                                .data(new Particle.DustOptions(Color.fromBGR(255, 97, 186), 1.0f))
                                .spawn();
                    }
                }
            }
        }
    }

    public static double[] getNumbersInRange(double min, double max, int count) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive.");
        }
        double[] numbers = new double[count];
        double step = (max - min) / (count - 1.0);
        numbers[0] = min;
        numbers[count - 1] = max;
        for (int i = 1; i < count - 1; i++) {
            numbers[i] = min + i * step;
        }
        return numbers;
    }
}
