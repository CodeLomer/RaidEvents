package ru.kforbro.raidevents.utils;

public final class Time {
    private Time() {}

    public static String prettyTime(long totalSeconds) {
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = Math.max(totalSeconds % 60L, 0L);

        StringBuilder formattedTime = new StringBuilder();

        if (hours > 0L) {
            formattedTime.append(hours).append(" ч.");
        }
        if (minutes > 0L) {
            if (formattedTime.length() > 0) {
                formattedTime.append(" ");
            }
            formattedTime.append(minutes).append(" мин.");
        }
        if (formattedTime.length() > 0) {
            formattedTime.append(" ");
        }
        formattedTime.append(seconds).append(" сек.");

        return formattedTime.toString();
    }
}

