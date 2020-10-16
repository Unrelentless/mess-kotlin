package com.unrelentless.mess.block.settings

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Material

val lowLimbBlockSettings: FabricBlockSettings =
    FabricBlockSettings.of(Material.WOOD)
            .strength(2.0F, 5.0F)

val midLimbBlockSettings: FabricBlockSettings =
    FabricBlockSettings.of(Material.METAL)
            .strength(2.0F, 60.0F)

val highLimbBlockSettings: FabricBlockSettings =
    FabricBlockSettings.of(Material.METAL)
            .strength(2.0F, 1200.0F)

val heartBlockSettings: FabricBlockSettings =
        FabricBlockSettings.of(Material.METAL)
                .strength(4.0F, 1200.0F)