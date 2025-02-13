package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Material;
import ru.kforbro.raidevents.utils.WeighedProbability;

@Setter
@Getter
@Configuration
public class AirDropSettings {
    private List<AirDropData> airDrops;

    public AirDropSettings() {
        airDrops = new ArrayList<>();
        airDrops.add(new AirDropData("Мирный аирдроп", 1, "airdrop_1", Color.GREEN, Material.BARREL, false, false, 100.0));
        airDrops.add(new AirDropData("Взрывной аирдроп", 1, "airdrop_3", Color.fromRGB(12079604), Material.RESPAWN_ANCHOR, true, true, 100.0));
    }

    public AirDropData getRandomAirDropData() {
        HashMap<Integer, Double> weightsMap = new HashMap<>();
        for (int i = 0; i < this.airDrops.size(); i++) {
            weightsMap.put(i, this.airDrops.get(i).weight);
        }
        return this.airDrops.get(WeighedProbability.pickWeighedProbability(weightsMap));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AirDropSettings other)) {
            return false;
        }
        if (!other.canEqual(this)) {
            return false;
        }
        List<AirDropData> this$airDrops = this.getAirDrops();
        List<AirDropData> other$airDrops = other.getAirDrops();
        return Objects.equals(this$airDrops, other$airDrops);
    }

    protected boolean canEqual(Object other) {
        return other instanceof AirDropSettings;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<AirDropData> $airDrops = this.getAirDrops();
        result = result * PRIME + ($airDrops == null ? 43 : $airDrops.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "AirDropSettings(airDrops=" + this.getAirDrops() + ")";
    }

    public record AirDropData(String name, int chestCount, String lootContent, Color color, Material material, boolean explode, boolean allowPvP, double weight) {
    }
}
