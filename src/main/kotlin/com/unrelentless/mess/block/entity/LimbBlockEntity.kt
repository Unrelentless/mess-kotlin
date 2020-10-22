package com.unrelentless.mess.block.entity

import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.LimbInventory
import com.unrelentless.mess.util.deserializeLimb
import com.unrelentless.mess.util.serializeLimb
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess


open class LimbBlockEntity(type: BlockEntityType<*>, val level: Level) : BlockEntity(type), BlockEntityClientSerializable, InventoryProvider {

    val inventory: LimbInventory by lazy { LimbInventory(level, this) }

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag.deserializeLimb(inventory))
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        return super.toTag(tag.serializeLimb(inventory))
    }

    override fun fromClientTag(tag: CompoundTag) = fromTag(cachedState, tag)
    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)
    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    override fun sync() {
        super.sync()
        markDirty()
    }
}