package ru.kforbro.raidevents.converter;

import de.exlll.configlib.Serializer;
import org.bukkit.Color;

public class ColorConverter implements Serializer<Color, String> {
    @Override
    public String serialize(Color color) {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();

    }

    @Override
    public Color deserialize(String s) {
        String[] parts = s.split(",");
        int red = Integer.parseInt(parts[0]);
        int green = Integer.parseInt(parts[1]);
        int blue = Integer.parseInt(parts[2]);
        return Color.fromRGB(red, green, blue);
    }
}
