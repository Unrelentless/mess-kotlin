package com.unrelentless.mess.entity

import com.unrelentless.mess.mixin.EyeOfEnderEntityAccessor
import net.minecraft.entity.EyeOfEnderEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class EnderLinkEntity(world: World,
                      x: Double,
                      y: Double,
                      z: Double,
                      private val handler: OnDestroyHandler?
): EyeOfEnderEntity(world, x, y, z) {

    companion object {
        const val MAX_TICKS = 18
        const val MAX_LIFESPAN = 78
    }

    private var ticks = 0
    private val shouldActuallyRemove
        get() = ticks >= MAX_TICKS

    override fun remove() {
        handler?.execute()
        super.remove()
    }

    override fun tick() {
        if((this as EyeOfEnderEntityAccessor).lifeSpan > MAX_LIFESPAN && !world.isClient) {
            if(ticks == 0) triggerWorldEvent()
            if(shouldActuallyRemove) return remove()
            ++ticks
        } else super.tick()
    }

    private fun triggerWorldEvent() {
        world.syncWorldEvent(2003, BlockPos(x, y, z), 0)
        setPos(x, 0.0, z)
    }

    fun interface OnDestroyHandler {
        fun execute()
    }
}