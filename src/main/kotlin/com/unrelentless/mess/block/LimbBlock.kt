package com.unrelentless.mess.block

import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World


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

        return ActionResult.SUCCESS
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        val blockEntity: LimbBlockEntity = world.getBlockEntity(pos) as LimbBlockEntity

        if (!world.isClient && !blockEntity.inventory.isEmpty) {
            val itemStack = ItemStack(asItem())
            val innerStack = blockEntity.inventory.getStack()

            itemStack.putSubTag("BlockEntityTag", innerStack.serializeInnerStackToTag())

            val itemEntity = ItemEntity(
                    world,
                    pos.x.toDouble() + 0.5,
                    pos.y.toDouble() + 0.5,
                    pos.z.toDouble() + 0.5,
                    itemStack
            )

            itemEntity.setToDefaultPickupDelay()
            world.spawnEntity(itemEntity)
        }
    }

    override fun getDroppedStacks(state: BlockState?, builder: LootContext.Builder?): MutableList<ItemStack> {
        return DefaultedList.of()
    }

    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: BlockView?, tooltip: MutableList<Text?>, options: TooltipContext?) {
        super.appendTooltip(stack, world, tooltip, options)
        val compoundTag = stack.getSubTag("BlockEntityTag")

        if (compoundTag != null) {
            if (compoundTag.contains("item", 10)) {
                val innerStack = compoundTag.deserializeInnerStack()

                val mutableText = innerStack.name.shallowCopy()
                mutableText.append(" x").append(innerStack.count.toString())
                tooltip.add(mutableText)
            }
        }
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