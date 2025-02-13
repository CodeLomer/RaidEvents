package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import ru.kforbro.raidevents.utils.WeighedProbability;

import java.util.*;

@Setter
@Getter
@Configuration
public class WanderSettings {
    private List<WandererData> wanderers = new ArrayList<>(Collections.singletonList(
            new WandererData("Странник", "airdrop_1", 20, 5, "", "", Color.GREEN, 100.0)
    ));

    public WandererData getRandomWandererData() {
        Map<Integer, Double> weightsMap = new HashMap<>();
        for (int i = 0; i < wanderers.size(); i++) {
            weightsMap.put(i, wanderers.get(i).weight);
        }
        return wanderers.get(WeighedProbability.pickWeighedProbability(weightsMap));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WanderSettings that)) return false;
        return Objects.equals(wanderers, that.wanderers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wanderers);
    }

    @Override
    public String toString() {
        return "WanderSettings{wanderers=" + wanderers + '}';
    }

    public record WandererData(String name, String lootContent, int expPerSecond, int expInterval,
                               String skinTexture, String skinSignature, Color color, double weight) {}
}
