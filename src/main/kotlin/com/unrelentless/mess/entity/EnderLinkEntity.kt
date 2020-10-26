package com.unrelentless.mess.entity

import net.minecraft.entity.EyeOfEnderEntity
import net.minecraft.world.World

class EnderLinkEntity(world: World,
                      x: Double,
                      y: Double,
                      z: Double,
                      private val handler: OnDestroyHandler?
): EyeOfEnderEntity(world, x, y, z) {

    override fun remove() {
        handler?.handle()
        super.remove()
    }

    fun interface OnDestroyHandler {
        fun handle()
    }
}