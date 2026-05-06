package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FabricSacredFungus extends AbstractSacredFungus {

    public FabricSacredFungus(Block log, Block wood, Block leaves, Block lights, Block vines, Block vines2,
                              Properties properties, Type type) {
        super(log, wood, leaves, lights, vines, vines2, properties, type);
    }

    public FabricSacredFungus(BlockState log, BlockState wood, BlockState leaves, BlockState lights,
                              BlockState vines, BlockState vines2, Properties properties, Type type) {
        super(log, wood, leaves, lights, vines, vines2, properties, type);
    }

    @Override
    protected void generateTree(ServerLevel world, BlockPos pos, BlockState state, RandomSource random) {
        FungusTreeGenerator gen = new FungusTreeGenerator(log, wood, leaves, lights, vines, vines2);
        switch (type) {
            case SACRED_SPRING:
                gen.setTreeScale(6 + random.nextInt(4), 1f, 0.9f)
                    .setLeafAttenuation(0.35f).setMinTrunkSize(4);
                break;
            case MEGA:
                gen.setTreeScale(4 + random.nextInt(3), 0.8f, 0.7f)
                    .setLeafAttenuation(0.6f).setSloped(true).setSafe(false);
                break;
            case MASSIVE:
                gen.setSloped(true).setLeafAttenuation(0.45f).setSloped(false);
                break;
        }
        TreePlacementTask.startPersistentGrowth(gen, world, random, pos,
                getTreeKind(), true, type);
    }

    private String getTreeKind() {
        net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(leaves.getBlock());
        String path = id.getPath();
        if (path.contains("crimson") || path.contains("nether_wart")) return "crimson";
        if (path.contains("warped")) return "warped";
        return "crimson";
    }
}
