package io.bluebeaker.sacredtrees;

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
        switch (type) {
            case SACRED_SPRING:
                TreeTypes.generateSacredSpringRubberTree(
                        new MassiveTreeGenerator(log, wood, leaves, lights, vines, vines2), world, random, pos);
                break;
            case MEGA:
                TreeTypes.generateMegaRubberTree(
                        new MassiveTreeGenerator(log, wood, leaves, lights, vines, vines2), world, random, pos, false);
                break;
            case MASSIVE:
                TreeTypes.generateMassiveRubberTree(
                        new MassiveTreeGenerator(log, wood, leaves, lights, vines, vines2), world, random, pos);
                break;
            default:
                break;
        }
    }
}
