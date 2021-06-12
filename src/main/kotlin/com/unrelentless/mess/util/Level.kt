package com.unrelentless.mess.util

import com.unrelentless.mess.block.HighLimbBlock
import com.unrelentless.mess.block.LowLimbBlock
import com.unrelentless.mess.block.MidLimbBlock
import com.unrelentless.mess.block.entity.HighLimbBlockEntity
import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.block.entity.LowLimbBlockEntity
import com.unrelentless.mess.block.entity.MidLimbBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

enum class Level {
    LOW, MID, HIGH;

    val size: Int
        get() = when(this) {
            LOW -> 32
            MID -> 128
            HIGH -> 4096
        }

    fun blockEntity(pos: BlockPos?, state: BlockState?): LimbBlockEntity {
        return when(this) {
            LOW -> LowLimbBlockEntity(pos, state)
            MID -> MidLimbBlockEntity(pos, state)
            HIGH -> HighLimbBlockEntity(pos, state)
        }
    }


    val block: Block
        get() = when(this) {
            LOW -> LowLimbBlock.BLOCK
            MID -> MidLimbBlock.BLOCK
            HIGH -> HighLimbBlock.BLOCK
        }
}