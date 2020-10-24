package com.unrelentless.mess.util

import net.minecraft.block.entity.BlockEntity
import net.minecraft.server.world.ServerWorld

fun BlockEntity.setChunkLoaded(chunkLoaded: Boolean) {
    (world as? ServerWorld)?.let { world ->
        world.getWorldChunk(pos)?.let { chunk ->
            world.setChunkForced(chunk.pos.x, chunk.pos.z, chunkLoaded)
        }
    }
}
