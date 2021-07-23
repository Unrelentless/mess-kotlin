package com.unrelentless.mess.inventory

import com.unrelentless.mess.block.LimbBlock
import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.util.Level
import net.minecraft.block.Block
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction
import kotlin.math.min

class LimbInventory(val level: Level, private val owner: LimbBlockEntity?): SidedInventory {
    private var itemStacks: MutableList<ItemStack> = mutableListOf()

    override fun size(): Int = level.size
    override fun isEmpty(): Boolean = itemStacks.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = itemStacks.getOrElse(slot){ ItemStack.EMPTY }
    override fun canPlayerUse(player: PlayerEntity?): Boolean = true
    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = true
    override fun getMaxCountPerStack(): Int = itemStacks.getOrElse(0){ ItemStack.EMPTY }.maxCount
    override fun clear() = itemStacks.clear()
    override fun getAvailableSlots(side: Direction?): IntArray = IntArray(level.size)
    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        return Block.getBlockFromItem(stack?.item) !is LimbBlock
    }
    override fun removeStack(slot: Int): ItemStack {
        return removeStack(slot, itemStacks.getOrElse(slot){ ItemStack.EMPTY }.count)
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack {
        if(itemStacks.isEmpty()) return ItemStack.EMPTY

        val stack = itemStacks.last().split(amount)

        if(itemStacks.getOrNull(itemStacks.lastIndex)?.isEmpty == true)
            itemStacks.removeAt(itemStacks.lastIndex)

        stack.increment(itemStacks
            .getOrElse(itemStacks.lastIndex){ ItemStack.EMPTY }
            .split(amount - stack.count)
            .count)

        markDirty()
        return stack
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        if(stack.isEmpty) return
        if(itemStacks.getOrNull(slot) == null) return
        itemStacks[slot] = stack
        markDirty()
    }

    override fun markDirty() {
        if (owner == null || owner.world?.isClient == true) return

        owner.markDirty()
        if(owner.world?.isClient == false) owner.sync()
    }

    fun getStack(): ItemStack = getStack(itemStacks.lastIndex)
    fun withdrawStack(count: Int) = removeStack(itemStacks.lastIndex, count)
    fun depositStack(stack: ItemStack): ItemStack = depositStack(stack, stack.count)
    fun depositStack(stack: ItemStack, count: Int): ItemStack {
        if (Block.getBlockFromItem(stack.item) is LimbBlock) return stack

        if (itemStacks.isEmpty()) {
            itemStacks.add(stack.split(count))
        } else if (ItemStack.areItemsEqual(itemStacks.last(), stack)) {
            val firstStack = stack.split(min(count, count - itemStacks.last().count))
            itemStacks.last().increment(firstStack.count)
            if(firstStack.count < count) itemStacks.add(stack.split(count - firstStack.count))
        }

        markDirty()
        return stack
    }
}