package com.unrelentless.mess.settings

import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ItemGroup

val messBlockItemSettings: FabricItemSettings =
        FabricItemSettings()
                .group(ItemGroup.MISC)

val enderLinkItemSettings: FabricItemSettings = FabricItemSettings()
