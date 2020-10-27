package com.unrelentless.mess.util

import net.minecraft.server.world.ServerWorld
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.World


fun ServerWorld.fromString(string: String): World? {
    val registryKey: RegistryKey<World> =
        when(string) {
            World.OVERWORLD.value.path -> World.OVERWORLD
            World.NETHER.value.path -> World.NETHER
            World.END.value.path -> World.END
            else -> return null
        }

    return this.server.getWorld(registryKey)
}
