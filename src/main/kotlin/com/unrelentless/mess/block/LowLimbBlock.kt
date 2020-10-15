package com.unrelentless.mess.block

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.settings.lowLimbBlockSettings
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.registerBlock
import com.unrelentless.mess.util.registerBlockItem
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.world.BlockView

class LowLimbBlock: LimbBlock(lowLimbBlockSettings, Level.LOW), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "low_limb")
        val BLOCK = registerBlock(LowLimbBlock(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.MISC))
    }

    override fun renderOnClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(BLOCK, RenderLayer.getCutout())
    }

}