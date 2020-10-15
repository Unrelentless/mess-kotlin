package com.unrelentless.mess.block

import com.unrelentless.mess.util.Level
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.world.BlockView

open class LimbBlock(settings: FabricBlockSettings, level: Level): BlockWithEntity(settings.nonOpaque()) {
    private val level: Level = level;

    override fun createBlockEntity(world: BlockView?): BlockEntity? = level.blockEntity
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL
}