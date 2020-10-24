package com.unrelentless.mess.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry

fun CompoundTag.serializeLimb(limbInventory: LimbInventory): CompoundTag {
    val itemStack = limbInventory.getStack()

    val compoundTag = CompoundTag()

    compoundTag.putString("id", Registry.ITEM.getId(itemStack.item).toString())
    compoundTag.putInt("count", itemStack.count)

    if (itemStack.tag != null)
        compoundTag.put("tag", itemStack.tag?.copy())

    this.put("item", compoundTag)

    return this
}

fun CompoundTag.deserializeLimb(limbInventory: LimbInventory): CompoundTag {
    if(!this.contains("item")) return this

    val compoundTag = this.getCompound("item")
    val item = Registry.ITEM[Identifier(compoundTag.getString("id"))]
    val stack = ItemStack(item)

    stack.count = compoundTag.getInt("count")

    if(compoundTag.contains("tag"))
        stack.tag = compoundTag.getCompound("tag")

    limbInventory.setStack(0, stack)

    return this
}

fun CompoundTag.deserializeInnerStack(): ItemStack? {
    if(!this.contains("item")) return null

    val compoundTag = this.getCompound("item")
    val item = Registry.ITEM[Identifier(compoundTag.getString("id"))]
    val itemStack = ItemStack(item)
    itemStack.count = compoundTag.getInt("count")

    if(compoundTag.contains("tag")) {
        itemStack.tag = compoundTag.getCompound("tag")
    }

    return itemStack
}

