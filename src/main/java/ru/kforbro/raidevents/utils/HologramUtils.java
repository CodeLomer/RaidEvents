package ru.kforbro.raidevents.utils;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HologramUtils {
    private HologramUtils(){}
    public static Hologram createHologram(String name, Location location, boolean saveToFile, List<String> lines) throws IllegalArgumentException {
        Validate.notNull(name);
        Validate.notNull(location);
        if (!name.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(String.format("Hologram name can only contain alphanumeric characters, underscores and dashes! (%s)", name));
        }
        if (Hologram.getCachedHologramNames().contains(name)) {
            DHAPI.removeHologram(name);
        }
        Hologram hologram = new Hologram(name, location, saveToFile);
        hologram.setDownOrigin(true);
        HologramPage page = hologram.getPage(0);
        if (lines != null) {
            for (String line : lines) {
                Map<String, Object> map = new HashMap<>();
                map.put("content", line);
                map.put("height", 0.25);
                HologramLine hologramLine = HologramLine.fromMap(map, page, page.getNextLineLocation());
                hologramLine.setHeight(0.25);
                page.addLine(hologramLine);
            }
        }
        hologram.showAll();
        hologram.save();
        return hologram;
    }
}
