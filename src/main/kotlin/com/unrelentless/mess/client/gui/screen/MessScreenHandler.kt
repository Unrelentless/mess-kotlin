package com.unrelentless.mess.client.gui.screen

import com.unrelentless.mess.Mess
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier


class MessScreenHandler(syncId: Int, playerInventory: PlayerInventory) : ScreenHandler(HANDLER_TYPE, syncId) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "mess_screen_handler")
        val HANDLER_TYPE: ScreenHandlerType<MessScreenHandler> = ScreenHandlerRegistry.registerSimple(IDENTIFIER, ::MessScreenHandler)
    }

    private val inventory: Inventory? = null

    override fun canUse(player: PlayerEntity?): Boolean = true
}