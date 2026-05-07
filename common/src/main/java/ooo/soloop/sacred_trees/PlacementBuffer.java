package ooo.soloop.sacred_trees;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Compact, resizable buffer for block placement data.
 * <p>
 * Stores positions as {@code long[]} and BlockState IDs as {@code int[]},
 * eliminating per-entry object overhead of {@code List&lt;PlacementEntry&gt;}.
 * <p>
 * Memory: ~12 bytes/entry (8B long + 4B int) + array amortization,
 * vs ~24-32 bytes/entry for PlacementEntry record + ArrayList Object[] overhead.
 * <p>
 * For a tree with 83M blocks, this saves ~1-2 GB of heap.
 */
public class PlacementBuffer {
    private static final int DEFAULT_CAPACITY = 16384;

    private long[] positions;
    private int[] stateIds;
    private int size;

    public PlacementBuffer(int initialCapacity) {
        this.positions = new long[initialCapacity];
        this.stateIds = new int[initialCapacity];
    }

    public PlacementBuffer() {
        this(DEFAULT_CAPACITY);
    }

    /** Append a block placement. */
    public void add(long pos, BlockState state) {
        int id = Block.getId(state);
        ensureCapacity(size + 1);
        positions[size] = pos;
        stateIds[size] = id;
        size++;
    }

    /** Bulk-append all entries from another buffer. Fast — uses {@code System.arraycopy}. */
    public void addAll(PlacementBuffer other) {
        int needed = size + other.size;
        ensureCapacity(needed);
        System.arraycopy(other.positions, 0, this.positions, size, other.size);
        System.arraycopy(other.stateIds, 0, this.stateIds, size, other.size);
        size = needed;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > positions.length) {
            int newCap = positions.length + (positions.length >> 1);
            if (newCap < minCapacity) newCap = minCapacity;
            positions = Arrays.copyOf(positions, newCap);
            stateIds = Arrays.copyOf(stateIds, newCap);
        }
    }

    /** Number of entries in this buffer. */
    public int size() {
        return size;
    }

    /** True if the buffer is empty. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Returns the packed position (BlockPos.asLong) at the given index. */
    public long getPos(int index) {
        return positions[index];
    }

    /** Returns the BlockState at the given index (resolved from the stored ID). */
    public BlockState getState(int index) {
        return Block.stateById(stateIds[index]);
    }

    /** Removes all entries. Does not shrink the backing arrays. */
    public void clear() {
        size = 0;
    }

    /**
     * Trims the backing arrays to exactly {@link #size()}.
     * Call after all additions are complete to free any excess capacity.
     */
    public void trimToSize() {
        if (size < positions.length) {
            positions = Arrays.copyOf(positions, size);
            stateIds = Arrays.copyOf(stateIds, size);
        }
    }

    // ---- bulk iteration helpers ----

    /** Returns a read-only snapshot of the position array (first {@link #size()} elements). */
    public long[] positions() {
        return positions;
    }

    /** Returns a read-only snapshot of the state-id array (first {@link #size()} elements). */
    public int[] stateIds() {
        return stateIds;
    }
}
