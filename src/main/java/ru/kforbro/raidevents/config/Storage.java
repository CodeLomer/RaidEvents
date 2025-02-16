package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import de.exlll.configlib.Serializer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import ru.kforbro.raidevents.utils.Serialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Configuration
public class Storage {
    private List<Location> shulkers = new ArrayList<>(List.of(new Location(Bukkit.getWorld("world"), 100, 100, 100)));


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Storage other)) return false;
        return Objects.equals(shulkers, other.shulkers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shulkers);
    }

    @Override
    public String toString() {
        return "Storage(shulkers=" + shulkers + ")";
    }

    static final class LocationStringSerializer implements Serializer<Location, String> {
        @Override
        public String serialize(Location location) {
            return String.format("%s;%d;%d;%d", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        @Override
        public Location deserialize(String s) {
            String[] split = s.split(";");
            World world = Bukkit.getWorld(split[0]);
            return new Location(world, Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
        }
    }

    static final class ItemStackSerializer implements Serializer<ItemStack, String> {
        @Override
        public String serialize(ItemStack itemStack) {
            return Serialization.itemStackToString(itemStack);
        }

        @Override
        public ItemStack deserialize(String s) {
            return Serialization.stringToItemStack(s);
        }
    }
}
