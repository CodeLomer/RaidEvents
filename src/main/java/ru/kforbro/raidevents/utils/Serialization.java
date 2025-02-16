package ru.kforbro.raidevents.utils;

import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;

import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;
import static java.util.Objects.requireNonNull;
import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Bukkit.getWorld;
import static org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder.decodeLines;
import static org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder.encodeLines;

public final class Serialization {
    private Serialization(){}

    public static byte[] itemStackToByte(final @NonNull ItemStack itemStack) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); final ObjectOutput objectOutputStream = new BukkitObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(itemStack);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize item stack", e);
        }
    }

    public static ItemStack byteToItemStack(final byte @NonNull [] inputItem) {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(inputItem); final BukkitObjectInputStream objectInputStream = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Unable to deserialize item stack", e);
        }
    }

    public static String itemStackToString(final @NonNull ItemStack itemStack) {
        return getEncoder().encodeToString(itemStackToByte(itemStack));
    }

    public static ItemStack stringToItemStack(final @NonNull String inputItem) {
        return byteToItemStack(getDecoder().decode(inputItem));
    }


    public static Inventory stringToInventory(final @NonNull String data) throws IOException {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(decodeLines(data)); final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            final Inventory inventory = getServer().createInventory(null, dataInput.readInt());
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }
            return inventory;
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Unable to deserialize inventory", e);
        }
    }

    public static String inventoryToString(final @NonNull Inventory inventory) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); final ObjectOutput dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(inventory.getSize());
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            dataOutput.flush();
            outputStream.flush();
            return encodeLines(outputStream.toByteArray());
        } catch (final IOException e) {
            throw new RuntimeException("Unable to serialize inventory", e);
        }
    }

    public static String locationToString(final @NonNull Location location) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(requireNonNull(location.getWorld()).getName());
            dataOutput.writeDouble(location.getX());
            dataOutput.writeDouble(location.getY());
            dataOutput.writeDouble(location.getZ());
            dataOutput.writeFloat(location.getYaw());
            dataOutput.writeFloat(location.getPitch());
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize location", e);
        }
        return getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static Location stringToLocation(final @NonNull String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(getDecoder().decode(data));
        try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            String worldName = (String) dataInput.readObject();
            double x = dataInput.readDouble();
            double y = dataInput.readDouble();
            double z = dataInput.readDouble();
            float yaw = dataInput.readFloat();
            float pitch = dataInput.readFloat();
            return new Location(getWorld(worldName), x, y, z, yaw, pitch);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Unable to deserialize location", e);
        }
    }

    public static <T extends Serializable> String serializeToString(T object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize object", e);
        }
    }

    public static <T extends Serializable> T deserializeFromString(String string, Class<T> clazz) {
        byte[] data = getDecoder().decode(string);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object object = ois.readObject();
            if (clazz.isInstance(object)) {
                return clazz.cast(object);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Unable to deserialize object", e);
        }
        return null;
    }
}
