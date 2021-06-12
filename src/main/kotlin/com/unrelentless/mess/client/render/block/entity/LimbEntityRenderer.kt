package com.unrelentless.mess.client.render.block.entity

import com.unrelentless.mess.block.entity.LimbBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.math.MatrixStack
import kotlin.math.sin
import kotlin.random.Random


class LimbEntityRenderer<T: LimbBlockEntity>: BlockEntityRenderer<T> {

    private val randomOffset = Random.nextDouble(100.0, 200.0)

    override fun render(
            blockEntity: T,
            tickDelta: Float,
            matrices: MatrixStack,
            vertexConsumers: VertexConsumerProvider,
            light: Int,
            overlay: Int
    ) {
        val offset = sin((blockEntity.world!!.time + randomOffset + tickDelta) / 8.0) / 8.0
        val lightAbove = WorldRenderer.getLightmapCoordinates(blockEntity.world, blockEntity.pos.up())

        matrices.push()
        matrices.translate(0.5, 0.3 + offset, 0.5)
//        matrices.multiply(Vector3d.POSITIVE_Y.getDegreesQuaternion((blockEntity.world!!.time + tickDelta) * 4))

        MinecraftClient.getInstance().itemRenderer.renderItem(
                blockEntity.inventory.getStack(0),
                ModelTransformation.Mode.GROUND,
                lightAbove,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            0
        )

        matrices.pop()
    }
}