package com.unrelentless.mess.client.gui.screen

import com.unrelentless.mess.Mess
import com.unrelentless.mess.util.LimbInventory
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Identifier
import kotlin.math.min


class MessScreenHandler(syncId: Int, playerInventory: PlayerInventory, val limbs: Array<LimbInventory>?) : ScreenHandler(HANDLER_TYPE, syncId) {

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
    override fun onSlotClick(index: Int, mouseButton: Int, actionType: SlotActionType, playerEntity: PlayerEntity): ItemStack {
        when(actionType) {
            SlotActionType.QUICK_MOVE -> return transferSlot(playerEntity, index)
            SlotActionType.PICKUP -> return pickup(index, mouseButton, playerEntity)
        }

        return super.onSlotClick(index, mouseButton, actionType, playerEntity)
    }

    override fun transferSlot(player: PlayerEntity, index: Int): ItemStack {
        val slot = this.slots[index]

        if(limbs == null) return ItemStack.EMPTY
        if(!slot.hasStack()) return ItemStack.EMPTY

        val slotStack = slot.stack
        val maxMessInvSize = min(limbs.size, 9*5)

        // Slot clicked in MESS inventory
        if(index < maxMessInvSize) {
            val count = min(slotStack.item.maxCount, slotStack.count)
            val itemStackCopy = slotStack.copy()
            itemStackCopy.count = count

            if(!insertItem(itemStackCopy, maxMessInvSize, this.slots.size, true))
                return ItemStack.EMPTY

            slotStack.decrement(count)
        } else {
            val limbSlots = slots.filterIndexed{index, _ ->  index < maxMessInvSize}
            val slotsWithItems = limbSlots.filter { ItemStack.areItemsEqual(slotStack, it.stack) }
            val iterator = slotsWithItems.iterator()

            // Fill in inventories that already have items
            while(iterator.hasNext() && !slotStack.isEmpty) {
                (iterator.next().inventory as LimbInventory).depositStack(slotStack)
            }

            // Fill empty slot with remainder
            if(!slotStack.isEmpty) {
                val emptySlot = limbSlots.find { !it.hasStack() }
                (emptySlot?.inventory as? LimbInventory)?.depositStack(slotStack)
            }
        }

        slot.markDirty()
        return slotStack
    }

    private fun createSlots(playerInventory: PlayerInventory) {
        val limbs = limbs ?: emptyArray()

        // Magic numbers
        val xOffset = 9
        val yOffsetInv = 18
        val yOffsetPlayerInv = 121
        val yOffsetPlayerHotbar = 179
        val maxColumns = 9
        val maxRows = 5
        val rowTotal = min(1 + limbs.size / 9, maxRows)

        // MESS inv
        for (row in 0 until rowTotal) {
            val columnMax = min(limbs.size - maxColumns * row, maxColumns)
            for (column in 0 until columnMax) {
                val index = column + row * 9
                addSlot(Slot(limbs[index], index, xOffset + column * 18, yOffsetInv + row * 18))
            }
        }

        // Player inv
        for(row in 0 until 3) {
            for (column in 0 until 9) {
                addSlot(Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        xOffset + column * 18,
                        yOffsetPlayerInv + row * 18)
                )
            }
        }

        // Player hotbar
        for(row in 0 until 9) {
            addSlot(Slot(playerInventory, row, xOffset + row * 18, yOffsetPlayerHotbar))
        }
    }

    private fun pickup(index: Int, mouseButton: Int, playerEntity: PlayerEntity): ItemStack {
        if(limbs == null) return ItemStack.EMPTY

        val slot = this.slots[index]
        var slotStack = slot.stack
        val count = min(slotStack.item.maxCount, slotStack.count)
        var cursorStack = playerEntity.inventory.cursorStack
        val maxMessInvSize = min(limbs.size, 9*5)

        if(index < maxMessInvSize) {
            // Deposit stack
            if(!cursorStack.isEmpty) {
                slotStack = ((slot.inventory) as LimbInventory).depositStack(cursorStack)
            } else {
                cursorStack = ((slot.inventory) as LimbInventory).withdrawStack(count)
                playerEntity.inventory.cursorStack = cursorStack
            }
            // Slot in player inventory
        } else {
            // Deposit stack
            if(!cursorStack.isEmpty) {
                if(slotStack.isEmpty || canStacksCombine(slotStack, cursorStack)) {
                    val itemStackCopy = slotStack.copy()
                    itemStackCopy.count = count
                    slotStack = itemStackCopy
                    cursorStack.decrement(count)
                }
            } else {
                val itemStackCopy = slotStack.copy()
                itemStackCopy.count = count
                cursorStack = itemStackCopy
                playerEntity.inventory.cursorStack = cursorStack
                slotStack.decrement(count)
            }
        }

        slot.markDirty()
        return cursorStack
    }
}