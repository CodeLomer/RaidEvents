package ru.kforbro.raidevents.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomLocation {
    private static final List<Material> NORMAL_BLACK_LIST = new ArrayList<>();
    private static final List<Material> NETHER_BLACK_LIST = new ArrayList<>();

    private RandomLocation() {}

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
        Chunk c = getChunkSafely(cf);

        ChunkSnapshot cs = c.getChunkSnapshot();
        int y = getHighestYAt(cs, loc);

        loc.setY(y - 1);
        while (y > 0) {
            Material material = cs.getBlockData(loc.getBlockX() & 0xF, y, loc.getBlockZ() & 0xF).getMaterial();
            if (material.isSolid()) {
                loc.setY(y + 1);
                return new ChunkLocationSnapshot(cs, loc);
            }
            y--;
        }
        return null;
    }

    private static Chunk getChunkSafely(CompletableFuture<Chunk> cf) {
        try {
            return cf.get();
        } catch (InterruptedException | ExecutionException e) {
            MyLogger.logError("Error getting chunk asynchronously: " + e.getMessage());
            throw new RuntimeException("Error getting chunk asynchronously", e);
        }
    }

    private static int getHighestYAt(ChunkSnapshot cs, Location loc) {
        return cs.getHighestBlockYAt(loc.getBlockX() & 0xF, loc.getBlockZ() & 0xF) - 1;
    }

    public static SafeLocation getRandomSafeLocation(World w, Algorithm a, double startRadius, double endRadius, int originX, int originY) {
        WorldBorder worldBorder = w.getWorldBorder();
        double borderSize = worldBorder.getSize() / 2.0;
        if(startRadius > borderSize || startRadius > -borderSize || endRadius > borderSize || endRadius > -borderSize) {
            startRadius = borderSize;
            endRadius = -borderSize;
        }



        Location loc;
        int tries = 0;
        while (tries++ <= 100) {
            ChunkLocationSnapshot cl = getRandomLocation(w, a, startRadius, endRadius, originX, originY);
            if (cl == null) {
                continue;
            }

            loc = cl.location;
            ChunkSnapshot cs = cl.chunkSnapshot;

            if (isInRegion(loc)) {
                continue;
            }

            int chunkX = loc.getBlockX() & 0xF;
            int chunkZ = loc.getBlockZ() & 0xF;
            BlockData b = cs.getBlockData(chunkX, loc.getBlockY(), chunkZ);

            if (w.getEnvironment() == World.Environment.NETHER && !NETHER_BLACK_LIST.contains(b.getMaterial())) {
                MyLogger.log(null, "Checking safe location in Nether.", null);
                return findSafeLocationInNether(cs, chunkX, chunkZ, loc, tries);
            } else if (w.getEnvironment() == World.Environment.NORMAL) {
                if (NORMAL_BLACK_LIST.contains(b.getMaterial())) {
                    MyLogger.log(null, "Found unsafe location in NORMAL environment - " + b.getMaterial(), null);
                    continue;
                }
                MyLogger.log(null, "Found safe location in NORMAL environment.", null);
                MyLogger.log(null,"X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ(), null);
                return new SafeLocation(tries, loc);
            } else if (w.getEnvironment() == World.Environment.THE_END && b.getMaterial().isSolid()) {
                MyLogger.log(null, "Found safe location in THE_END environment.", null);
                MyLogger.log(null,"X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ(), null);
                return new SafeLocation(tries, loc);
            }
        }
        return null;
    }


    private static SafeLocation findSafeLocationInNether(ChunkSnapshot cs, int chunkX, int chunkZ, Location loc, int tries) {
        for (int y = 0; y < 127; y++) {
            if (isSafeNetherLocation(cs, chunkX, chunkZ, y)) {
                loc.setY(y + 1);
                MyLogger.log(null, "Found safe location in Nether at Y: " + (y + 1), null);
                MyLogger.log(null,"X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ(), null);
                return new SafeLocation(tries, loc);
            }
        }
        return null;
    }

    private static boolean isSafeNetherLocation(ChunkSnapshot cs, int chunkX, int chunkZ, int y) {
        return cs.getBlockData(chunkX, y, chunkZ).getMaterial().isSolid() &&
                cs.getBlockData(chunkX, y + 1, chunkZ).getMaterial() == Material.AIR &&
                cs.getBlockData(chunkX, y + 2, chunkZ).getMaterial() == Material.AIR;
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

    private static int[] generateRandomPointOnCircle(double startRadius, double endRadius, int originX, int originY) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double radius = startRadius + random.nextDouble() * (endRadius - startRadius);
        return new int[] {
                originX + (int) (radius * Math.cos(angle)) * (random.nextBoolean() ? 1 : -1),
                originY + (int) (radius * Math.sin(angle)) * (random.nextBoolean() ? 1 : -1)
        };
    }

    private static int[] generateRandomPointOnSquare(double startRadius, double endRadius, int originX, int originY) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double sideLength = endRadius - startRadius;
        return new int[] {
                originX + (random.nextBoolean() ? 1 : -1) * ((int) (random.nextDouble() * sideLength) + (int) startRadius),
                originY + (random.nextBoolean() ? 1 : -1) * ((int) (random.nextDouble() * sideLength) + (int) startRadius)
        };
    }

    public enum Algorithm {
        CIRCLE, SQUARE
    }

    public record ChunkLocationSnapshot(ChunkSnapshot chunkSnapshot, Location location) {}
    public record SafeLocation(int tries, Location location) {}
}
