package com.unrelentless.mess.client.render.item

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.item.ItemColors
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.VertexFormats
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
import kotlin.math.max
import kotlin.math.round


class MessScreenItemRenderer(
        manager: TextureManager?,
        bakery: BakedModelManager?,
        colorMap: ItemColors?
) : ItemRenderer(manager, bakery, colorMap) {

    override fun getModels(): ItemModels = MinecraftClient.getInstance().itemRenderer.models

    override fun getHeldItemModel(stack: ItemStack, world: World?, entity: LivingEntity?): BakedModel? {
        val item = stack.item
        val bakedModel = if (item === Items.TRIDENT)
            models.modelManager.getModel(ModelIdentifier("minecraft:trident_in_hand#inventory"))
        else
            models.getModel(stack)

        val clientWorld = if (world is ClientWorld) world else null
        val bakedModel3 = bakedModel.overrides.apply(bakedModel, stack, clientWorld, entity)
        return bakedModel3 ?: models.modelManager.missingModel
    }

    override fun renderGuiItemOverlay(renderer: TextRenderer, stack: ItemStack, x: Int, y: Int, countLabel: String?) {
        if(stack.isEmpty) return

        val newCountLabel = if (stack.count > 1000)
            (stack.count / 1000).toString() + "." + (stack.count % 1000 / 100).toString() + "k"
        else
            stack.count.toString()

        if (stack.count != 1 || countLabel != null) {
            val matrixStack = MatrixStack()
            val scaleMultiplier = if (stack.count < 100) 1.0f else 0.8f
            val xTranslate = if (stack.count < 100) 2 else 0

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

        if (stack.isDamaged) {
            RenderSystem.disableDepthTest()
            RenderSystem.disableTexture()
            RenderSystem.disableAlphaTest()
            RenderSystem.disableBlend()
            val tessellator = Tessellator.getInstance()
            val bufferBuilder = tessellator.buffer
            val f = stack.damage.toFloat()
            val g = stack.maxDamage.toFloat()
            val h = max(0.0f, (g - f) / g)
            val i = round(13.0f - f * 13.0f / g).toInt()
            val j = MathHelper.hsvToRgb(h / 3.0f, 1.0f, 1.0f)
            renderGuiQuad(bufferBuilder, x + 2, y + 13, 13, 2, 0, 0, 0, 255)
            renderGuiQuad(bufferBuilder, x + 2, y + 13, i, 1, j shr 16 and 255, j shr 8 and 255, j and 255, 255)
            RenderSystem.enableBlend()
            RenderSystem.enableAlphaTest()
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

    private fun renderGuiQuad(buffer: BufferBuilder, x: Int, y: Int, width: Int, height: Int, red: Int, green: Int, blue: Int,
                              alpha: Int) {
        buffer.begin(7, VertexFormats.POSITION_COLOR)
        buffer.vertex((x + 0).toDouble(), (y + 0).toDouble(), 0.0).color(red, green, blue, alpha).next()
        buffer.vertex((x + 0).toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).next()
        buffer.vertex((x + width).toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).next()
        buffer.vertex((x + width).toDouble(), (y + 0).toDouble(), 0.0).color(red, green, blue, alpha).next()
        Tessellator.getInstance().draw()
    }
}