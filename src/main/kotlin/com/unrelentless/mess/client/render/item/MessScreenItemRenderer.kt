package com.unrelentless.mess.client.render.item

import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.item.ItemColors
import net.minecraft.client.render.item.ItemModels
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.BakedModelManager
import net.minecraft.client.texture.TextureManager
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class MessScreenItemRenderer(
        manager: TextureManager?,
        bakery: BakedModelManager?,
        colorMap: ItemColors?
) : ItemRenderer(manager, bakery, colorMap) {

    override fun getModels(): ItemModels = MinecraftClient.getInstance().itemRenderer.models;

}