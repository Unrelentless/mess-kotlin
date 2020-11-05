package com.unrelentless.mess.block

import com.unrelentless.mess.Mess
import com.unrelentless.mess.settings.messBlockItemSettings
import com.unrelentless.mess.settings.midLimbBlockSettings
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.registerBlock
import com.unrelentless.mess.util.registerBlockItem
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class MidLimbBlock: LimbBlock(midLimbBlockSettings, Level.MID), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "mid_limb")
        val BLOCK = registerBlock(MidLimbBlock(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, messBlockItemSettings)
    }

    override fun renderOnClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(BLOCK, RenderLayer.getCutout())
    }
}