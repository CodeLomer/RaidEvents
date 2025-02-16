package ru.kforbro.raidevents.converter;

import de.exlll.configlib.Serializer;
import ru.kforbro.raidevents.config.Loot;
import ru.kforbro.raidevents.utils.Serialization;

public class LootContextConverter implements Serializer<Loot.LootContent, String> {
    @Override
    public String serialize(Loot.LootContent lootContext) {
        return Serialization.serializeToString(lootContext);
    }

    @Override
    public Loot.LootContent deserialize(String s) {
        return Serialization.deserializeFromString(s, Loot.LootContent.class);
    }
}
