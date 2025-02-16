package ru.kforbro.raidevents.events;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public abstract class Event {
    protected final UUID uuid = UUID.randomUUID();
    protected String name;
    protected String rarity;
    protected abstract void stop();

    protected abstract void start();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;

        return Objects.equals(uuid, event.uuid) &&
                Objects.equals(name, event.name) &&
                Objects.equals(rarity, event.rarity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, rarity);
    }

    @Override
    public String toString() {
        return String.format("Event(uuid=%s, name=%s, rarity=%s)", uuid, name, rarity);
    }
}
