package ru.kforbro.raidevents.config;

import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.converter.ColorConverter;
import ru.kforbro.raidevents.converter.LootContextConverter;

import java.io.File;

public class ConfigManager {
    private final RaidEvents plugin;
    @Getter private Settings settings;
    @Getter
    private Storage storage;
    @Getter
    private CustomItem customItem;
    @Getter
    private Loot loot;
    @Getter
    private AirDropSettings airDropSettings;
    @Getter
    private WanderSettings wanderSettings;
    @Getter
    private MineSettings mineSettings;
    @Getter
    private GoldRushSettings goldRushSettings;
    @Getter
    private ShipSettings shipSettings;

    public ConfigManager(RaidEvents plugin) {
        this.plugin = plugin;
        loadConfigurations();
    }

    private void loadConfigurations() {
        settings = loadConfig("config.yml", Settings.class);
        storage = loadConfig("storage.yml", Storage.class);
        customItem = loadConfig("customItems.yml", CustomItem.class);
        loot = loadConfig("loot.yml", Loot.class);
        airDropSettings = loadConfig("airdrops.yml", AirDropSettings.class);
        wanderSettings = loadConfig("wanderers.yml", WanderSettings.class);
        mineSettings = loadConfig("mines.yml", MineSettings.class);
        goldRushSettings = loadConfig("goldrush.yml", GoldRushSettings.class);
        shipSettings = loadConfig("ships.yml", ShipSettings.class);
    }

    private <T> T loadConfig(String fileName, Class<T> clazz) {
        File file = new File(plugin.getDataFolder(), fileName);
        return YamlConfigurations.update(file.toPath(), clazz, getConfigProperties());
    }

    public void saveAll() {
        YamlConfigurations.save(new File(plugin.getDataFolder(), "customItems.yml").toPath(),CustomItem.class,customItem,getConfigProperties());
    }


    private YamlConfigurationProperties getConfigProperties() {
        return YamlConfigurationProperties
                .newBuilder()
                .addSerializer(Location.class, new Storage.LocationStringSerializer())
                .addSerializer(ItemStack.class, new Storage.ItemStackSerializer())
                .addSerializer(Loot.LootContent.class, new LootContextConverter())
                .addSerializer(Color.class, new ColorConverter())
                .build();
    }
}

