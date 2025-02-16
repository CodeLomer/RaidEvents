package ru.kforbro.raidevents.events;

import lombok.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.config.*;
import ru.kforbro.raidevents.utils.WeighedProbability;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public class EventManager {
    private final RaidEvents plugin;
    private final ConcurrentHashMap<UUID, AirDrop> currentAirDrops = new ConcurrentHashMap<>();
    private Mine currentMine;
    private Wanderer currentWanderer;
    private GoldRush currentGoldRush;
    private Ship currentShip;
    private Event nextEvent;
    private List<Integer> hours = Arrays.asList(0, 2, 4, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
    private List<EventData> events = List.of(
            new EventData("airdrop", 1.0),
            new EventData("wanderer", 1.0),
            new EventData("mine", 1.0),
            new EventData("goldrush", 1.0),
            new EventData("ship", 1.0));
    private long lastEventTime;
    private BukkitTask bukkitTask;


    public EventData getRandomEventData() {
        HashMap<Integer, Double> weightsMap = new HashMap<>();
        for (int i = this.events.size() - 1; i >= 0; --i) {
            weightsMap.put(i, this.events.get(i).weight);
        }
        return this.events.get(WeighedProbability.pickWeighedProbabilityOrGetFirst(weightsMap));
    }

    public Event getEventByData(EventData eventData) {
        return switch (eventData.name.toLowerCase()) {
            case "airdrop" -> startAirDrop(false);
            case "wanderer" -> startWanderer(false);
            case "mine" -> startMine(false);
            case "goldrush" -> startGoldRush(false);
            case "ship" -> startShip(false);
            default -> null;
        };
    }

    public void startCheckNextEvent() {
        if(taskStarted()){
            bukkitTask.cancel();
        }
       bukkitTask = new BukkitRunnable() {
            public void run() {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (nextEvent == null) {
                        nextEvent = getEventByData(getRandomEventData());
                        lastEventTime = System.currentTimeMillis();
                    }
                });
                handleEventAnnouncements();
            }

        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    public boolean taskStarted(){
        return bukkitTask != null && !bukkitTask.isCancelled();
    }

    public void handleEventAnnouncements() {
        if (currentWanderer != null) {
            Bukkit.getOnlinePlayers().forEach(player -> currentWanderer.announce(player));
        }
        if (currentMine != null) {
            Bukkit.getOnlinePlayers().forEach(player -> currentMine.announce(player));
        }
        currentAirDrops.forEach((uuid, airDrop) -> {
            Bukkit.getOnlinePlayers().forEach(airDrop::announce);
        });
    }

    public Long millisToNextEvent() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        Calendar next = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        int nextHour = this.hours.get(0);
        boolean nextDay = true;
        for (int hour : this.hours) {
            if (hour <= now.get(Calendar.HOUR_OF_DAY)) continue;
            nextHour = hour;
            nextDay = false;
            break;
        }
        if (nextDay) {
            next.set(Calendar.DATE, now.get(Calendar.DATE) + 1);
        }
        next.set(Calendar.HOUR_OF_DAY, nextHour);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        return next.getTimeInMillis() - now.getTimeInMillis();
    }

    public AirDrop startAirDrop(boolean start) {
        AirDropSettings.AirDropData airDropData = plugin.getConfigManager().getAirDropSettings().getRandomAirDropData();
        AirDrop airDrop = new AirDrop("&x&A&0&D&B&7&5Аирдроп", airDropData.name(), airDropData.chestCount(), plugin.getConfigManager().getLoot().getContents().get(airDropData.lootContent()), airDropData.material(), airDropData.color(), airDropData.explode(), airDropData.allowPvP());
        if (start) {
            airDrop.start();
        }
        return airDrop;
    }

    public Wanderer startWanderer(boolean start) {
        WanderSettings.WandererData wandererData = plugin.getConfigManager().getWanderSettings().getRandomWandererData();
        Wanderer wanderer = new Wanderer("&x&f&0&c&5&6&cСтранник", wandererData.name(), plugin.getConfigManager().getLoot().getContents().get(wandererData.lootContent()), wandererData.expPerSecond(), wandererData.expInterval(), wandererData.skinTexture(), wandererData.skinSignature(), wandererData.color());
        if (start) {
            wanderer.start();
        }
        return wanderer;
    }

    public Mine startMine(boolean start) {
        MineSettings.MineData mineData = plugin.getConfigManager().getMineSettings().getRandomMineData();
        Mine mine = new Mine(mineData);
        if (start) {
            mine.start();
        }
        return mine;
    }

    public GoldRush startGoldRush(boolean start) {
        GoldRushSettings.GoldRushData goldRushData = plugin.getConfigManager().getGoldRushSettings().getRandomGoldRushData();
        GoldRush goldRush = new GoldRush(goldRushData.name());
        if (start) {
            goldRush.start();
        }
        return goldRush;
    }

    public Ship startShip(boolean start) {
        ShipSettings.ShipData shipData = plugin.getConfigManager().getShipSettings().getRandomShipData();
        HashMap<String, Loot.LootContent> lootContent = new HashMap<>();
        shipData.loot().forEach((s, s2) -> lootContent.put(s, plugin.getConfigManager().getLoot().getContents().get(s2)));
        Ship ship = new Ship("&x&C&9&9&8&4&3Таинственный корабль", shipData.name(), shipData.schematic(), lootContent, plugin.getConfigManager().getLoot().getContents().get(shipData.mythicLootContent()), shipData.keyInterval(), shipData.keyChances());
        if (start) {
            ship.start();
        }
        return ship;
    }

    public AirDrop getAirDrop(Location location) {
        return currentAirDrops.values().stream()
                .filter(airDrop -> airDrop.getChestLocations().contains(location))
                .findFirst()
                .orElse(null);
    }

    public record EventData(String name, double weight) {
    }
}
