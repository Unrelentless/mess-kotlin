package com.unrelentless.mess.block.entity

import com.unrelentless.mess.util.*
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.math.BlockPos
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
                set: Pair<ArrayList<LimbBlockEntity>, ArrayList<BrainBlockEntity>> = Pair(arrayListOf(), arrayListOf())
        ): Pair<ArrayList<LimbBlockEntity>, ArrayList<BrainBlockEntity>> {
            val posArray = arrayOf(
                    BlockPos(1, 0, 0),
                    BlockPos(0, 1, 0),
                    BlockPos(0, 0, 1),
                    BlockPos(-1, 0, 0),
                    BlockPos(0, -1, 0),
                    BlockPos(0, 0, -1)
            )

            // Recursion? - Why not!
            posArray.forEach {
                val nextPos = pos.add(it)
                val nextBlock = world?.getBlockEntity(nextPos)
                if (nextBlock is LimbBlockEntity && !set.first.contains(nextBlock) && !nextBlock.isRemoved) {
                    set.first.add(nextBlock)
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

    fun onPlaced() {
        updateBrains()
        println("ON PLACE LIMB - BRAINS = ${linkedBrains.size}")
        linkedBrains.forEach {
            it.updateLimbs()
        }
    }

    fun onBroken(fromPos: BlockPos) {
        println("ON BROKE LIMB - BRAINS = ${linkedBrains.size}")
        linkedBrains.forEach { it.updateLimbs(fromPos) }
    }

    fun addBrain(brainBlockEntity: BrainBlockEntity) {
        println("ON ADD BRAIN BEFORE: ${linkedBrains.size}")
        linkedBrains.add(brainBlockEntity)
        println("ON ADD BRAIN AFTER: ${linkedBrains.size}")
    }

    fun removeBrain(brainBlockEntity: BrainBlockEntity) {
        println("ON REMOVE BRAIN BEFORE: ${linkedBrains.size}")
        linkedBrains.remove(brainBlockEntity)
        println("ON REMOVE BRAIN AFTER: ${linkedBrains.size}")
        if(linkedBrains.isEmpty()) setChunkLoaded(false)
    }

    fun updateBrains() {
        println("ON UPDATE $pos BRAINS BEFORE: ${linkedBrains.size}")
        linkedBrains.clear()
        linkedBrains.addAll(findLimbsAndBrains(world as World, pos).second)
        println("ON UPDATE $pos BRAINS AFTER: ${linkedBrains.size}")
    }
}