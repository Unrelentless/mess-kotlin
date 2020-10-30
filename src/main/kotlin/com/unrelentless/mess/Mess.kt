package com.unrelentless.mess

import com.unrelentless.mess.block.BrainBlock
import com.unrelentless.mess.block.HighLimbBlock
import com.unrelentless.mess.block.LowLimbBlock
import com.unrelentless.mess.block.MidLimbBlock
import com.unrelentless.mess.block.entity.HighLimbBlockEntity
import com.unrelentless.mess.block.entity.LowLimbBlockEntity
import com.unrelentless.mess.block.entity.MidLimbBlockEntity
import com.unrelentless.mess.client.gui.screen.MessScreen
import com.unrelentless.mess.client.gui.screen.MessScreenHandler
import com.unrelentless.mess.item.EnderLinkItem
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.LimbInventory
import com.unrelentless.mess.util.deserializeInnerStack
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.util.Identifier

class Mess : ModInitializer, ClientModInitializer {

    companion object {
        const val IDENTIFIER = "mess"
        val C2S_IDENTIFIER = Identifier(IDENTIFIER, "sync_server")
        val S2C_IDENTIFIER = Identifier(IDENTIFIER, "sync_client")

        val BLOCKS: List<Block> = listOf(
                LowLimbBlock.BLOCK,
                MidLimbBlock.BLOCK,
                HighLimbBlock.BLOCK,
                BrainBlock.BLOCK
        )

        val ITEMS: List<Item> = listOf(
                LowLimbBlock.BLOCK_ITEM,
                MidLimbBlock.BLOCK_ITEM,
                HighLimbBlock.BLOCK_ITEM,
                BrainBlock.BLOCK_ITEM,
                EnderLinkItem.ITEM
        )

        val ENTITIES: List<Clientside> = listOf(
                LowLimbBlockEntity.Companion,
                MidLimbBlockEntity.Companion,
                HighLimbBlockEntity.Companion
        )

        val SCREENS: List<Clientside> by lazy {
            listOf(MessScreen.Companion)
        }
    }

    override fun onInitialize() {
        ServerSidePacketRegistry.INSTANCE.register(C2S_IDENTIFIER) { context, buffer ->
            val scrollPosition = buffer.readFloat()
            val searchString = buffer.readString(Short.MAX_VALUE.toInt())
            val tabs: Map<Level, Boolean> = Level.values().map {
                Pair(buffer.readEnumConstant(Level::class.java), buffer.readBoolean())
            }.toMap()


            context.taskQueue.execute {
                (context.player.currentScreenHandler as? MessScreenHandler).let {
                    for(tab in tabs) {
                        it?.selectedTabs?.set(tab.key, tab.value)
                    }
                    it?.updateInfo(searchString, scrollPosition)
                    it?.owner?.updateTabs(it.selectedTabs)
                }
            }
        }
    }

    override fun onInitializeClient() {
        listOf(BLOCKS, ITEMS, ENTITIES, SCREENS)
                .flatten()
                .filterIsInstance<Clientside>()
                .forEach{it.renderOnClient()}

        ClientSidePacketRegistry.INSTANCE.register(S2C_IDENTIFIER) { context, buf ->
            val inventories = buf.readIntArray().map { int ->
                LimbInventory(Level.values().find { it.size == int }!!, null)
            }.toTypedArray()

            (buf.readCompoundTag()?.get("items") as ListTag)
                    .mapNotNull { (it as CompoundTag).deserializeInnerStack() }
                    .forEachIndexed { index, item ->
                        inventories[index].depositStack(item)
                    }

            context.taskQueue.execute {
                (context.player.currentScreenHandler as? MessScreenHandler).let { handler ->
                    handler?.updateClientLimbs(inventories)
                }
            }

        }
    }
}

