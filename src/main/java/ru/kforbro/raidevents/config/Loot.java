package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.utils.WeighedProbability;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Setter
@Getter
@Configuration
public class Loot {
    private Map<String, LootContent> contents = Map.of(
            "airdrop_1", new LootContent(4, 10, List.of(
                    new LootEntry(50.0, Material.DIAMOND, null, 8, 32, null, null),
                    new LootEntry(100.0, Material.IRON_INGOT, null, 24, 54, null, null)
            ))
    );

    public static class LootContent implements Serializable {
        @Serial
        private static final long serialVersionUID = 45040504003020L;
        private final int minAmount;
        private final int maxAmount;
        private final List<LootEntry> items;

        public LootContent(int minAmount, int maxAmount, List<LootEntry> items) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.items = items;
        }

        private int getAmount() {
            int amount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
            int playerCount = Bukkit.getOnlinePlayers().size();

            if (playerCount < 30) {
                return Math.min(amount, 15);
            } else if (playerCount < 50) {
                return Math.min(amount, 20);
            } else if (playerCount < 75) {
                return Math.min(amount, 30);
            } else if (playerCount < 100) {
                return Math.min(amount, 40);
            }
            return amount;
        }

        public void populateContainer(Inventory inventory) {
            populateContainer(inventory::setItem, inventory.getSize());
        }

        public void populateContainer(Container container) {
            populateContainer(container.getSnapshotInventory()::setItem, container.getSnapshotInventory().getSize());
        }

        public void populateContainer(HashMap<Integer, ItemStack> inventory, int size) {
            populateContainer(inventory::put, size);
        }

        private void populateContainer(ItemSetter setter, int size) {
            int amount = getAmount();
            for (int i = 0; i < amount; ++i) {
                ItemStack itemStack = rollLoot();
                if (itemStack == null) continue;

                for (int counter = 0; counter < 100; ++counter) {
                    int randomizedIndex = ThreadLocalRandom.current().nextInt(size);
                    if (setter.accepts(randomizedIndex)) continue;
                    setter.accept(randomizedIndex, itemStack);
                    break;
                }
            }
        }

        public ItemStack rollLoot() {
            List<LootEntry> lootEntries = this.items;
            Map<Integer, Double> weightsMap = new HashMap<>(lootEntries.size());
            for (int i = 0; i < lootEntries.size(); i++) {
                weightsMap.put(i, lootEntries.get(i).weight);
            }
            return lootEntries.get(WeighedProbability.pickWeighedProbability(weightsMap)).getItemStack();
        }
    }

        public record LootEntry(double weight, Material material, List<Material> materials, int minAmount, int maxAmount,
                                String customItem, List<String> customItems) implements Serializable {
        @Serial
        private static final long serialVersionUID = 45040504003020L;

        public ItemStack getItemStack() {
                ItemStack itemStack = null;
                if (material != null) {
                    itemStack = new ItemStack(material);
                } else if (materials != null && !materials.isEmpty()) {
                    itemStack = new ItemStack(materials.get(ThreadLocalRandom.current().nextInt(materials.size())));
                } else if (customItem != null) {
                    itemStack = RaidEvents.getInstance().getConfigManager().getCustomItem().getItems().get(customItem);
                } else if (customItems != null && !customItems.isEmpty()) {
                    itemStack = RaidEvents.getInstance().getConfigManager().getCustomItem().getItems().get(customItems.get(ThreadLocalRandom.current().nextInt(customItems.size())));
                }
                if (itemStack != null) {
                    itemStack.setAmount(ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1));
                }
                return itemStack;
            }
        }

    @FunctionalInterface
    interface ItemSetter {
        void accept(int index, ItemStack itemStack);
        default boolean accepts(int index) {
            return false;
        }
    }
}
