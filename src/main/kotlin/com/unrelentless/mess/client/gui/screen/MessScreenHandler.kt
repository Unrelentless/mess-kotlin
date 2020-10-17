package com.unrelentless.mess.client.gui.screen

import com.unrelentless.mess.Mess
import com.unrelentless.mess.util.LimbInventory
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import kotlin.math.min


class MessScreenHandler(syncId: Int, playerInventory: PlayerInventory, private val limbs: Array<LimbInventory>?) : ScreenHandler(HANDLER_TYPE, syncId) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "mess_screen_handler")
        val HANDLER_TYPE: ScreenHandlerType<MessScreenHandler> = ScreenHandlerRegistry.registerExtended(IDENTIFIER, ::MessScreenHandler)
    }

    constructor(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): this(
            syncId,
            playerInventory,
            buf.readIntArray().map { LimbInventory(it, null) }.toTypedArray()
    )

    init {
        createSlots(playerInventory)
    }

    override fun canUse(player: PlayerEntity?): Boolean = true

    private fun createSlots(playerInventory: PlayerInventory) {
        val limbs = limbs ?: emptyArray()

        var column: Int
        val xOffset = 9
        val yOffsetInv = 18
        val yOffsetPlayerInv = 121
        val yOffsetPlayerHotbar = 179
        val maxColumns = 9
        val maxRows = 3
        val rowTotal = min(1 + limbs.size / 9, maxRows)
        var row = 0

        while (row < rowTotal) {
            val columnMax = min(limbs.size - maxColumns * row, maxColumns)
            column = 0
            while (column < columnMax) {
                val index = column + row * 9
                addSlot(Slot(limbs[index], index, xOffset + column * 18, yOffsetInv + row * 18))
                ++column
            }
            ++row
        }


        row = 0

        while (row < 3) {
            column = 0
            while (column < 9) {
                addSlot(Slot(playerInventory, column + row * 9 + 9, xOffset + column * 18,
                        yOffsetPlayerInv + row * 18))
                ++column
            }
            ++row
        }

        row = 0

        while (row < 9) {
            addSlot(Slot(playerInventory, row, xOffset + row * 18, yOffsetPlayerHotbar))
            ++row
        }
    }
}