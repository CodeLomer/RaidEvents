package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@Configuration
public class CustomItem {
    private HashMap<String, ItemStack> items = new HashMap<>(Map.of("TESTITEM", new ItemStack(Material.STRING)));

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomItem that)) return false;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        return "CustomItem{items=" + items + "}";
    }
}
