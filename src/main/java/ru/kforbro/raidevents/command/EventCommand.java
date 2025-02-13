package ru.kforbro.raidevents.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.events.*;
import ru.kforbro.raidevents.utils.Colorize;
import ru.kforbro.raidevents.utils.Time;

public class EventCommand implements CommandExecutor {
    private final RaidEvents plugin;
    private final EventManager eventManager;

    public EventCommand(RaidEvents plugin) {
        this.plugin = plugin;
        this.eventManager = this.plugin.getEventManager();
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            boolean anyEvent = announceCurrentEvents((Player) sender);
            if (!anyEvent) {
                showNextEvent(sender);
            }
            return true;
        } else if (!sender.hasPermission("raidevents.admin")) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                showHelp(sender);
                break;
            case "airdrop":
                if ("spawn".equalsIgnoreCase(args[1])) startAirDrop();
                break;
            case "wanderer":
                if ("spawn".equalsIgnoreCase(args[1])) startWanderer();
                break;
            case "mine":
                if ("spawn".equalsIgnoreCase(args[1])) startMine();
                break;
            case "goldrush":
                if ("spawn".equalsIgnoreCase(args[1])) startGoldRush();
                break;
            case "ship":
                if ("spawn".equalsIgnoreCase(args[1])) startShip();
                break;
            case "customitem":
                handleCustomItem(args, sender);
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean announceCurrentEvents(Player player) {
        boolean anyEvent = false;

        Ship ship = this.eventManager.getCurrentShip();
        if (ship != null && ship.getStopAt() > System.currentTimeMillis()) {
            ship.announce(player);
            anyEvent = true;
        }

        Wanderer wanderer = this.eventManager.getCurrentWanderer();
        if (wanderer != null && wanderer.getSpawnChestAt() - System.currentTimeMillis() > 0L) {
            wanderer.announce(player);
            anyEvent = true;
        }

        Mine mine = this.eventManager.getCurrentMine();
        if (mine != null) {
            mine.announce(player);
            anyEvent = true;
        }

        GoldRush goldRush = this.eventManager.getCurrentGoldRush();
        if (goldRush != null) {
            goldRush.announce(player);
            anyEvent = true;
        }

        for (AirDrop airdrop : this.eventManager.getCurrentAirDrops().values()) {
            if (airdrop.getOpenAt() - System.currentTimeMillis() <= 0L) continue;
            airdrop.announce(player);
            anyEvent = true;
        }

        return anyEvent;
    }

    private void showNextEvent(CommandSender sender) {
        Event nextEvent = this.eventManager.getNextEvent();
        long secondsToNext = this.eventManager.millisToNextEvent() / 1000L;
        String time = Time.prettyTime(secondsToNext);

        String message = sender.hasPermission("raidevents.show_events") ?
                "&8 | &f " + nextEvent.getName() + " &fпоявится через &x&f&e&c&2&2&3" + time :
                "&8 | &f Следующий ивент появится через &x&f&e&c&2&2&3" + time;

        Colorize.sendMessage(sender, message);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("/raidevents airdrop spawn");
        sender.sendMessage("/raidevents wanderer spawn");
        sender.sendMessage("/raidevents mine spawn");
        sender.sendMessage("/raidevents goldrush spawn");
        sender.sendMessage("/raidevents customitem add");
        sender.sendMessage("/raidevents customitem remove");
    }

    private void startAirDrop() {
        AirDrop airDrop = this.eventManager.startAirDrop(false);
        airDrop.start();
    }

    private void startWanderer() {
        Wanderer wanderer = this.eventManager.startWanderer(false);
        wanderer.start();
    }

    private void startMine() {
        Mine mine = this.eventManager.startMine(false);
        mine.start();
    }

    private void startGoldRush() {
        GoldRush goldRush = this.eventManager.startGoldRush(false);
        goldRush.start();
    }

    private void startShip() {
        Ship ship = this.eventManager.startShip(false);
        ship.start();
    }

    private void handleCustomItem(String[] args, CommandSender sender) {
        if (args.length < 3) return;

        Player player = (Player) sender;
        switch (args[1].toLowerCase()) {
            case "add":
                addCustomItem(args[2], player);
                break;
            case "remove":
                removeCustomItem(args[2]);
                break;
        }
    }

    private void addCustomItem(String itemName, Player player) {
        this.plugin.getConfigManager().getCustomItem().getItems().put(itemName, player.getInventory().getItemInMainHand());
        this.plugin.getConfigManager().saveAll();
        player.sendMessage("Item added.");
    }

    private void removeCustomItem(String itemName) {
        this.plugin.getConfigManager().getCustomItem().getItems().remove(itemName);
        this.plugin.getConfigManager().saveAll();
    }
}
