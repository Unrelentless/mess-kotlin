package com.unrelentless.mess.block.entity

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.BrainBlock
import com.unrelentless.mess.extensions.serializeInnerStackToTag
import com.unrelentless.mess.extensions.setChunkLoaded
import com.unrelentless.mess.screen.MessScreenHandler
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.registerBlockEntity
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World


class BrainBlockEntity(pos: BlockPos?, state: BlockState?) : BlockEntity(ENTITY_TYPE, pos, state), ExtendedScreenHandlerFactory {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "brain_entity")
        val ENTITY_TYPE: BlockEntityType<BrainBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                .create({ pos, state -> BrainBlockEntity(pos, state) }, BrainBlock.BLOCK)
                .build(null)
        }

        private fun findLimbs(
                world: World?,
                pos: BlockPos,
                ignoringPos: BlockPos? = null,
                list: ArrayList<LimbBlockEntity> = arrayListOf()
        ): ArrayList<LimbBlockEntity> {
            Direction.values().forEach {
                val nextPos = pos.offset(it)
                val nextBlock = world?.getBlockEntity(nextPos) as? LimbBlockEntity ?: return@forEach

                if (nextPos != ignoringPos && !list.contains(nextBlock) && !nextBlock.isRemoved) {
                    list.add(nextBlock)
                    findLimbs(world, nextPos, ignoringPos, list)
                }
            }

            return list
        }

        private fun writeLimbsToBuffer(
                limbs : MutableList<LimbBlockEntity>,
                tabs: HashMap<Level, Boolean>?,
                scrollPosition: Float,
                searchString: String,
                buf: PacketByteBuf?
        ) {
            val sizes = limbs.map { it.level.size }.toIntArray()
            val compoundTag = NbtCompound()
            val listTag = NbtList()
            limbs.map { it.inventory.getStack().serializeInnerStackToTag() }.forEach { listTag.add(it) }
            compoundTag.put("items", listTag)

            buf?.writeIntArray(sizes)
            buf?.writeNbt(compoundTag)
            buf?.writeBoolean(tabs != null)

            tabs?.forEach {
                buf?.writeEnumConstant(it.key)
                buf?.writeBoolean(it.value)
            }

            buf?.writeFloat(scrollPosition)
            buf?.writeString(searchString)
        }
    }
    val limbs: MutableList<LimbBlockEntity> = mutableListOf()

    private val selectedTabs: HashMap<String, HashMap<Level, Boolean>> = hashMapOf()
    private val searchStrings: HashMap<String, String> = hashMapOf()
    private val scrolledPositions: HashMap<String, Float> = hashMapOf()
    var chunkLoaded: Boolean = false
        private set

    override fun createMenu(
            syncId: Int,
            playerInventory: PlayerInventory,
            player: PlayerEntity
    ): ScreenHandler? = MessScreenHandler(syncId, playerInventory, this)

    override fun getDisplayName(): Text = TranslatableText("container." + Mess.IDENTIFIER + ".mess")
    override fun writeScreenOpeningData(serverPlayerEntity: ServerPlayerEntity, packetByteBuf: PacketByteBuf?) {
        writeLimbsToBuffer(
                limbs,
                selectedTabs[serverPlayerEntity.uuidAsString],
                scrolledPositions[serverPlayerEntity.uuidAsString] ?: 0.0f,
                searchStrings[serverPlayerEntity.uuidAsString] ?: "",
                packetByteBuf
        )
    }

    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)

        val listTag = (nbt?.get("tabs") as? NbtList) ?: return
        listTag.forEach { playerTags ->
            val tabTags = (playerTags as? NbtCompound) ?: return@forEach
            val playerId = tabTags.getString("id")

            selectedTabs[playerId] = hashMapOf()
            Level.values().forEach {
                selectedTabs[playerId]?.set(it, tabTags.getBoolean(it.name))
            }
        }

        chunkLoad(nbt.getBoolean("chunkLoaded"))
    }

    override fun writeNbt(nbt: NbtCompound?): NbtCompound {
        super.writeNbt(nbt)

        val tabsTag = NbtList()

        selectedTabs.forEach { playerTabs ->
            val playerTag = NbtCompound()
            playerTag.putString("id", playerTabs.key)
            playerTabs.value.forEach {
                playerTag.putBoolean(it.key.name, it.value)
            }

            tabsTag.add(playerTag)
        }

        nbt?.put("tabs", tabsTag)
        nbt?.putBoolean("chunkLoaded", chunkLoaded)

        return nbt!!
    }

    fun onPlaced() = findLimbs(world as World, pos).forEach{ it.addBrain(this) }
    fun onBroken() = findLimbs(world as World, pos).forEach{ it.removeBrain(this) }
    fun updateBrains() = limbs.forEach(LimbBlockEntity::findBrains)
    fun updateLimbs(ignoringPos: BlockPos? = null) {
        limbs.clear()
        limbs.addAll(findLimbs(world, pos, ignoringPos))
        chunkLoad(chunkLoaded)
    }

    fun contentChangedByPlayer(player: PlayerEntity) {
        val brainsToUpdate = limbs.map { it.getBrains() }.flatten().toSet()
        brainsToUpdate.forEach{ it.contentChanged(player) }
    }

    fun contentChanged(player: PlayerEntity? = null) {
        world?.players
                ?.filter { it != player }
                ?.filter {(it.currentScreenHandler as? MessScreenHandler)?.owner == this }
                //TODO: Make this actually update the limbs instead of just reopening the screen
                ?.forEach { MessScreenHandler.openScreen(cachedState, world!!, pos, it) }
    }

    fun updateTabs(newTabs: HashMap<Level, Boolean>, player: PlayerEntity) {
        selectedTabs[player.uuidAsString] = newTabs
    }

    fun updateSearchString(searchString: String, player: PlayerEntity) {
        searchStrings[player.uuidAsString] = searchString
    }

    fun updateScrollPosition(scrollPosition: Float, player: PlayerEntity) {
        scrolledPositions[player.uuidAsString] = scrollPosition
    }

    fun chunkLoad(chunkLoad: Boolean) {
        limbs.forEach { it.chunkLoad(chunkLoad) }

        chunkLoaded = chunkLoad
        setChunkLoaded(chunkLoad)
        markDirty()
    }
}