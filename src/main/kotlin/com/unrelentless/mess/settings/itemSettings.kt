package com.unrelentless.mess.settings

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.Material
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup

val messItemSettings: FabricItemSettings =
        FabricItemSettings()
                .group(ItemGroup.MISC)
                .maxCount(1)
