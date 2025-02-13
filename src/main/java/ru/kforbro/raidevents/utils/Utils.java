package ru.kforbro.raidevents.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public final class Utils {
    private Utils(){}

    public static ItemStack getCustomSkull(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(),null);
        ProfileProperty profileProperty = new ProfileProperty("textures",base64);
        profile.getProperties().add(profileProperty);
        skullMeta.setPlayerProfile(profile);

        skull.setItemMeta(skullMeta);
        return skull;
    }

    public static boolean isInvFull(Player player) {
        return Arrays.stream(player.getInventory().getStorageContents()).noneMatch(Objects::isNull);
    }

    public static boolean giveOrDrop(Player player, ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        if (isInvFull(player)) {
            player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            return false;
        }
        player.getInventory().addItem(itemStack);
        return true;
    }
}


