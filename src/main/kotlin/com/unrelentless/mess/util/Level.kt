package com.unrelentless.mess.util

import com.unrelentless.mess.block.HighLimbBlock
import com.unrelentless.mess.block.LowLimbBlock
import com.unrelentless.mess.block.MidLimbBlock
import com.unrelentless.mess.block.entity.HighLimbBlockEntity
import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.block.entity.LowLimbBlockEntity
import com.unrelentless.mess.block.entity.MidLimbBlockEntity
import net.minecraft.block.Block

enum class Level {
    LOW, MID, HIGH;

    val blockEntity: LimbBlockEntity
    get() = when(this) {
            LOW -> LowLimbBlockEntity()
            MID -> MidLimbBlockEntity()
            HIGH -> HighLimbBlockEntity()
    }

    val block: Block
        get() = when(this) {
            LOW -> LowLimbBlock.BLOCK
            MID -> MidLimbBlock.BLOCK
            HIGH -> HighLimbBlock.BLOCK
        }
}