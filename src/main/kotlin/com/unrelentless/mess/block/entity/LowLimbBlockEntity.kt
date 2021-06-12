package com.unrelentless.mess.block.entity

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.LowLimbBlock
import com.unrelentless.mess.client.render.block.entity.LimbEntityRenderer
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.registerBlockEntity
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class LowLimbBlockEntity(pos: BlockPos?, state: BlockState?) : LimbBlockEntity(ENTITY_TYPE, Level.LOW, pos, state) {

    companion object: Clientside {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "low_limb_entity")
        val ENTITY_TYPE = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                    .create({pos, state -> LowLimbBlockEntity(pos, state) }, LowLimbBlock.BLOCK)
                    .build(null)
        }

        override fun renderOnClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { LimbEntityRenderer<LowLimbBlockEntity>() }
        }
    }
}