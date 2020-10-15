package com.unrelentless.mess.block.settings

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Material

val lowLimbBlockSettings: FabricBlockSettings =
    FabricBlockSettings.of(Material.WOOD)

val midLimbBlockSettings: FabricBlockSettings =
    FabricBlockSettings.of(Material.STONE)

val highLimbBlockSettings: FabricBlockSettings =
    FabricBlockSettings.of(Material.METAL)
