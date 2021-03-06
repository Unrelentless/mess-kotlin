package com.unrelentless.mess.screen.slot

import com.unrelentless.mess.block.LimbBlock
import net.minecraft.block.Block
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot

class LimbSlot(inventory: Inventory?, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
    override fun canInsert(stack: ItemStack?): Boolean = Block.getBlockFromItem(stack?.item) !is LimbBlock
}