package com.unrelentless.mess.block

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.entity.HeartBlockEntity
import com.unrelentless.mess.block.settings.heartBlockSettings
import com.unrelentless.mess.util.registerBlock
import com.unrelentless.mess.util.registerBlockItem
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World


class HeartBlock: BlockWithEntity(heartBlockSettings) {
    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "heart")
        val BLOCK = registerBlock(HeartBlock(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.MISC))
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? = HeartBlockEntity()
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun onUse(state: BlockState?, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if(world.isClient) return ActionResult.SUCCESS

        state?.createScreenHandlerFactory(world, pos).let {
            if (world.getBlockEntity(pos) is HeartBlockEntity) {
                player.openHandledScreen(it)
            }
        }

        return ActionResult.SUCCESS
    }
}