package com.unrelentless.mess.block

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.entity.HeartBlockEntity
import com.unrelentless.mess.block.settings.heartBlockSettings
import com.unrelentless.mess.util.registerBlock
import com.unrelentless.mess.util.registerBlockItem
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.world.BlockView

class HeartBlock: BlockWithEntity(heartBlockSettings) {
    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "heart")
        val BLOCK = registerBlock(HeartBlock(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.MISC))
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? = HeartBlockEntity()
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL
}