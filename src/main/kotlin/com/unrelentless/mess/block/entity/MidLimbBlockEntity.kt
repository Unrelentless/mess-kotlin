package com.unrelentless.mess.block.entity

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.MidLimbBlock
import com.unrelentless.mess.client.render.block.entity.LimbEntityRenderer
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.registerBlockEntity
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier

class MidLimbBlockEntity: LimbBlockEntity(ENTITY_TYPE, Level.MID) {

    companion object: Clientside {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "mid_limb_entity")
        val ENTITY_TYPE = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                    .create({ MidLimbBlockEntity() }, MidLimbBlock.BLOCK)
                    .build(null)
        }

        override fun renderOnClient() {
            BlockEntityRendererRegistry.INSTANCE.register(ENTITY_TYPE) { LimbEntityRenderer<MidLimbBlockEntity>(it) }
        }
    }
}