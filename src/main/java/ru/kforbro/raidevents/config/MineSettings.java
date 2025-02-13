package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import ru.kforbro.raidevents.utils.WeighedProbability;

import java.util.*;

@Getter
@Setter
@Configuration
public class MineSettings {
    private List<MineData> mines = List.of(
            new MineData("Смертельная шахта",
                    List.of(new Block(Material.STONE, 1.0)),
                    List.of(new Loot.LootEntry(1.0, Material.RESPAWN_ANCHOR, null, 1, 3, null, null)),
                    1.0)
    );

    public MineData getRandomMineData() {
        return mines.get(pickRandomMineIndex());
    }

    private int pickRandomMineIndex() {
        Map<Integer, Double> weightsMap = new HashMap<>();
        for (int i = 0; i < mines.size(); i++) {
            weightsMap.put(i, mines.get(i).weight());
        }
        return WeighedProbability.pickWeighedProbability(weightsMap);
    }

    public record MineData(String name, List<Block> blocks, List<Loot.LootEntry> loot, double weight) {
        public Material getRandomMaterial() {
            return blocks.get(pickWeightedIndex(blocks)).material();
        }

        public ItemStack rollLoot() {
            return loot.get(pickWeightedIndex(loot)).getItemStack();
        }

        private <T> int pickWeightedIndex(List<T> list) {
            Map<Integer, Double> weightsMap = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                weightsMap.put(i, list.get(i) instanceof Block b ? b.weight() : ((Loot.LootEntry) list.get(i)).weight());
            }
            return WeighedProbability.pickWeighedProbability(weightsMap);
        }
    }

    public record Block(Material material, double weight) {
    }
}
