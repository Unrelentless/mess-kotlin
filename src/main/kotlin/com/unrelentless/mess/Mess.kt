package com.unrelentless.mess

import com.unrelentless.mess.block.HeartBlock
import com.unrelentless.mess.block.HighLimbBlock
import com.unrelentless.mess.block.LowLimbBlock
import com.unrelentless.mess.block.MidLimbBlock
import com.unrelentless.mess.block.entity.HighLimbBlockEntity
import com.unrelentless.mess.block.entity.LowLimbBlockEntity
import com.unrelentless.mess.block.entity.MidLimbBlockEntity
import com.unrelentless.mess.client.gui.screen.MessScreen
import com.unrelentless.mess.item.PortableMessItem
import com.unrelentless.mess.util.Clientside
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.minecraft.block.Block
import net.minecraft.item.Item

class Mess : ModInitializer, ClientModInitializer {

    companion object {
        const val IDENTIFIER = "mess"

        val BLOCKS: List<Block> = listOf(
                LowLimbBlock.BLOCK,
                MidLimbBlock.BLOCK,
                HighLimbBlock.BLOCK,
                HeartBlock.BLOCK
        )

        val ITEMS: List<Item> = listOf(
                LowLimbBlock.BLOCK_ITEM,
                MidLimbBlock.BLOCK_ITEM,
                HighLimbBlock.BLOCK_ITEM,
                HeartBlock.BLOCK_ITEM,
                PortableMessItem.ITEM
        )

        val ENTITIES: List<Clientside> = listOf(
                LowLimbBlockEntity.Companion,
                MidLimbBlockEntity.Companion,
                HighLimbBlockEntity.Companion
        )

        val SCREENS: List<Clientside> = listOf(
                MessScreen.Companion
        )
    }

    override fun onInitialize() {}

    @Environment(EnvType.CLIENT)
    override fun onInitializeClient() {
        listOf(BLOCKS, ITEMS, ENTITIES, SCREENS)
                .flatten()
                .filterIsInstance<Clientside>()
                .forEach{it.renderOnClient()}
    }
}

