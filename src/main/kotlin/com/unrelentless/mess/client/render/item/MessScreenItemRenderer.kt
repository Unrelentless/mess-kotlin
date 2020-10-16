package com.unrelentless.mess.client.render.item

import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.item.ItemColors
import net.minecraft.client.render.item.ItemModels
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.BakedModelManager
import net.minecraft.client.texture.TextureManager
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.world.World


class MessScreenItemRenderer(
        manager: TextureManager?,
        bakery: BakedModelManager?,
        colorMap: ItemColors?
) : ItemRenderer(manager, bakery, colorMap) {

    override fun getModels(): ItemModels = MinecraftClient.getInstance().itemRenderer.models

    override fun getHeldItemModel(stack: ItemStack, world: World?, entity: LivingEntity?): BakedModel? {
        val item = stack.item
        val bakedModel2: BakedModel
        bakedModel2 = if (item === Items.TRIDENT) {
            models.modelManager
                    .getModel(ModelIdentifier("minecraft:trident_in_hand#inventory"))
        } else {
            models.getModel(stack)
        }
        val clientWorld = if (world is ClientWorld) world else null
        val bakedModel3 = bakedModel2.overrides.apply(bakedModel2, stack, clientWorld, entity)
        return bakedModel3 ?: models.modelManager.missingModel
    }
}