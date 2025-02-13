package ru.kforbro.raidevents.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomLocation {
    private static final List<Material> NORMAL_BLACK_LIST = new ArrayList<>();
    private static final List<Material> NETHER_BLACK_LIST = new ArrayList<>();

    private RandomLocation(){}

    static {
        NETHER_BLACK_LIST.add(Material.WATER);
        NETHER_BLACK_LIST.add(Material.LAVA);
        NORMAL_BLACK_LIST.add(Material.WATER);
        NORMAL_BLACK_LIST.add(Material.LAVA);
        NORMAL_BLACK_LIST.addAll(Tag.LEAVES.getValues());
    }

    public static ChunkLocationSnapshot getRandomLocation(World w, Algorithm a, double startRadius, double endRadius, int originX, int originY) {
        int[] point = getRandomPoint(a, startRadius, endRadius, originX, originY);
        Location loc = new Location(w, point[0], 0.0, point[1]);
        CompletableFuture<Chunk> cf = w.getChunkAtAsync(loc, true);
        Chunk c;
        try {
            c = cf.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("error getting chunk at async ",e);
        }
        ChunkSnapshot cs = c.getChunkSnapshot();
        int y = cs.getHighestBlockYAt(loc.getBlockX() & 0xF, loc.getBlockZ() & 0xF) - 1;
        loc.setY(y + 1);
        return new ChunkLocationSnapshot(cs, loc);
    }

    public static SafeLocation getRandomSafeLocation(World w, Algorithm a, double startRadius, double endRadius, int originX, int originY) {
        Location loc;
        int tries = 0;
        while (tries++ <= 100) {
            ChunkLocationSnapshot cl = getRandomLocation(w, a, startRadius, endRadius, originX, originY);
            loc = cl.location;
            ChunkSnapshot cs = cl.chunkSnapshot;
            if (isInRegion(loc)) continue;

            int chunkX = loc.getBlockX() & 0xF;
            int chunkZ = loc.getBlockZ() & 0xF;
            BlockData b = cs.getBlockData(chunkX, loc.getBlockY(), chunkZ);

            if (w.getEnvironment() == World.Environment.NETHER) {
                if (!NETHER_BLACK_LIST.contains(b.getMaterial())) {
                    return findSafeLocationInNether(cs, chunkX, chunkZ, loc, tries);
                }
            } else if (w.getEnvironment() == World.Environment.NORMAL) {
                if (!NORMAL_BLACK_LIST.contains(b.getMaterial()) && b.getMaterial().isSolid()) {
                    return new SafeLocation(tries, loc);
                }
            } else if (w.getEnvironment() == World.Environment.THE_END && b.getMaterial().equals(Material.END_STONE)) {
                return new SafeLocation(tries, loc);
            }
        }
        return null;
    }

    private static SafeLocation findSafeLocationInNether(ChunkSnapshot cs, int chunkX, int chunkZ, Location loc, int tries) {
        int y = 0;
        while (y < 127) {
            BlockData b = cs.getBlockData(chunkX, y, chunkZ);
            if (b.getMaterial().isSolid() && cs.getBlockData(chunkX, y + 1, chunkZ).getMaterial() == Material.AIR
                    && cs.getBlockData(chunkX, y + 2, chunkZ).getMaterial() == Material.AIR) {
                loc.setY(y + 1);
                return new SafeLocation(tries, loc);
            }
            ++y;
        }
        return null;
    }

    public static boolean isInRegion(Location location) {
        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(loc);
        return set.size() > 0;
    }

    public static int[] getRandomPoint(Algorithm algorithm, double startRadius, double endRadius, int originX, int originY) {
        return switch (algorithm) {
            case CIRCLE -> generateRandomPointOnCircle(startRadius, endRadius, originX, originY);
            case SQUARE -> generateRandomPointOnSquare(startRadius, endRadius, originX, originY);
        };
    }

    public static int[] generateRandomPointOnCircle(double startRadius, double endRadius, int originX, int originY) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double radius = startRadius + random.nextDouble() * (endRadius - startRadius);
        int x = originX + (int) (radius * Math.cos(angle)) * (random.nextBoolean() ? 1 : -1);
        int y = originY + (int) (radius * Math.sin(angle)) * (random.nextBoolean() ? 1 : -1);
        return new int[]{x, y};
    }

    public static int[] generateRandomPointOnSquare(double startRadius, double endRadius, int originX, int originY) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double sideLength = endRadius - startRadius;
        int xDirection = random.nextBoolean() ? 1 : -1;
        int yDirection = random.nextBoolean() ? 1 : -1;
        int x = originX + xDirection * ((int) (random.nextDouble() * sideLength) + (int) startRadius);
        int y = originY + yDirection * ((int) (random.nextDouble() * sideLength) + (int) startRadius);
        return new int[]{x, y};
    }

    public enum Algorithm {
        CIRCLE, SQUARE
    }

    public record ChunkLocationSnapshot(ChunkSnapshot chunkSnapshot, Location location) {
    }

    public record SafeLocation(int tries, Location location) {
    }
}
