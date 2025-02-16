package ru.kforbro.raidevents.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import ru.kforbro.raidevents.RaidEvents;
import ru.kforbro.raidevents.utils.Colorize;
import ru.kforbro.raidevents.utils.Time;

@Getter
@Setter
public class GoldRush extends Event {
    private final String name;
    private long spawnAt;
    private long stopAt;

    public GoldRush(String name) {
        this.name = name;
    }

    @Override
    public void start() {
        this.spawnAt = System.currentTimeMillis();
        this.stopAt = this.spawnAt + 1200000L;
        RaidEvents.getInstance().getEventManager().setCurrentGoldRush(this);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "raidcastle goldrush start");
        RaidEvents.getInstance().getEventManager().setCurrentGoldRush(this);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (stopAt < System.currentTimeMillis()) {
                    stop();
                    cancel();
                }
            }
        }.runTaskTimer(RaidEvents.getInstance(), 0L, 20L);

        announceAllPlayers();
    }

    private void announceAllPlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Colorize.sendMessage(player, "&f");
            Colorize.sendMessage(player, "&9 &n┃r " + this.name + " &fтолько что начался.");
            Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3X: 0, Z: 0");
            Colorize.sendMessage(player, "&f");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
        });
    }

    public void announce(CommandSender player) {
        if (stopAt < System.currentTimeMillis()) return;

        Colorize.sendMessage(player, "&f");
        Colorize.sendMessage(player, "&9 &n┃r " + this.name + " &fактивна уже &x&f&e&c&2&2&3" + Time.prettyTime((System.currentTimeMillis() - this.spawnAt) / 1000L));
        Colorize.sendMessage(player, "&9 ┃&f Координаты: &x&f&e&c&2&2&3X: 0, Z: 0");
        Colorize.sendMessage(player, "&f");
    }

    @Override
    public void stop() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "raidcastle goldrush stop");
        RaidEvents.getInstance().getEventManager().setCurrentGoldRush(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoldRush other)) return false;
        if (!super.equals(o)) return false;
        return spawnAt == other.spawnAt && stopAt == other.stopAt;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 59 * result + Long.hashCode(spawnAt);
        result = 59 * result + Long.hashCode(stopAt);
        return result;
    }

    @Override
    public String toString() {
        return "GoldRush(spawnAt=" + spawnAt + ", stopAt=" + stopAt + ")";
    }
}
