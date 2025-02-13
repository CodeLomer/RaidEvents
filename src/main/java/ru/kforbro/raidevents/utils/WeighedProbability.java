package ru.kforbro.raidevents.utils;

import java.util.Map;


public final class WeighedProbability {

    private WeighedProbability() {}

    private static <T> T pick(Map<T, Double> weighedValues) {
        double totalWeight = weighedValues.values().stream().mapToDouble(Double::doubleValue).sum();
        double random = Math.random() * totalWeight;

        for (Map.Entry<T, Double> entry : weighedValues.entrySet()) {
            if ((random -= entry.getValue()) <= 0.0) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Integer pickWeighedProbability(Map<Integer, Double> weighedValues) {
        return pick(weighedValues);
    }

    public static String pickStringDouble(Map<String, Double> weighedValues) {
        return pick(weighedValues);
    }
}


