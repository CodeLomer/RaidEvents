package ru.kforbro.raidevents;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kforbro.raidevents.command.EventCommand;
import ru.kforbro.raidevents.config.ConfigManager;
import ru.kforbro.raidevents.events.AirDrop;
import ru.kforbro.raidevents.events.EventManager;
import ru.kforbro.raidevents.listener.*;

import java.util.Objects;


@Getter
public final class RaidEvents extends JavaPlugin {
    @Getter
    private static RaidEvents instance;
    private ConfigManager configManager;
    private EventManager eventManager;
    private boolean disabling = false;

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.eventManager = new EventManager(this);
        Objects.requireNonNull(this.getCommand("event")).setExecutor(new EventCommand(this));
        this.getServer().getPluginManager().registerEvents(new AirDropListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GoldRushListener(this), this);
        this.getServer().getPluginManager().registerEvents(new WandererListener(this), this);
        this.getServer().getPluginManager().registerEvents(new MineListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ShipListener(this), this);
    }

    @Override
    public void onDisable() {
        this.disabling = true;
        if(eventManager == null) return;
        this.eventManager.getCurrentAirDrops().values().forEach(AirDrop::stop);
        if (this.eventManager.getCurrentWanderer() != null) {
            this.eventManager.getCurrentWanderer().stop();
        }
        if (this.eventManager.getCurrentMine() != null) {
            this.eventManager.getCurrentMine().stop();
        }
        if (this.eventManager.getCurrentGoldRush() != null) {
            this.eventManager.getCurrentGoldRush().stop();
        }
        if (this.eventManager.getCurrentShip() != null) {
            this.eventManager.getCurrentShip().stop();
        }
    }

}

