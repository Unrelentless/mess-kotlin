package com.unrelentless.mess.block

import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.util.Level
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import javax.swing.Action

open class LimbBlock(settings: FabricBlockSettings, level: Level): BlockWithEntity(settings.nonOpaque()) {
    private val level: Level = level;

    override fun createBlockEntity(world: BlockView?): BlockEntity? = level.blockEntity
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        val blockEntity = world.getBlockEntity(pos)
        val handStack = player.getStackInHand(hand)

        require(blockEntity is LimbBlockEntity)

        if (Block.getBlockFromItem(handStack.item) is LimbBlock)
            return ActionResult.SUCCESS

        if (handStack.isEmpty)
            withdraw(player, hand, world, blockEntity)
        else
            deposit(handStack, player, hand, blockEntity)

        if(!world.isClient) blockEntity.sync()

        return ActionResult.SUCCESS
    }

    private fun withdraw(player: PlayerEntity, hand: Hand, world: World, blockEntity: LimbBlockEntity) {
        if(player.isSneaking)
            player.inventory.offerOrDrop(world, blockEntity.inventory.removeStack(0, 1))
        else
            player.inventory.offerOrDrop(world, blockEntity.inventory.removeStack(0))
    }

    private fun deposit(stack: ItemStack, player: PlayerEntity, hand: Hand, blockEntity: LimbBlockEntity) {
        player.setStackInHand(hand, blockEntity.inventory.depositStack(stack))
    }
}