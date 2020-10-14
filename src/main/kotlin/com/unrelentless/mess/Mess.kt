package com.unrelentless.mess

import com.unrelentless.mess.block.HighLimbBlock
import com.unrelentless.mess.block.LowLimbBlock
import com.unrelentless.mess.block.MidLimbBlock
import com.unrelentless.mess.util.Clientside
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.Item

class Mess : ModInitializer, ClientModInitializer {

    companion object {
        const val IDENTIFIER = "mess"

        val BLOCKS: List<Block> = listOf(
            LowLimbBlock.BLOCK,
            MidLimbBlock.BLOCK,
            HighLimbBlock.BLOCK
        )

        val ITEMS: List<Item> = listOf(
            LowLimbBlock.BLOCK_ITEM,
            MidLimbBlock.BLOCK_ITEM,
            HighLimbBlock.BLOCK_ITEM
        )
    }

    override fun onInitialize() {
    }

    @Environment(EnvType.CLIENT)
    override fun onInitializeClient() {
        BLOCKS.filterIsInstance<Clientside>().forEach{it.renderOnClient()}
    }
}

