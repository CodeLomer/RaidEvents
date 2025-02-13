package ru.kforbro.raidevents.config;

import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;

@Setter
@Getter
@Configuration
public class Settings {
    private Map<String, Integer> events = Map.of((Object)"airdrop", (Object)25, (Object)"mine", (Object)25, (Object)"wanderer", (Object)25, (Object)"goldrush", (Object)25);

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Settings other)) {
            return false;
        }
        if (!other.canEqual(this)) {
            return false;
        }
        Map<String, Integer> this$events = this.getEvents();
        Map<String, Integer> other$events = other.getEvents();
        return Objects.equals(this$events, other$events);
    }

    protected boolean canEqual(Object other) {
        return other instanceof Settings;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Map<String, Integer> $events = this.getEvents();
        result = result * 59 + ($events == null ? 43 : $events.hashCode());
        return result;
    }

    public String toString() {
        return "Settings(events=" + this.getEvents() + ")";
    }
}
