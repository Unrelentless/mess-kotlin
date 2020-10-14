package com.unrelentless.mess

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer

class Mess : ModInitializer, ClientModInitializer {
    override fun onInitialize() {
    }

    @Environment(EnvType.CLIENT)
    override fun onInitializeClient() {
    }

}

