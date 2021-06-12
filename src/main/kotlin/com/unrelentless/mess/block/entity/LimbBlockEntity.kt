package com.unrelentless.mess.block.entity

import com.unrelentless.mess.extensions.deserializeLimb
import com.unrelentless.mess.extensions.serializeLimb
import com.unrelentless.mess.extensions.setChunkLoaded
import com.unrelentless.mess.inventory.LimbInventory
import com.unrelentless.mess.util.*
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

open class LimbBlockEntity(
    type: BlockEntityType<*>,
    val level: Level,
    pos: BlockPos?,
    state: BlockState?,
) : BlockEntity(type, pos, state), BlockEntityClientSerializable, InventoryProvider {

    companion object {
        private fun findLimbsAndBrains(
                world: World?,
                pos: BlockPos,
                ignoringPos: BlockPos? = null,
                set: Pair<ArrayList<BlockPos>, ArrayList<BrainBlockEntity>> = Pair(arrayListOf(), arrayListOf())
        ): Pair<ArrayList<BlockPos>, ArrayList<BrainBlockEntity>> {
            Direction.values().forEach {
                val nextPos = pos.offset(it)
                val nextBlock = world?.getBlockEntity(nextPos) ?: return@forEach

                if (nextPos != ignoringPos && nextBlock is LimbBlockEntity && !set.first.contains(nextPos) && !nextBlock.isRemoved) {
                    set.first.add(nextPos)
                    findLimbsAndBrains(world, nextPos, ignoringPos, set)
                } else if(nextBlock is BrainBlockEntity && !set.second.contains(nextBlock)) {
                    set.second.add(nextBlock)
                }
            }

            return set
        }
    }

    val inventory: LimbInventory by lazy { LimbInventory(level, this) }
    private val linkedBrains: MutableSet<BrainBlockEntity> = mutableSetOf()
    var chunkLoaded: Boolean = false
        private set

    override fun fromClientTag(tag: NbtCompound) = readNbt(tag)
    override fun toClientTag(tag: NbtCompound): NbtCompound = writeNbt(tag)

    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)
        chunkLoad(nbt!!.getBoolean("chunkLoaded"))
    }

    override fun writeNbt(nbt: NbtCompound?): NbtCompound {
        nbt?.putBoolean("chunkLoaded", chunkLoaded)
        return super.writeNbt(nbt?.serializeLimb(inventory))
    }

//    override fun fromTag(state: BlockState?, tag: NbtCompound){
//        super.fromTag(state, tag.deserializeLimb(inventory))
//        chunkLoad(tag.getBoolean("chunkLoaded"))
//    }
//    override fun toTag(tag: NbtCompound): NbtCompound {
//        tag.putBoolean("chunkLoaded", chunkLoaded)
//        return super.toTag(tag.serializeLimb(inventory))
//    }

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory = inventory

    fun onContentChanged(player: PlayerEntity? = null) = linkedBrains.forEach { it.contentChanged(player) }
    fun onPlaced() {
        findBrains()
        linkedBrains.forEach(BrainBlockEntity::updateLimbs)
        onContentChanged()
    }

    fun onBreak(fromPos: BlockPos) {
        linkedBrains.forEach { it.updateLimbs(fromPos) }
        linkedBrains.forEach(BrainBlockEntity::updateBrains)
        chunkLoad(false)
        onContentChanged()
    }

    fun getBrains(): List<BrainBlockEntity> = linkedBrains.toList()
    fun addBrain(brainBlockEntity: BrainBlockEntity) = linkedBrains.add(brainBlockEntity)

    fun removeBrain(brainBlockEntity: BrainBlockEntity) {
        linkedBrains.remove(brainBlockEntity)
        if(linkedBrains.isEmpty()) { chunkLoad(false) }
    }

    fun findBrains() {
        linkedBrains.clear()
        linkedBrains.addAll(findLimbsAndBrains(world as World, pos, pos).second)
        if(linkedBrains.isEmpty()) { chunkLoad(false) }
    }

    fun chunkLoad(chunkLoad: Boolean) {
        chunkLoaded = chunkLoad
        setChunkLoaded(chunkLoad)
        markDirty()
    }
}