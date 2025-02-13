package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;
import ru.kforbro.raidevents.utils.WeighedProbability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@Getter
@Setter
@Configuration
public class GoldRushSettings {
    private List<GoldRushData> goldRushes = List.of(new GoldRushData("&eЗолотоая лихорадка", 1.0));

    public GoldRushData getRandomGoldRushData() {
        Map<Integer, Double> weightsMap = generateWeightsMap();
        return goldRushes.get(WeighedProbability.pickWeighedProbability(weightsMap));
    }

    private Map<Integer, Double> generateWeightsMap() {
        return IntStream.range(0, goldRushes.size())
                .collect(HashMap::new,
                        (map, index) -> map.put(index, goldRushes.get(index).weight),
                        HashMap::putAll);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoldRushSettings other)) return false;
        return Objects.equals(goldRushes, other.goldRushes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goldRushes);
    }

    @Override
    public String toString() {
        return "GoldRushSettings(goldRushes=" + goldRushes + ")";
    }

    public record GoldRushData(String name, double weight) {
    }
}
