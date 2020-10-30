package com.unrelentless.mess.block.entity

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.BrainBlock
import com.unrelentless.mess.client.gui.screen.MessScreenHandler
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.registerBlockEntity
import com.unrelentless.mess.util.serializeInnerStackToTag
import com.unrelentless.mess.util.setChunkLoaded
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
    }

    private val selectedTabs: HashMap<Level, Boolean> = hashMapOf(
            Pair(Level.LOW, true),
            Pair(Level.MID, true),
            Pair(Level.HIGH, true)
    )

    private var limbs: Array<LimbBlockEntity>? = null
        get() = field?.sortedBy{it.inventory.isEmpty}?.toTypedArray()

    override fun createMenu(
            syncId: Int,
            playerInventory: PlayerInventory,
            player: PlayerEntity
    ): ScreenHandler? = MessScreenHandler(
            syncId,
            playerInventory,
            limbs?.map(LimbBlockEntity::inventory)?.toTypedArray() ?: emptyArray(),
            this
    )

    override fun getDisplayName(): Text = TranslatableText("container." + Mess.IDENTIFIER + ".mess")
    override fun writeScreenOpeningData(serverPlayerEntity: ServerPlayerEntity?, packetByteBuf: PacketByteBuf?) {
        val sizes = limbs?.map{it.level.size}?.toIntArray()
        val compoundTag = CompoundTag()
        val listTag = ListTag()
        limbs?.map { it.inventory.getStack().serializeInnerStackToTag() }?.forEach{listTag.add(it)}
        compoundTag.put("items", listTag)

        packetByteBuf?.writeIntArray(sizes)
        packetByteBuf?.writeCompoundTag(compoundTag)

        selectedTabs.forEach {
            packetByteBuf?.writeEnumConstant(it.key)
            packetByteBuf?.writeBoolean(it.value)
        }
    }

    override fun fromTag(state: BlockState?, compoundTag: CompoundTag?) {
        super.fromTag(state, compoundTag)

        (compoundTag?.get("tabs") as? CompoundTag)?.let { tag ->
            selectedTabs.forEach {
                selectedTabs[it.key] = tag.getBoolean(it.key.name)
            }
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)

        val tabsTag = CompoundTag()

        selectedTabs.forEach {
            tabsTag.putBoolean(it.key.name, it.value)
        }

        tag.put("tabs", tabsTag)

        return tag
    }

    fun onPlaced() = findLimbs(world as World, pos).forEach{it.addBrain(this)}
    fun onBroken() = findLimbs(world as World, pos).forEach{it.removeBrain(this)}
    fun updateBrains() = limbs?.forEach(LimbBlockEntity::findBrains)
    fun updateLimbs(ignoringPos: BlockPos? = null) {
        limbs = findLimbs(world, pos, ignoringPos).toTypedArray()
    }

    fun updateTabs(selectedTabs:  HashMap<Level, Boolean>) {
        selectedTabs.forEach{
            this.selectedTabs[it.key] = it.value
        }
        markDirty()
    }

    fun chunkLoad(chunkLoad: Boolean) {
        setChunkLoaded(chunkLoad)
        limbs?.forEach{it.setChunkLoaded(chunkLoad)}
    }
}