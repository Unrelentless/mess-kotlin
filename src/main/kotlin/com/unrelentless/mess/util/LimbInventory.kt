package com.unrelentless.mess.util

import com.unrelentless.mess.block.LimbBlock
import com.unrelentless.mess.block.entity.LimbBlockEntity
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction
import kotlin.math.min

class LimbInventory(val level: Level, private val owner: LimbBlockEntity?): SidedInventory {
    private var itemStack: ItemStack = ItemStack.EMPTY

    override fun size(): Int = 1
    override fun isEmpty(): Boolean = itemStack.isEmpty
    override fun getStack(slot: Int): ItemStack = itemStack
    override fun canPlayerUse(player: PlayerEntity?): Boolean = true
    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = true
    override fun getMaxCountPerStack(): Int = level.size * itemStack.item.maxCount
    override fun clear() { itemStack = ItemStack.EMPTY }
    override fun removeStack(slot: Int): ItemStack = removeStack(slot, itemStack.item.maxCount)
    override fun getAvailableSlots(side: Direction?): IntArray = IntArray(1)
    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        return Block.getBlockFromItem(stack?.item) !is LimbBlock
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val newStack: ItemStack = itemStack.split(amount)
        markDirty()
        return newStack
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        itemStack = stack
        if (stack.count > maxCountPerStack) { stack.count = maxCountPerStack }
        markDirty()
    }

    override fun markDirty() {
        if (owner == null || owner.world?.isClient == true) return

        owner.markDirty()
        if(owner?.world?.isClient == false) (owner as? BlockEntityClientSerializable)?.sync()
    }

    fun getStack(): ItemStack = getStack(0)
    fun withdrawStack(count: Int) = removeStack(0, count)
    fun depositStack(stack: ItemStack): ItemStack = depositStack(stack, stack.count)
    fun depositStack(stack: ItemStack, count: Int): ItemStack {
        if (Block.getBlockFromItem(stack.item) is LimbBlock) return stack

        if (itemStack.isEmpty) {
            itemStack = stack.split(count)
        } else if (ItemStack.areItemsEqual(itemStack, stack)) {
            val countToDeposit = min(count, maxCountPerStack - itemStack.count)
            itemStack.increment(countToDeposit)
            stack.decrement(countToDeposit)
        }

        markDirty()
        return stack
    }
}