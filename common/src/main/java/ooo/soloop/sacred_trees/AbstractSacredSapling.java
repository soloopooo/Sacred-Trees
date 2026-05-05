package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public abstract class AbstractSacredSapling extends SaplingBlock {
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    protected BlockState log;
    protected BlockState wood;
    protected BlockState leaves;

    public enum Type {
        MEGA, MASSIVE, SACRED_SPRING
    }

    protected Type type;

    public AbstractSacredSapling(Block log, Block wood, Block leaves, Properties properties, Type type) {
        super(null, properties);
        this.log = log.defaultBlockState();
        this.wood = wood.defaultBlockState();
        this.leaves = leaves.defaultBlockState();
        this.type = type;
    }

    public AbstractSacredSapling(BlockState log, BlockState wood, BlockState leaves, Properties properties, Type type) {
        super(null, properties);
        this.log = log;
        this.wood = wood;
        this.leaves = leaves;
        this.type = type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(STAGE, 0);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // No random growth - only via bonemeal
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return (double) world.getRandom().nextFloat() < 0.45D;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        this.advanceTree(world, pos, state, random);
    }

    public void advanceTree(ServerLevel world, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            world.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            if (!PlatformHooks.fireTreeGrowEvent(world, random, pos)) return;
            generateTree(world, pos, state, random);
        }
    }

    protected abstract void generateTree(ServerLevel world, BlockPos pos, BlockState state, RandomSource random);
}
