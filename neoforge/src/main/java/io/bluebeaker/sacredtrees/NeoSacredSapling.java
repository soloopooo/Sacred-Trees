package io.bluebeaker.sacredtrees;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NeoSacredSapling extends AbstractSacredSapling {

    public NeoSacredSapling(Block log, Block wood, Block leaves, Properties properties, Type type) {
        super(log, wood, leaves, properties, type);
    }

    public NeoSacredSapling(BlockState log, BlockState wood, BlockState leaves, Properties properties, Type type) {
        super(log, wood, leaves, properties, type);
    }

    @Override
    protected void generateTree(ServerLevel world, BlockPos pos, BlockState state, RandomSource random) {
        switch (type) {
            case SACRED_SPRING:
                TreeTypes.generateSacredSpringRubberTree(new MassiveTreeGenerator(log, wood, leaves), world, random, pos);
                break;
            case MEGA:
                TreeTypes.generateMegaRubberTree(new MassiveTreeGenerator(log, wood, leaves), world, random, pos, false);
                break;
            case MASSIVE:
                TreeTypes.generateMassiveRubberTree(new MassiveTreeGenerator(log, wood, leaves), world, random, pos);
                break;
            default:
                break;
        }
    }
}
