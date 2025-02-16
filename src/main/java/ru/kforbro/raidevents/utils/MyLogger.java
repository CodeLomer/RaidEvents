package ru.kforbro.raidevents.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import ru.kforbro.raidevents.events.Event;

public final class MyLogger {
    private final static ConsoleCommandSender CONSOLE_COMMAND_SENDER = Bukkit.getConsoleSender();

    public static void log(Event event, String message, ChatColor chatColor) {
        if(event != null) {
            message = "["+event.getName()+"] "+message;
        } else {
            message = "[Raidevents] "+message;
        }
        if(chatColor != null) {
            message = chatColor + message;
        }
        CONSOLE_COMMAND_SENDER.sendMessage(message);
    }

    public static void logError(Event event, String message) {
        log(event, message, ChatColor.RED);
    }

    public static void logError(String message) {
        log(null, message, ChatColor.RED);
    }

}
