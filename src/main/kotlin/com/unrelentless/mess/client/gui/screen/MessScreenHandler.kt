package com.unrelentless.mess.client.gui.screen

import com.unrelentless.mess.Mess
import com.unrelentless.mess.util.LimbInventory
import com.unrelentless.mess.util.LimbSlot
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
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


class MessScreenHandler(syncId: Int, private val playerInventory: PlayerInventory, val limbs: Array<LimbInventory>) : ScreenHandler(HANDLER_TYPE, syncId) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "mess_screen_handler")
        val HANDLER_TYPE: ScreenHandlerType<MessScreenHandler> = ScreenHandlerRegistry.registerExtended(IDENTIFIER, ::MessScreenHandler)
        val C2S_IDENTIFIER = Identifier(Mess.IDENTIFIER, "sync_inv")

        init {
            ServerSidePacketRegistry.INSTANCE.register(C2S_IDENTIFIER) { context, buffer ->
                val scrollPosition = buffer.readFloat()
                context.taskQueue.execute {
                    (context.player.currentScreenHandler as? MessScreenHandler)?.scrollPosition = scrollPosition
                }
            }
        }
    }

    constructor(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): this(
            syncId,
            playerInventory,
            buf.readIntArray().map { LimbInventory(it, null) }.toTypedArray()
    )

    init { createNewSlots() }

    var scrolledRows = 0
    var scrollPosition = 0.0f
        set(newValue) {
            field = newValue
            sendNewPositionToServer()
            calculateScrolledRows()
            createNewSlots()
        }

    val limbsToDisplay: Array<LimbInventory>
        get() = this.limbs.filterIndexed { index, _ ->
            val min = scrolledRows * MessScreen.COLUMNS
            val max = min(this.limbs.size, MessScreen.INV_SIZE + min)
            (min until max).contains(index)
        }.toTypedArray()

    override fun canUse(player: PlayerEntity?): Boolean = true

    override fun onSlotClick(index: Int, mouseButton: Int, actionType: SlotActionType, playerEntity: PlayerEntity): ItemStack {
        if(index == -1) return ItemStack.EMPTY

        when(actionType) {
            SlotActionType.QUICK_MOVE -> return transferSlot(playerEntity, index)
            SlotActionType.PICKUP -> return pickup(index, mouseButton, playerEntity)
            else -> super.onSlotClick(index, mouseButton, actionType, playerEntity)
            // TODO: Implement custom pickupAll or quickCraft to prevent limbs being deposited. Test to see which.
//            SlotActionType.PICKUP_ALL -> return pickupAll(index, mouseButton, playerEntity)
//            SlotActionType.QUICK_CRAFT -> return quickCraft(index, mouseButton, playerEntity)
        }
        return super.onSlotClick(index, mouseButton, actionType, playerEntity)
    }

    override fun transferSlot(player: PlayerEntity, index: Int): ItemStack {
        val slot = this.slots[index]
        val limbs = limbsToDisplay

        if(!slot.hasStack()) return ItemStack.EMPTY

        val slotStack = slot.stack

        // Slot clicked in MESS inventory
        if(index < limbs.size) {
            val count = min(slotStack.item.maxCount, slotStack.count)
            val itemStackCopy = slotStack.copy()
            itemStackCopy.count = count

            if(!insertItem(itemStackCopy, limbs.size, this.slots.size, true))
                return ItemStack.EMPTY

            slotStack.decrement(count)
        } else {
            val limbSlots = slots.filterIndexed{ _,_ ->  index < limbs.size}
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

    private fun createNewSlots() {
        slots.clear()

        val limbs = limbsToDisplay

        // Magic numbers
        val xOffset = 9
        val yOffsetInv = 18
        val yOffsetPlayerInv = 121
        val yOffsetPlayerHotbar = 179
        val rowTotal = min(1 + limbs.size / MessScreen.COLUMNS, MessScreen.ROWS)

        // MESS inv
        for (row in 0 until rowTotal) {
            val columnMax = min(limbs.size - MessScreen.COLUMNS * row, MessScreen.COLUMNS)

            for (column in 0 until columnMax) {
                val index = column + row * MessScreen.COLUMNS
                addSlot(LimbSlot(limbs[index], index, xOffset + column * 18, yOffsetInv + row * 18))
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
        if (index == -999) {
            val stack = if(mouseButton == 0) playerInventory.cursorStack else playerInventory.cursorStack.split(1)
            playerEntity.dropItem(stack, true)
            return ItemStack.EMPTY
        }

        val slot = this.slots[index]
        val slotStack = slot.stack.copy()
        val cursorStack = playerEntity.inventory.cursorStack.copy()

        if(index < limbsToDisplay.size) {
            if(!cursorStack.isEmpty) {
               ((slot.inventory) as LimbInventory).depositStack(cursorStack)
            } else {
                val count = min(slotStack.item.maxCount, slotStack.count)
                playerEntity.inventory.cursorStack = ((slot.inventory) as LimbInventory).withdrawStack(count)
            }
        } else {
            if(canStacksCombine(slotStack, cursorStack)) {
                val itemStackLarge = if(cursorStack.count > slotStack.count) cursorStack else slotStack
                val itemStackSmall = if(cursorStack.count < slotStack.count) cursorStack else slotStack

                itemStackSmall.increment(itemStackLarge.split(itemStackLarge.count - itemStackSmall.count).count)
            } else if(cursorStack.isEmpty || slotStack.isEmpty) {
                var emptyItemStack = if(cursorStack.isEmpty) cursorStack else slotStack
                val fullItemStack = if(!cursorStack.isEmpty) cursorStack else slotStack

                emptyItemStack = fullItemStack.split(fullItemStack.count)
            } else {
                playerInventory.cursorStack = slotStack
                slot.stack = cursorStack
            }
        }

        return ItemStack.EMPTY
    }

    private fun calculateScrolledRows() {
        val numberOfPositions = (this.limbs.size + 9 - 1) / 9 - 5
        scrolledRows = (numberOfPositions * scrollPosition + 0.5).toInt()
    }

    private fun sendNewPositionToServer() {
        if(playerInventory.player.world.isClient) {
            val buffer = PacketByteBuf(Unpooled.buffer())
            buffer.writeFloat(scrollPosition)

            ClientSidePacketRegistry.INSTANCE.sendToServer(C2S_IDENTIFIER, buffer)
        }
    }
}