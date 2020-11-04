package com.unrelentless.mess.block.entity

import com.unrelentless.mess.util.*
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

open class LimbBlockEntity(
        type: BlockEntityType<*>,
        val level: Level
) : BlockEntity(type), BlockEntityClientSerializable, InventoryProvider {

    companion object {
        private fun findLimbsAndBrains(
                world: World?,
                pos: BlockPos,
                set: Pair<ArrayList<BlockPos>, ArrayList<BrainBlockEntity>> = Pair(arrayListOf(), arrayListOf())
        ): Pair<ArrayList<BlockPos>, ArrayList<BrainBlockEntity>> {
            Direction.values().forEach {
                val nextPos = pos.offset(it)
                val nextBlock = world?.getBlockEntity(nextPos) ?: return@forEach

                if (nextBlock is LimbBlockEntity && !set.first.contains(nextPos) && !nextBlock.isRemoved) {
                    set.first.add(nextPos)
                    findLimbsAndBrains(world, nextPos, set)
                } else if(nextBlock is BrainBlockEntity && !set.second.contains(nextBlock)) {
                    set.second.add(nextBlock)
                }
            }

            return set
        }
    }

    val inventory: LimbInventory by lazy { LimbInventory(level, this) }
    private val linkedBrains: MutableSet<BrainBlockEntity> = mutableSetOf()

    override fun fromClientTag(tag: CompoundTag) = fromTag(cachedState, tag)
    override fun toClientTag(tag: CompoundTag): CompoundTag = toTag(tag)
    override fun fromTag(state: BlockState?, tag: CompoundTag) = super.fromTag(state, tag.deserializeLimb(inventory))
    override fun toTag(tag: CompoundTag): CompoundTag = super.toTag(tag.serializeLimb(inventory))
    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    fun onContentChanged(player: PlayerEntity? = null) {
        linkedBrains.forEach { it.contentChanged(player) }
    }

    fun onPlaced() {
        findBrains()
        linkedBrains.forEach(BrainBlockEntity::updateLimbs)
        onContentChanged(null)
    }

    fun onBroken(fromPos: BlockPos) {
        linkedBrains.forEach { it.updateLimbs(fromPos) }
        linkedBrains.forEach(BrainBlockEntity::updateBrains)
        onContentChanged(null)
    }

    fun addBrain(brainBlockEntity: BrainBlockEntity) {
        linkedBrains.add(brainBlockEntity)
    }

    fun removeBrain(brainBlockEntity: BrainBlockEntity) {
        linkedBrains.remove(brainBlockEntity)
        if(linkedBrains.isEmpty()) setChunkLoaded(false)
    }

    fun findBrains() {
        linkedBrains.clear()
        linkedBrains.addAll(findLimbsAndBrains(world as World, pos).second)
    }

    fun getBrains(): List<BrainBlockEntity> {
        return linkedBrains.toList()
    }
}