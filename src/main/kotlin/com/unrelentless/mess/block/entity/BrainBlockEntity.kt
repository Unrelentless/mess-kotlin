package com.unrelentless.mess.block.entity

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.BrainBlock
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.registerBlockEntity
import com.unrelentless.mess.extensions.serializeInnerStackToTag
import com.unrelentless.mess.extensions.setChunkLoaded
import com.unrelentless.mess.screen.MessScreenHandler
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World


class BrainBlockEntity: BlockEntity(ENTITY_TYPE), ExtendedScreenHandlerFactory {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "brain_entity")
        val ENTITY_TYPE: BlockEntityType<BrainBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                    .create({ BrainBlockEntity() }, BrainBlock.BLOCK)
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
            val compoundTag = CompoundTag()
            val listTag = ListTag()
            limbs.map { it.inventory.getStack().serializeInnerStackToTag() }.forEach { listTag.add(it) }
            compoundTag.put("items", listTag)

            buf?.writeIntArray(sizes)
            buf?.writeCompoundTag(compoundTag)
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

    override fun fromTag(state: BlockState?, compoundTag: CompoundTag?) {
        super.fromTag(state, compoundTag)

        val listTag = (compoundTag?.get("tabs") as? ListTag) ?: return
        listTag.forEach { playerTags ->
            val tabTags = (playerTags as? CompoundTag) ?: return@forEach
            val playerId = tabTags.getString("id")
            Level.values().forEach {
                selectedTabs[playerId]?.put(it, tabTags.getBoolean(it.name))
            }
        }

        chunkLoad(compoundTag.getBoolean("chunkLoaded"))
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)

        val tabsTag = ListTag()

        selectedTabs.forEach { playerTabs ->
            val playerTag = CompoundTag()
            playerTag.putString("id", playerTabs.key)
            playerTabs.value.forEach {
                playerTag.putBoolean(it.key.name, it.value)
            }

            tabsTag.add(playerTag)
        }

        tag.put("tabs", tabsTag)
        tag.putBoolean("chunkLoaded", chunkLoaded)

        return tag
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