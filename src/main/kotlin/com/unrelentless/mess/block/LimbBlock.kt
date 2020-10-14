package com.unrelentless.mess.block

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer

open class LimbBlock: Block {
    constructor(settings: FabricBlockSettings): super(settings)
}