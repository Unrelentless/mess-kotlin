package com.unrelentless.mess.util

import com.unrelentless.mess.block.entity.HighLimbBlockEntity
import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.block.entity.LowLimbBlockEntity
import com.unrelentless.mess.block.entity.MidLimbBlockEntity

enum class Level {
    LOW, MID, HIGH;

    val blockEntity: LimbBlockEntity

    get() = when(this) {
            LOW -> LowLimbBlockEntity()
            MID -> MidLimbBlockEntity()
            HIGH -> HighLimbBlockEntity()
    }
}