package ru.kforbro.raidevents.utils;

import java.util.Map;


public final class WeighedProbability {

    private WeighedProbability() {}

    private static <T> T pick(Map<T, Double> weighedValues, boolean getFirstIfNotFound) {
        if (weighedValues == null || weighedValues.isEmpty()) {
            throw new IllegalArgumentException("Map of weighed values cannot be null or empty");
        }

        double totalWeight = weighedValues.values().stream().mapToDouble(Double::doubleValue).sum();
        double random = Math.random() * totalWeight;

        for (Map.Entry<T, Double> entry : weighedValues.entrySet()) {
            if ((random -= entry.getValue()) <= 0.0) {
                return entry.getKey();
            }
        }

        // Возвращаем первый элемент как fallback
        if(getFirstIfNotFound) {
            return weighedValues.keySet().iterator().next();
        }
        return null;
    }


    public static Integer pickWeighedProbability(Map<Integer, Double> weighedValues) {
        return pick(weighedValues,false);
    }

    public static String pickStringDouble(Map<String, Double> weighedValues) {
        return pick(weighedValues,false);
    }

    public static Integer pickWeighedProbabilityOrGetFirst(Map<Integer, Double> weighedValues) {
        return pick(weighedValues,true);
    }
}


