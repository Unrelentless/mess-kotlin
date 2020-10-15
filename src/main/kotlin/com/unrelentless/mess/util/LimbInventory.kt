package com.unrelentless.mess.util

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Direction

class LimbInventory(size: Int, val owner: BlockEntity): SidedInventory {
    val items: DefaultedList<ItemStack> = DefaultedList.ofSize(size, ItemStack.EMPTY)

    override fun clear() = items.clear()
    override fun size(): Int = items.size
    override fun isEmpty(): Boolean = items.isEmpty()
    override fun getStack(slot: Int): ItemStack = items[slot]
    override fun canPlayerUse(player: PlayerEntity?): Boolean = true
    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = true
    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = true

    override fun removeStack(slot: Int): ItemStack {
        val itemStack: ItemStack = Inventories.removeStack(items, slot)
        markDirty()
        return itemStack
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val itemStack: ItemStack = Inventories.splitStack(items, slot, amount)
        markDirty()
        return itemStack
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
        markDirty()
    }

    override fun getAvailableSlots(side: Direction?): IntArray {
        // Just return an array of all slots
        val result = IntArray(items.size)
        for (i in result.indices) {
            result[i] = i
        }

        return result
    }

    override fun markDirty() {
        owner.markDirty()
        require(owner is BlockEntityClientSerializable)
        if (owner.world?.isClient == false) owner.sync()
    }
}