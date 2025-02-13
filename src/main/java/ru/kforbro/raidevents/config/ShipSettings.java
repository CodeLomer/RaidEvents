package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;
import ru.kforbro.raidevents.utils.WeighedProbability;

import java.util.*;

@Getter
@Setter
@Configuration
public class ShipSettings {
    private List<ShipData> ships = List.of(
            new ShipData("Корабль", "raidevents_ship_1",
                    Map.of("common", "airdrop_1", "rare", "airdrop_1", "unique", "airdrop_1"),
                    "airdrop_1", 30, 100.0,
                    Map.of("common", 25.0, "rare", 25.0, "unique", 25.0, "secret", 25.0))
    );

    public ShipData getRandomShipData() {
        return ships.get(pickRandomShipIndex());
    }

    private int pickRandomShipIndex() {
        Map<Integer, Double> weightsMap = new HashMap<>();
        for (int i = 0; i < ships.size(); i++) {
            weightsMap.put(i, ships.get(i).weight);
        }
        return WeighedProbability.pickWeighedProbability(weightsMap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShipSettings other)) return false;
        return Objects.equals(ships, other.ships);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ships);
    }

    @Override
    public String toString() {
        return "ShipSettings(ships=" + ships + ")";
    }

    public record ShipData(
            String name,
            String schematic,
            Map<String, String> loot,
            String mythicLootContent,
            int keyInterval,
            double weight,
            Map<String, Double> keyChances
    ) {}
}
