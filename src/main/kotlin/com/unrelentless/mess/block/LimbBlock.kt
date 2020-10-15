package com.unrelentless.mess.block

import com.unrelentless.mess.util.Level
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.world.BlockView

open class LimbBlock(settings: FabricBlockSettings, level: Level): Block(settings.nonOpaque()) {
    private val level: Level = level;

//    override fun createBlockEntity(world: BlockView?): BlockEntity? = level.blockEntity()
}