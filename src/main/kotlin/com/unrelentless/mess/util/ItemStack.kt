package com.unrelentless.mess.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry

fun ItemStack.serializeInnerStackToTag(): CompoundTag {
    val mainTag = CompoundTag()
    val itemStack = if(!isEmpty) this else ItemStack.EMPTY

    val compoundTag = CompoundTag()
    val identifier = Registry.ITEM.getId(itemStack.item)

    compoundTag.putString("id", identifier.toString())
    compoundTag.putInt("count", itemStack.count)

    if (itemStack.tag != null) { compoundTag.put("tag", itemStack.tag?.copy()) }
    mainTag.put("item", compoundTag)

    return mainTag
}

fun ItemStack.deserializeBrain(): Pair<BlockPos, String>? {
    val brainTag = this.getSubTag("brain") ?: return null

    return Pair(BlockPos(
            brainTag.getInt("x"),
            brainTag.getInt("y"),
            brainTag.getInt("z")),
            brainTag.getString("world")
    )
}