package com.minecraftabnormals.abnormals_core.common.blocks.wood;

import com.minecraftabnormals.abnormals_core.core.util.BlockUtil;
import com.minecraftabnormals.abnormals_core.core.util.item.filling.TargetedItemGroupFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.function.Supplier;

public class AbnormalsLogBlock extends RotatedPillarBlock {
	private static final TargetedItemGroupFiller FILLER = new TargetedItemGroupFiller(() -> Items.WARPED_STEM);
	private final Supplier<Block> block;

	public AbnormalsLogBlock(Supplier<Block> strippedBlock, Properties properties) {
		super(properties);
		this.block = strippedBlock;
	}

	@Override
	public BlockState getToolModifiedState(BlockState state, Level world, BlockPos pos, Player player, ItemStack stack, ToolAction action) {
		if (action == ToolActions.AXE_STRIP)
			return this.block != null ? BlockUtil.transferAllBlockStates(state, this.block.get().defaultBlockState()) : null;
		return super.getToolModifiedState(state, world, pos, player, stack, action);
	}

	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
		FILLER.fillItem(this.asItem(), group, items);
	}
}