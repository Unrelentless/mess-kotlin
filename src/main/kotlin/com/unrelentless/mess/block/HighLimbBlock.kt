package com.unrelentless.mess.block

import com.unrelentless.mess.Mess
import com.unrelentless.mess.settings.highLimbBlockSettings
import com.unrelentless.mess.settings.messBlockItemSettings
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.extensions.registerBlock
import com.unrelentless.mess.extensions.registerBlockItem
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class HighLimbBlock: LimbBlock(highLimbBlockSettings, Level.HIGH), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "high_limb")
        val BLOCK = registerBlock(HighLimbBlock(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, messBlockItemSettings.fireproof())
    }

    override fun renderOnClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(BLOCK, RenderLayer.getCutout())
    }
}