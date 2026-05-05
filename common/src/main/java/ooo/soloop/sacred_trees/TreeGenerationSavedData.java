package ooo.soloop.sacred_trees;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists tree generation progress across world save/load cycles.
 * When a tree is growing, its parameters are saved here.
 * On world reload, pending trees are resumed.
 */
public class TreeGenerationSavedData extends SavedData {
    public static final Codec<TreeGenerationSavedData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                PendingTree.CODEC.listOf().fieldOf("pending_trees").forGetter(d -> d.pendingTrees)
            )
            .apply(i, TreeGenerationSavedData::new)
    );

    public static final SavedDataType<TreeGenerationSavedData> TYPE = new SavedDataType<TreeGenerationSavedData>(
            Identifier.fromNamespaceAndPath("sacred_trees", "tree_generation"),
            TreeGenerationSavedData::new,
            CODEC,
            null
    );

    private final List<PendingTree> pendingTrees;

    public TreeGenerationSavedData() {
        this.pendingTrees = new ArrayList<>();
    }

    public TreeGenerationSavedData(List<PendingTree> pendingTrees) {
        this.pendingTrees = new ArrayList<>(pendingTrees);
    }

    /** Get or create the saved data for a level. */
    public static TreeGenerationSavedData get(ServerLevel level) {
        SavedDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    /** Add a pending tree to the save data. */
    public void addPending(PendingTree tree) {
        pendingTrees.add(tree);
        setDirty();
    }

    /** Remove a completed/cancelled tree. */
    public void removePending(PendingTree tree) {
        pendingTrees.remove(tree);
        setDirty();
    }

    /** Get all pending trees. */
    public List<PendingTree> getPendingTrees() {
        return pendingTrees;
    }

    /**
     * Represents a single pending tree generation task.
     */
    public static class PendingTree {
        public static final Codec<PendingTree> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(t -> t.pos),
                    Codec.LONG.fieldOf("seed").forGetter(t -> t.seed),
                    Codec.STRING.fieldOf("tree_type").forGetter(t -> t.treeType.name()),
                    Codec.STRING.fieldOf("tree_kind").forGetter(t -> t.treeKind),
                    Codec.BOOL.fieldOf("is_fungus").forGetter(t -> t.isFungus),
                    Codec.INT.fieldOf("progress").forGetter(t -> t.progress)
                )
                .apply(i, (pos, seed, treeTypeName, treeKind, isFungus, progress) ->
                    new PendingTree(pos, seed, AbstractSacredSapling.Type.valueOf(treeTypeName),
                            treeKind, isFungus, progress))
        );

        public final BlockPos pos;
        public final long seed;
        public final AbstractSacredSapling.Type treeType;
        public final String treeKind;
        public final boolean isFungus;
        public int progress;

        public PendingTree(BlockPos pos, long seed, AbstractSacredSapling.Type treeType,
                          String treeKind, boolean isFungus, int progress) {
            this.pos = pos;
            this.seed = seed;
            this.treeType = treeType;
            this.treeKind = treeKind;
            this.isFungus = isFungus;
            this.progress = progress;
        }
    }
}
