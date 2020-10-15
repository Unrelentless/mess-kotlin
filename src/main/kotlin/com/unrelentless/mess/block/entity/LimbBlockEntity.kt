package com.unrelentless.mess.block.entity

import com.unrelentless.mess.util.LimbInventory
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess

open class LimbBlockEntity(type: BlockEntityType<*>, size: Int) : BlockEntity(type), BlockEntityClientSerializable, InventoryProvider {

    val inventory: SidedInventory by lazy { LimbInventory(size, this) }

    override fun fromTag(state: BlockState?, tag: CompoundTag?) {
        super.fromTag(state, tag)
    }

    override fun toTag(tag: CompoundTag?): CompoundTag {
        return super.toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag?) = fromTag(cachedState, tag)
    override fun toClientTag(tag: CompoundTag?): CompoundTag = toTag(tag)
    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun sync() {
        super.sync()
        markDirty()
    }
}