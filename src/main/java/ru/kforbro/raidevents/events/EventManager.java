package ru.kforbro.raidevents.events;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.config.AirDropSettings;
import ru.kforbro.raidevents.config.GoldRushSettings;
import ru.kforbro.raidevents.config.Loot;
import ru.kforbro.raidevents.config.MineSettings;
import ru.kforbro.raidevents.config.ShipSettings;
import ru.kforbro.raidevents.config.WanderSettings;
import ru.kforbro.raidevents.events.AirDrop;
import ru.kforbro.raidevents.events.Event;
import ru.kforbro.raidevents.events.GoldRush;
import ru.kforbro.raidevents.events.Mine;
import ru.kforbro.raidevents.events.Ship;
import ru.kforbro.raidevents.events.Wanderer;
import ru.kforbro.raidevents.utils.WeighedProbability;

@Getter
public class EventManager {
    private final RaidEvents plugin;
    private final ConcurrentHashMap<UUID, AirDrop> currentAirDrops = new ConcurrentHashMap<>();
    private Mine currentMine = null;
    private Wanderer currentWanderer = null;
    private GoldRush currentGoldRush = null;
    private Ship currentShip = null;
    public Event nextEvent = null;
    public List<Integer> hours = Arrays.asList(0, 2, 4, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
    public List<EventData> events = new ArrayList<EventData>();
    public long lastEventTime = 0L;

    public EventData getRandomEventData() {
        HashMap<Integer, Double> weightsMap = new HashMap<Integer, Double>();
        for (int i = this.events.size() - 1; i >= 0; --i) {
            weightsMap.put(i, this.events.get(i).weight);
        }
        return this.events.get(WeighedProbability.pickWeighedProbability(weightsMap));
    }

    public Event getEventByData(EventData eventData) {
        Event event = eventData.name.equalsIgnoreCase("airdrop") ? this.startAirDrop(false) : (eventData.name.equalsIgnoreCase("wanderer") ? this.startWanderer(false) : (eventData.name.equalsIgnoreCase("mine") ? this.startMine(false) : (eventData.name.equalsIgnoreCase("goldrush") ? this.startGoldRush(false) : (eventData.name.equalsIgnoreCase("ship") ? this.startShip(false) : null))));
        return event;
    }

    public EventManager(final RaidEvents plugin) {
        this.plugin = plugin;
        plugin.getConfigManager().getSettings().getEvents().forEach((s, integer) -> this.events.add(new EventData((String)s, integer.intValue())));
        this.nextEvent = this.getEventByData(this.getRandomEventData());
        this.lastEventTime = System.currentTimeMillis();
        new BukkitRunnable(){

            public void run() {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
                if (EventManager.this.hours.contains(calendar.get(Calendar.HOUR_OF_DAY)) && calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.SECOND) == 0) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        EventManager.this.nextEvent.start();
                        EventManager.this.nextEvent = EventManager.this.getEventByData(EventManager.this.getRandomEventData());
                        EventManager.this.lastEventTime = System.currentTimeMillis();
                    });
                }
                if (EventManager.this.currentWanderer != null && (EventManager.this.currentWanderer.getSpawnChestAt() - System.currentTimeMillis()) / 1000L % 180L == 0L) {
                    Bukkit.getOnlinePlayers().forEach(player -> EventManager.this.currentWanderer.announce(player));
                }
                if (EventManager.this.currentMine != null && (EventManager.this.currentMine.getStopAt() - System.currentTimeMillis()) / 1000L % 180L == 0L) {
                    Bukkit.getOnlinePlayers().forEach(player -> EventManager.this.currentMine.announce((Player)player));
                }
                EventManager.this.currentAirDrops.forEach((uuid, airDrop) -> {
                    if ((airDrop.getOpenAt() - System.currentTimeMillis()) / 1000L % 180L == 0L) {
                        Bukkit.getOnlinePlayers().forEach(airDrop::announce);
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    public Long millisToNextEvent() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        Calendar next = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        int nextHour = this.hours.get(0);
        boolean nextDay = true;
        for (int hour : this.hours) {
            if (hour <= now.get(11)) continue;
            nextHour = hour;
            nextDay = false;
            break;
        }
        if (nextDay) {
            next.set(5, now.get(5) + 1);
        }
        next.set(11, nextHour);
        next.set(12, 0);
        next.set(13, 0);
        return next.getTimeInMillis() - now.getTimeInMillis();
    }

    public AirDrop startAirDrop(boolean start) {
        AirDropSettings.AirDropData airDropData = this.plugin.getConfigManager().getAirDropSettings().getRandomAirDropData();
        AirDrop airDrop = new AirDrop("&x&A&0&D&B&7&5\u0410\u0438\u0440\u0434\u0440\u043e\u043f", airDropData.name(), airDropData.chestCount(), this.plugin.getConfigManager().getLoot().getContents().get(airDropData.lootContent()), airDropData.material(), airDropData.color(), airDropData.explode(), airDropData.allowPvP());
        if (start) {
            airDrop.start();
        }
        return airDrop;
    }

    public Wanderer startWanderer(boolean start) {
        WanderSettings.WandererData wandererData = this.plugin.getConfigManager().getWanderSettings().getRandomWandererData();
        Wanderer wanderer = new Wanderer("&x&f&0&c&5&6&c\u0421\u0442\u0440\u0430\u043d\u043d\u0438\u043a", wandererData.name(), this.plugin.getConfigManager().getLoot().getContents().get(wandererData.lootContent()), wandererData.expPerSecond(), wandererData.expInterval(), wandererData.skinTexture(), wandererData.skinSignature(), wandererData.color());
        if (start) {
            wanderer.start();
        }
        return wanderer;
    }

    public Mine startMine(boolean start) {
        MineSettings.MineData mineData = this.plugin.getConfigManager().getMineSettings().getRandomMineData();
        Mine mine = new Mine(mineData);
        if (start) {
            mine.start();
        }
        return mine;
    }

    public GoldRush startGoldRush(boolean start) {
        GoldRushSettings.GoldRushData goldRushData = this.plugin.getConfigManager().getGoldRushSettings().getRandomGoldRushData();
        GoldRush goldRush = new GoldRush(goldRushData.name());
        if (start) {
            goldRush.start();
        }
        return goldRush;
    }

    public Ship startShip(boolean start) {
        ShipSettings.ShipData shipData = this.plugin.getConfigManager().getShipSettings().getRandomShipData();
        HashMap<String, Loot.LootContent> lootContent = new HashMap<String, Loot.LootContent>();
        shipData.loot().forEach((s, s2) -> lootContent.put((String)s, this.plugin.getConfigManager().getLoot().getContents().get(s2)));
        Ship ship = new Ship("&x&C&9&9&8&4&3\u0422\u0430\u0438\u043d\u0441\u0442\u0432\u0435\u043d\u043d\u044b\u0439 \u043a\u043e\u0440\u0430\u0431\u043b\u044c", shipData.name(), shipData.schematic(), lootContent, this.plugin.getConfigManager().getLoot().getContents().get(shipData.mythicLootContent()), shipData.keyInterval(), shipData.keyChances());
        if (start) {
            ship.start();
        }
        return ship;
    }

    public AirDrop getAirDrop(Location location) {
        for (AirDrop airDrop : this.currentAirDrops.values()) {
            if (!airDrop.getChestLocations().contains(location)) continue;
            return airDrop;
        }
        return null;
    }

    public void setCurrentMine(Mine currentMine) {
        this.currentMine = currentMine;
    }

    public void setCurrentWanderer(Wanderer currentWanderer) {
        this.currentWanderer = currentWanderer;
    }

    public void setCurrentGoldRush(GoldRush currentGoldRush) {
        this.currentGoldRush = currentGoldRush;
    }

    public void setCurrentShip(Ship currentShip) {
        this.currentShip = currentShip;
    }

    public void setNextEvent(Event nextEvent) {
        this.nextEvent = nextEvent;
    }

    public void setHours(List<Integer> hours) {
        this.hours = hours;
    }

    public void setEvents(List<EventData> events) {
        this.events = events;
    }

    public void setLastEventTime(long lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EventManager other)) {
            return false;
        }
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getLastEventTime() != other.getLastEventTime()) {
            return false;
        }
        RaidEvents this$plugin = this.getPlugin();
        RaidEvents other$plugin = other.getPlugin();
        if (!Objects.equals(this$plugin, other$plugin)) {
            return false;
        }
        ConcurrentHashMap<UUID, AirDrop> this$currentAirDrops = this.getCurrentAirDrops();
        ConcurrentHashMap<UUID, AirDrop> other$currentAirDrops = other.getCurrentAirDrops();
        if (!Objects.equals(this$currentAirDrops, other$currentAirDrops)) {
            return false;
        }
        Mine this$currentMine = this.getCurrentMine();
        Mine other$currentMine = other.getCurrentMine();
        if (this$currentMine == null ? other$currentMine != null : !((Object)this$currentMine).equals(other$currentMine)) {
            return false;
        }
        Wanderer this$currentWanderer = this.getCurrentWanderer();
        Wanderer other$currentWanderer = other.getCurrentWanderer();
        if (!Objects.equals(this$currentWanderer, other$currentWanderer)) {
            return false;
        }
        GoldRush this$currentGoldRush = this.getCurrentGoldRush();
        GoldRush other$currentGoldRush = other.getCurrentGoldRush();
        if (this$currentGoldRush == null ? other$currentGoldRush != null : !((Object)this$currentGoldRush).equals(other$currentGoldRush)) {
            return false;
        }
        Ship this$currentShip = this.getCurrentShip();
        Ship other$currentShip = other.getCurrentShip();
        if (this$currentShip == null ? other$currentShip != null : !((Object)this$currentShip).equals(other$currentShip)) {
            return false;
        }
        Event this$nextEvent = this.getNextEvent();
        Event other$nextEvent = other.getNextEvent();
        if (!Objects.equals(this$nextEvent, other$nextEvent)) {
            return false;
        }
        List<Integer> this$hours = this.getHours();
        List<Integer> other$hours = other.getHours();
        if (!Objects.equals(this$hours, other$hours)) {
            return false;
        }
        List<EventData> this$events = this.getEvents();
        List<EventData> other$events = other.getEvents();
        return Objects.equals(this$events, other$events);
    }

    protected boolean canEqual(Object other) {
        return other instanceof EventManager;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $lastEventTime = this.getLastEventTime();
        result = result * 59 + Long.hashCode($lastEventTime);
        RaidEvents $plugin = this.getPlugin();
        result = result * 59 + ($plugin == null ? 43 : $plugin.hashCode());
        ConcurrentHashMap<UUID, AirDrop> $currentAirDrops = this.getCurrentAirDrops();
        result = result * 59 + ($currentAirDrops == null ? 43 : $currentAirDrops.hashCode());
        Mine $currentMine = this.getCurrentMine();
        result = result * 59 + ($currentMine == null ? 43 : ((Object)$currentMine).hashCode());
        Wanderer $currentWanderer = this.getCurrentWanderer();
        result = result * 59 + ($currentWanderer == null ? 43 : $currentWanderer.hashCode());
        GoldRush $currentGoldRush = this.getCurrentGoldRush();
        result = result * 59 + ($currentGoldRush == null ? 43 : ((Object)$currentGoldRush).hashCode());
        Ship $currentShip = this.getCurrentShip();
        result = result * 59 + ($currentShip == null ? 43 : ((Object)$currentShip).hashCode());
        Event $nextEvent = this.getNextEvent();
        result = result * 59 + ($nextEvent == null ? 43 : $nextEvent.hashCode());
        List<Integer> $hours = this.getHours();
        result = result * 59 + ($hours == null ? 43 : $hours.hashCode());
        List<EventData> $events = this.getEvents();
        result = result * 59 + ($events == null ? 43 : $events.hashCode());
        return result;
    }

    public String toString() {
        return "EventManager(plugin=" + this.getPlugin() + ", currentAirDrops=" + this.getCurrentAirDrops() + ", currentMine=" + this.getCurrentMine() + ", currentWanderer=" + this.getCurrentWanderer() + ", currentGoldRush=" + this.getCurrentGoldRush() + ", currentShip=" + this.getCurrentShip() + ", nextEvent=" + this.getNextEvent() + ", hours=" + this.getHours() + ", events=" + this.getEvents() + ", lastEventTime=" + this.getLastEventTime() + ")";
    }

    public record EventData(String name, double weight) {
    }
}
