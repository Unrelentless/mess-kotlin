package com.unrelentless.mess.block

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.entity.HeartBlockEntity
import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.item.PortableMessItem
import com.unrelentless.mess.settings.heartBlockSettings
import com.unrelentless.mess.settings.messItemSettings
import com.unrelentless.mess.util.registerBlock
import com.unrelentless.mess.util.registerBlockItem
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import java.util.*

class HeartBlock: BlockWithEntity(heartBlockSettings) {
    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "heart")
        val BLOCK = registerBlock(HeartBlock(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, messItemSettings)

        fun openScreen(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity) {
            state?.createScreenHandlerFactory(world, pos).let {
                val blockEntity = world.getBlockEntity(pos) as? HeartBlockEntity
                blockEntity?.setLimbs(findLimbs(world, pos).toTypedArray())
                player.openHandledScreen(it)
            }
        }

        private fun findLimbs(
                world: World?,
                pos: BlockPos,
                set: HashSet<LimbBlockEntity> = hashSetOf()
        ): HashSet<LimbBlockEntity> {
            val posArray = arrayOf(
                    BlockPos(1, 0, 0),
                    BlockPos(0, 1, 0),
                    BlockPos(0, 0, 1),
                    BlockPos(-1, 0, 0),
                    BlockPos(0, -1, 0),
                    BlockPos(0, 0, -1)
            )

            // Recursion? - Why not!
            posArray.forEach {
                val nextPos = pos.add(it)
                val nextBlock = world?.getBlockEntity(nextPos)
                if (nextBlock is LimbBlockEntity && !set.contains(nextBlock)) {
                    set.add(nextBlock)
                    findLimbs(world, nextPos, set)
                }
            }

            return set
        }
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? = HeartBlockEntity()
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if(world.isClient || (player.isSneaking && player.mainHandStack.item == PortableMessItem.ITEM))
            return ActionResult.SUCCESS

        openScreen(state, world, pos, player)
        return ActionResult.SUCCESS
    }
}