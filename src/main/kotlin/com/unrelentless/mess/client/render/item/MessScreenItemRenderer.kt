package com.unrelentless.mess.client.render.item

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.item.ItemColors
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.item.BuiltinModelItemRenderer
import net.minecraft.client.render.item.ItemModels
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.BakedModelManager
import net.minecraft.client.texture.TextureManager
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World


class MessScreenItemRenderer(
    manager: TextureManager?,
    bakery: BakedModelManager?,
    colorMap: ItemColors?,
    builtinModelItemRenderer: BuiltinModelItemRenderer?
) : ItemRenderer(manager, bakery, colorMap, builtinModelItemRenderer) {

    override fun getModels(): ItemModels = MinecraftClient.getInstance().itemRenderer.models
    override fun getHeldItemModel(stack: ItemStack?, world: World?, entity: LivingEntity?, seed: Int): BakedModel {
        val bakedModel3: BakedModel = when {
            stack!!.isOf(Items.TRIDENT) ->  models.modelManager.getModel(ModelIdentifier("minecraft:trident_in_hand#inventory"))
            stack.isOf(Items.SPYGLASS) -> models.modelManager.getModel(ModelIdentifier("minecraft:spyglass_in_hand#inventory"))
            else ->  models.getModel(stack)
        }

        val clientWorld = if (world is ClientWorld) world else null
        val bakedModel4 = bakedModel3.overrides.apply(bakedModel3, stack, clientWorld, entity, seed)
        return bakedModel4 ?: models.modelManager.missingModel
    }

    override fun renderGuiItemOverlay(renderer: TextRenderer, stack: ItemStack, x: Int, y: Int, countLabel: String?) {
        if(stack.isEmpty) return

        val newCountLabel = if (stack.count >= 1000) {
            (stack.count / 1000).toString() + "." + (stack.count % 1000 / 100).toString() + "k"
        } else {
            stack.count.toString()
        }

        if (stack.count != 1 || countLabel != null) {
            val matrixStack = MatrixStack()
            val scaleMultiplier = 0.8f
            val xTranslate = if (stack.count < 10) 2 else if (stack.count < 100) 1 else 0

            matrixStack.scale(scaleMultiplier, scaleMultiplier, 1.0f)
            matrixStack.translate(0.0, 0.0, (zOffset + 200.0f).toDouble())

            val immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().buffer)
            val xPos = ((x + 19 - xTranslate - renderer.getWidth(newCountLabel)) / scaleMultiplier)
            val yPos = ((y + 6 + 3) / scaleMultiplier)
            renderer.draw(
                    newCountLabel,
                    xPos,
                    yPos,
                    16777215,
                    true,
                    matrixStack.peek().model,
                    immediate,
                    false,
                    0,
                    15728880
            )
            immediate.draw()
        }

        // COPY-PASTA
        if (stack.isItemBarVisible) {
            RenderSystem.disableDepthTest()
            RenderSystem.disableTexture()
            RenderSystem.disableBlend()
            val tessellator = Tessellator.getInstance()
            val bufferBuilder = tessellator.buffer
            val i = stack.itemBarStep
            val j = stack.itemBarColor
            renderGuiQuad(bufferBuilder, x + 2, y + 13, 13, 2, 0, 0, 0, 255)
            renderGuiQuad(bufferBuilder, x + 2, y + 13, i, 1, j shr 16 and 255, j shr 8 and 255, j and 255, 255)
            RenderSystem.enableBlend()
            RenderSystem.enableTexture()
            RenderSystem.enableDepthTest()
        }

        val clientPlayerEntity = MinecraftClient.getInstance().player
        val k = clientPlayerEntity?.itemCooldownManager?.getCooldownProgress(
                stack.item,
                MinecraftClient.getInstance().tickDelta
        ) ?: 0.0f

        if (k > 0.0f) {
            RenderSystem.disableDepthTest()
            RenderSystem.disableTexture()
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            val tessellator2 = Tessellator.getInstance()
            val bufferBuilder2 = tessellator2.buffer
            renderGuiQuad(
                    bufferBuilder2,
                    x,
                    y + MathHelper.floor(16.0f * (1.0f - k)),
                    16,
                    MathHelper.ceil(16.0f * k),
                    255,
                    255,
                    255,
                    127
            )
            RenderSystem.enableTexture()
            RenderSystem.enableDepthTest()
        }
    }

    // COPY-PASTA
    private fun renderGuiQuad(buffer: BufferBuilder,
                              x: Int,
                              y: Int,
                              width: Int,
                              height: Int,
                              red: Int,
                              green: Int,
                              blue: Int,
                              alpha: Int
    ) {
        RenderSystem.setShader { GameRenderer.getPositionColorShader() }
        buffer.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        buffer.vertex((x + 0).toDouble(), (y + 0).toDouble(), 0.0).color(red, green, blue, alpha).next()
        buffer.vertex((x + 0).toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).next()
        buffer.vertex((x + width).toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).next()
        buffer.vertex((x + width).toDouble(), (y + 0).toDouble(), 0.0).color(red, green, blue, alpha).next()
        buffer.end()
        BufferRenderer.draw(buffer)
    }
}