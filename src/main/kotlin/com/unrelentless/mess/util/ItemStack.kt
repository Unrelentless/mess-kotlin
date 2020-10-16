package com.unrelentless.mess.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.registry.Registry

fun ItemStack.serializeInnerStackToTag(): CompoundTag {
    val mainTag = CompoundTag()

    if (!this.isEmpty) {
        val compoundTag = CompoundTag()
        val identifier = Registry.ITEM.getId(this.item)

        compoundTag.putString("id", identifier.toString())
        compoundTag.putInt("count", this.count)

        if (this.tag != null) {
            compoundTag.put("tag", this.tag?.copy())
        }

        mainTag.put("item", compoundTag)
    }

    return mainTag
}