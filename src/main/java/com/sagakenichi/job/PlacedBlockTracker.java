package com.sagakenichi.job;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * Stores player-placed block coordinates in the chunk PDC. This survives restarts and
 * prevents the common place -> break -> get paid loop without maintaining a global file.
 */
final class PlacedBlockTracker {

    private final NamespacedKey key;
    private boolean enabled;

    PlacedBlockTracker(JavaPlugin plugin, boolean enabled) {
        this.key = new NamespacedKey(plugin, "placed_blocks");
        this.enabled = enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void markPlaced(Block block) {
        if (!enabled) {
            return;
        }
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int encoded = encode(block);
        int[] values = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        if (values == null) {
            pdc.set(key, PersistentDataType.INTEGER_ARRAY, new int[]{encoded});
            return;
        }
        for (int value : values) {
            if (value == encoded) {
                return;
            }
        }
        int[] expanded = Arrays.copyOf(values, values.length + 1);
        expanded[values.length] = encoded;
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, expanded);
    }

    boolean consumeIfPlaced(Block block) {
        if (!enabled) {
            return false;
        }
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int[] values = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        if (values == null || values.length == 0) {
            return false;
        }

        int encoded = encode(block);
        int index = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == encoded) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return false;
        }

        if (values.length == 1) {
            pdc.remove(key);
        } else {
            int[] reduced = new int[values.length - 1];
            System.arraycopy(values, 0, reduced, 0, index);
            System.arraycopy(values, index + 1, reduced, index, values.length - index - 1);
            pdc.set(key, PersistentDataType.INTEGER_ARRAY, reduced);
        }
        return true;
    }

    private static int encode(Block block) {
        int localX = block.getX() & 15;
        int localZ = block.getZ() & 15;
        int yOffset = block.getY() - block.getWorld().getMinHeight();
        return (yOffset << 8) | (localX << 4) | localZ;
    }
}
