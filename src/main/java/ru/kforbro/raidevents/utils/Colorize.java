package ru.kforbro.raidevents.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Colorize {
    private Colorize(){}

    public static void sendMessage(String playerName, String message) {
        Player player = getPlayer(playerName);
        if (player != null) {
            player.sendMessage(format(message));
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(format(title), format(subtitle), fadeIn, stay, fadeOut);
    }

    public static void sendTitle(String playerName, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = getPlayer(playerName);
        if (player != null) {
            player.sendTitle(format(title), format(subtitle), fadeIn, stay, fadeOut);
        }
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(format(message));
    }

    public static void sendActionBar(String playerName, String message) {
        Player player = getPlayer(playerName);
        if (player != null) {
            player.sendActionBar(format(message));
        }
    }

    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static Player getPlayer(String playerName) {
        return Bukkit.getPlayerExact(playerName);
    }
}
