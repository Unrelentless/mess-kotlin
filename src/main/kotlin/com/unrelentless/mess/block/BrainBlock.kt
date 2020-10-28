package com.unrelentless.mess.block

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.entity.BrainBlockEntity
import com.unrelentless.mess.client.gui.screen.MessScreen
import com.unrelentless.mess.item.EnderLinkItem
import com.unrelentless.mess.settings.brainBlockSettings
import com.unrelentless.mess.settings.messBlockItemSettings
import com.unrelentless.mess.util.registerBlock
import com.unrelentless.mess.util.registerBlockItem
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class BrainBlock: BlockWithEntity(brainBlockSettings) {
    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "brain")
        val BLOCK = registerBlock(BrainBlock(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, messBlockItemSettings)
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? = BrainBlockEntity()
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if(world.isClient || (player.isSneaking && player.mainHandStack.item == EnderLinkItem.ITEM))
            return ActionResult.SUCCESS

        MessScreen.openScreen(world, pos, player)
        return ActionResult.SUCCESS
    }

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack?) {
        if(!world.isClient) (world.getBlockEntity(pos) as? BrainBlockEntity)?.onPlaced()
        super.onPlaced(world, pos, state, placer, itemStack)
    }

    override fun afterBreak(world: World?, player: PlayerEntity, pos: BlockPos, state: BlockState, blockEntity: BlockEntity?, stack: ItemStack?) {
        (blockEntity as? BrainBlockEntity)?.onBroken()
        super.onBreak(world, pos, state, player)
    }
}