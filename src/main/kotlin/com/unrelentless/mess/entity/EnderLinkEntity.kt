package com.unrelentless.mess.entity

import net.minecraft.entity.EyeOfEnderEntity
import net.minecraft.world.World

class EnderLinkEntity(world: World,
                      x: Double,
                      y: Double,
                      z: Double,
): EyeOfEnderEntity(world, x, y, z) {

    var handler: OnDestroyHandler? = null

    override fun remove() {
        handler?.handle()
        super.remove()
    }

    fun interface OnDestroyHandler {
        fun handle()
    }
}