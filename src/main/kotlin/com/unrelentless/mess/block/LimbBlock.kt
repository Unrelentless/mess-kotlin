package com.unrelentless.mess.block

import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.extensions.deserializeInnerStack
import com.unrelentless.mess.extensions.serializeInnerStackToTag
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import java.util.*
import kotlin.math.min


open class LimbBlock(settings: FabricBlockSettings, private val level: Level): BlockWithEntity(settings.nonOpaque()) {

    override fun createBlockEntity(world: BlockView?): BlockEntity? = level.blockEntity
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        val blockEntity = world.getBlockEntity(pos) as? LimbBlockEntity ?: return ActionResult.SUCCESS
        if (Block.getBlockFromItem(player.mainHandStack.item) is LimbBlock) return ActionResult.SUCCESS

        if (player.mainHandStack.isEmpty) {
            withdraw(player, world, blockEntity)
        } else {
            deposit(player.mainHandStack, player, hand, blockEntity)
        }

        return ActionResult.SUCCESS
    }

    override fun onBreak(world: World, pos: BlockPos, state: BlockState, player: PlayerEntity) {
        super.onBreak(world, pos, state, player)
        if(world.isClient) return

        val blockEntity = world.getBlockEntity(pos) as? LimbBlockEntity ?: return
        val itemStack = ItemStack(asItem())

        if (!blockEntity.inventory.isEmpty) {
            val innerStack = blockEntity.inventory.getStack()
            itemStack.putSubTag("BlockEntityTag", innerStack.serializeInnerStackToTag())
        }

        val itemEntity = ItemEntity(
                world,
                pos.x.toDouble() + 0.5,
                pos.y.toDouble() + 0.5,
                pos.z.toDouble() + 0.5,
                itemStack
        )

        itemEntity.setToDefaultPickupDelay()
        world.spawnEntity(itemEntity)

        blockEntity.onBreak(pos)
    }

    override fun onBroken(world: WorldAccess, pos: BlockPos, state: BlockState) {
        super.onBroken(world, pos, state)
        if(!world.isClient) (world as? World)?.updateNeighborsAlways(pos, this)
    }

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        super.onPlaced(world, pos, state, placer, itemStack)
        if(world.isClient) return

        (world.getBlockEntity(pos) as? LimbBlockEntity)?.onPlaced()
        world.updateNeighborsAlways(pos, this)
    }

    override fun neighborUpdate(state: BlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos, notify: Boolean) {
        super.neighborUpdate(state,world, pos, block, fromPos, notify)
        if(!world.isClient) (world.getBlockEntity(pos) as? LimbBlockEntity)?.findBrains()
    }

    override fun getDroppedStacks(state: BlockState?, builder: LootContext.Builder?): MutableList<ItemStack> {
        return DefaultedList.of()
    }

    override fun appendTooltip(stack: ItemStack, world: BlockView?, tooltip: MutableList<Text?>, options: TooltipContext?) {
        super.appendTooltip(stack, world, tooltip, options)
        val compoundTag = stack.getSubTag("BlockEntityTag")

        compoundTag?.deserializeInnerStack()?.let {
            val mutableText = it.name.shallowCopy()
            mutableText.append(" x").append(it.count.toString())
            tooltip.add(mutableText.formatted(Formatting.GRAY))
        }
    }

    private fun withdraw(player: PlayerEntity, world: World, blockEntity: LimbBlockEntity) {
        val count = if(!player.isSneaking) {
            min(blockEntity.inventory.getStack().count, blockEntity.inventory.getStack().item.maxCount)
        } else 1
        player.inventory.offerOrDrop(world, blockEntity.inventory.withdrawStack(count))
        blockEntity.onContentChanged(player)
    }

    private fun deposit(stack: ItemStack, player: PlayerEntity, hand: Hand, blockEntity: LimbBlockEntity) {
        player.setStackInHand(hand, blockEntity.inventory.depositStack(stack))
        blockEntity.onContentChanged(player)
    }
}