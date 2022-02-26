package dev.momostudios.coldsweat.common.block;

import dev.momostudios.coldsweat.core.init.ItemInit;
import dev.momostudios.coldsweat.core.network.message.HearthFuelSyncMessage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.common.te.HearthBlockEntity;
import dev.momostudios.coldsweat.core.init.BlockInit;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

import java.util.*;

public class HearthTopBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new HashMap<>();

    public static Properties getProperties()
    {
        return Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .hardnessAndResistance(2f, 10f)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(1)
                .notSolid()
                .setLightLevel(s -> 0)
                .setOpaque((bs, br, bp) -> false);
    }

    public HearthTopBlock(Properties properties)
    {
        super(HearthTopBlock.getProperties());
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH));
        runCalculation(VoxelShapes.or(
            makeCuboidShape(3, -15, 4, 13, 3, 12), // Shell
            makeCuboidShape(8, 3, 6, 12, 15, 10), // Exhaust 1
            makeCuboidShape(6, 11.5, 5.5, 8, 15.5, 10.5), // Exhaust 2
            makeCuboidShape(13, -11, 6, 16, -3, 10), // Water Canister
            makeCuboidShape(0, -11, 6, 3, -3, 10))); // Lava Canister)
    }

    static void calculateShapes(Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[] { shape, VoxelShapes.empty() };

        int times = (to.getHorizontalIndex() - Direction.NORTH.getHorizontalIndex() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1],
                VoxelShapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }

        SHAPES.put(to, buffer[0]);
    }

    static void runCalculation(VoxelShape shape) {
        for (Direction direction : Direction.values()) {
            calculateShapes(direction, shape);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
    {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        if (!worldIn.isRemote)
        {
            if (worldIn.getTileEntity(pos.down()) instanceof HearthBlockEntity)
            {
                HearthBlockEntity te = (HearthBlockEntity) worldIn.getTileEntity(pos.down());
                ItemStack stack = player.getHeldItem(hand);
                int itemFuel = te.getItemFuel(stack);
                int hearthFuel = itemFuel > 0 ? te.getHotFuel() : te.getColdFuel();

                if (itemFuel != 0 && hearthFuel + Math.abs(itemFuel) * 0.75 < HearthBlockEntity.MAX_FUEL)
                {
                    if (!player.isCreative())
                    {
                        if (stack.hasContainerItem())
                        {
                            ItemStack container = stack.getContainerItem();
                            stack.shrink(1);
                            player.inventory.addItemStackToInventory(container);
                        }
                        else
                        {
                            stack.shrink(1);
                        }
                    }
                    te.addFuel(itemFuel);
                    te.updateFuelState();

                    worldIn.playSound(null, pos.down(), itemFuel > 0 ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY,
                            SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);

                    ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                            new HearthFuelSyncMessage(te.getPos(), te.getHotFuel(), te.getColdFuel()));
                }
                else
                {
                    NetworkHooks.openGui((ServerPlayerEntity) player, te, pos.down());
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        if (worldIn.getBlockState(pos.down()).getBlock() != BlockInit.HEARTH.get())
        {
            worldIn.destroyBlock(pos, false);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.matchesBlock(newState.getBlock()))
        {
            if (world.getBlockState(pos.down()).getBlock() == BlockInit.HEARTH.get())
            {
                world.destroyBlock(pos.down(), false);
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        return new ItemStack(ItemInit.HEARTH.get());
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        List<ItemStack> dropsOriginal = super.getDrops(state, builder);
        if (!dropsOriginal.isEmpty())
            return dropsOriginal;
        return Collections.singletonList(new ItemStack(ItemInit.HEARTH.get(), 1));
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction)
    {
        return state.with(FACING, direction.rotate(state.get(FACING)));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing());
    }
}
