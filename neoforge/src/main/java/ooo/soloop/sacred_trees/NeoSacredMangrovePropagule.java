package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NeoSacredMangrovePropagule extends AbstractSacredMangrovePropagule {

    public NeoSacredMangrovePropagule(Block log, Block wood, Block leaves, Properties properties, Type type) {
        super(log, wood, leaves, properties, type);
    }

    public NeoSacredMangrovePropagule(BlockState log, BlockState wood, BlockState leaves, Properties properties, Type type) {
        super(log, wood, leaves, properties, type);
    }

    @Override
    protected void generateTree(ServerLevel world, BlockPos pos, BlockState state, RandomSource random) {
        MangroveTreeGenerator gen = new MangroveTreeGenerator(log, wood, leaves);
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
        TreePlacementTask.startPersistentGrowth(gen, world, random, pos, "mangrove", false, type);
    }
}
