package com.unrelentless.mess.block.entity

import com.unrelentless.mess.util.LimbInventory
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.WorldAccess


open class LimbBlockEntity(type: BlockEntityType<*>, size: Int) : BlockEntity(type), BlockEntityClientSerializable, InventoryProvider {

    val inventory: LimbInventory by lazy { LimbInventory(size, this) }

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        if(tag.contains("item")) {
            val compoundTag = tag.getCompound("item")
            val item = Registry.ITEM[Identifier(compoundTag.getString("id"))]
            val stack = ItemStack(item)
            stack.count = compoundTag.getInt("count")

            if(compoundTag.contains("tag"))
                stack.tag = compoundTag.getCompound("tag")

            inventory.setStack(0, stack)
        }
        super.fromTag(state, tag)
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        val itemStack = inventory.getStack()

        if (!itemStack.isEmpty) {
            val compoundTag = CompoundTag()
            compoundTag.putString("id", Registry.ITEM.getId(itemStack.item).toString())
            compoundTag.putInt("count", itemStack.count)

            if (itemStack.tag != null)
                compoundTag.put("tag", itemStack.tag?.copy())

            tag.put("item", compoundTag)
        }

        return super.toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag) = fromTag(cachedState, tag)
    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)
    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun sync() {
        if (world!!.isClient) return
        super.sync()
        markDirty()
    }
}