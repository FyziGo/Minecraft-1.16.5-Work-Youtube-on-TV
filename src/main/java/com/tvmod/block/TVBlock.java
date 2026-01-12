package com.tvmod.block;

import com.tvmod.client.gui.TVScreen;
import com.tvmod.tileentity.TVTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;

public class TVBlock extends HorizontalBlock {

    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_NORTH = VoxelShapes.or(
            Block.box(1, 0, 12, 15, 12, 15),
            Block.box(3, 1, 11, 13, 10, 12)
    );
    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.or(
            Block.box(1, 0, 1, 15, 12, 4),
            Block.box(3, 1, 4, 13, 10, 5)
    );
    private static final VoxelShape SHAPE_EAST = VoxelShapes.or(
            Block.box(1, 0, 1, 4, 12, 15),
            Block.box(4, 1, 3, 5, 10, 13)
    );
    private static final VoxelShape SHAPE_WEST = VoxelShapes.or(
            Block.box(12, 0, 1, 15, 12, 15),
            Block.box(11, 1, 3, 12, 10, 13)
    );

    public TVBlock() {
        super(Properties.of(Material.METAL)
                .strength(3.5f)
                .requiresCorrectToolForDrops()
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        Direction facing = state.getValue(FACING);
        switch (facing) {
            case SOUTH: return SHAPE_SOUTH;
            case EAST: return SHAPE_EAST;
            case WEST: return SHAPE_WEST;
            case NORTH:
            default: return SHAPE_NORTH;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TVTileEntity();
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, 
                                 PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isClientSide) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof TVTileEntity) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft.getInstance().setScreen(new TVScreen((TVTileEntity) tileEntity));
                });
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof TVTileEntity) {
                ((TVTileEntity) tileEntity).stop();
            }

            if (world.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.tvmod.client.VideoPlayerManager.remove(pos);
                });
            }

            super.onRemove(state, world, pos, newState, isMoving);
        }
    }
}
