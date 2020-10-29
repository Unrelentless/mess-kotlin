package com.unrelentless.mess.util

import com.unrelentless.mess.block.entity.BrainBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World

class BrainSyncManager(private val brain: BrainBlockEntity) {

    fun registerPlayerForSync(playerEntity: PlayerEntity) {

    }

    fun brainPlaced() {
        BrainBlockEntity.findLimbs(brain.world, brain.pos).forEach{it.addBrain(brain)}
    }

    fun brainBroken() {
        BrainBlockEntity.findLimbs(brain.world, brain.pos).forEach{ it.removeBrain(brain) }
    }

    fun limbsChanged() {

    }

    fun inventoryChangedByPlayer(playerEntity: PlayerEntity) {

    }
}