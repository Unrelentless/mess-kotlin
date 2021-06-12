package com.unrelentless.mess.extensions

import com.unrelentless.mess.inventory.LimbInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun NbtCompound.serializeLimb(limbInventory: LimbInventory): NbtCompound {
    val itemStack = limbInventory.getStack()
    val compoundTag = NbtCompound()

    compoundTag.putString("id", Registry.ITEM.getId(itemStack.item).toString())
    compoundTag.putInt("count", itemStack.count)

    if (itemStack.tag != null) { compoundTag.put("tag", itemStack.tag?.copy()) }

    put("item", compoundTag)

    return this
}

fun NbtCompound.deserializeLimb(limbInventory: LimbInventory): NbtCompound {
    if(!contains("item")) return this

    val compoundTag = this.getCompound("item")
    val item = Registry.ITEM[Identifier(compoundTag.getString("id"))]
    val stack = ItemStack(item)

    stack.count = compoundTag.getInt("count")

    if(compoundTag.contains("tag")) { stack.tag = compoundTag.getCompound("tag") }
    limbInventory.setStack(0, stack)

    return this
}

fun NbtCompound.deserializeInnerStack(): ItemStack? {
    if(!this.contains("item")) return null

    val compoundTag = this.getCompound("item")
    val item = Registry.ITEM[Identifier(compoundTag.getString("id"))]
    val itemStack = ItemStack(item)
    itemStack.count = compoundTag.getInt("count")

    if(compoundTag.contains("tag")) { itemStack.tag = compoundTag.getCompound("tag") }

    return itemStack
}

