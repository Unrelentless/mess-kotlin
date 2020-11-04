package com.unrelentless.mess.util

import net.minecraft.block.entity.BlockEntity
import net.minecraft.server.world.ServerWorld

fun BlockEntity.setChunkLoaded(chunkLoaded: Boolean) {
    val world = world as? ServerWorld ?: return
    val chunk = world.getWorldChunk(pos) ?: return

    world.setChunkForced(chunk.pos.x, chunk.pos.z, chunkLoaded)
}
