package com.sagakenichi.job;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

final class PlacedBlockTracker {
    private final NamespacedKey key;
    private final NamespacedKey fallingKey;
    private boolean enabled;

    PlacedBlockTracker(JavaPlugin plugin, boolean enabled) {
        this.key = new NamespacedKey(plugin, "placed_blocks");
        this.fallingKey = new NamespacedKey(plugin, "placed_falling_block");
        this.enabled = enabled;
    }
    void setEnabled(boolean enabled) { this.enabled = enabled; }

    void markPlaced(Block block) {
        if (!enabled) return;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int encoded = encode(block);
        int[] values = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        if (values == null) {
            pdc.set(key, PersistentDataType.INTEGER_ARRAY, new int[]{encoded});
            return;
        }
        for (int value : values) if (value == encoded) return;
        int[] expanded = Arrays.copyOf(values, values.length + 1);
        expanded[values.length] = encoded;
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, expanded);
    }

    boolean consumeIfPlaced(Block block) {
        if (!enabled) return false;
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        int[] values = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
        if (values == null || values.length == 0) return false;
        int encoded = encode(block);
        int index = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == encoded) { index = i; break; }
        }
        if (index < 0) return false;
        if (values.length == 1) pdc.remove(key);
        else {
            int[] reduced = new int[values.length - 1];
            System.arraycopy(values, 0, reduced, 0, index);
            System.arraycopy(values, index + 1, reduced, index, values.length - index - 1);
            pdc.set(key, PersistentDataType.INTEGER_ARRAY, reduced);
        }
        return true;
    }

    void moveMarkedBlocks(List<Block> blocks, BlockFace direction) {
        if (!enabled || blocks.isEmpty()) return;
        boolean[] marked = new boolean[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) marked[i] = consumeIfPlaced(blocks.get(i));
        for (int i = 0; i < blocks.size(); i++) if (marked[i]) markPlaced(blocks.get(i).getRelative(direction));
    }

    void moveToFallingEntity(Block source, FallingBlock fallingBlock, Material to) {
        if (!enabled || to != Material.AIR) return;
        if (consumeIfPlaced(source)) {
            fallingBlock.getPersistentDataContainer().set(fallingKey, PersistentDataType.BYTE, (byte) 1);
        }
    }
    void restoreFromFallingEntity(FallingBlock fallingBlock, Block destination, Material to) {
        if (!enabled || to == Material.AIR) return;
        Byte placed = fallingBlock.getPersistentDataContainer().get(fallingKey, PersistentDataType.BYTE);
        if (placed != null && placed == (byte) 1) {
            markPlaced(destination);
            fallingBlock.getPersistentDataContainer().remove(fallingKey);
        }
    }

    private static int encode(Block block) {
        int localX = block.getX() & 15;
        int localZ = block.getZ() & 15;
        int yOffset = block.getY() - block.getWorld().getMinHeight();
        return (yOffset << 8) | (localX << 4) | localZ;
    }
}
