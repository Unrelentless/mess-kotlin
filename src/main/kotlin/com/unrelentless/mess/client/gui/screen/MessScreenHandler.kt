package com.unrelentless.mess.client.gui.screen

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.entity.BrainBlockEntity
import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.util.LimbInventory
import com.unrelentless.mess.util.LimbSlot
import com.unrelentless.mess.util.deserializeInnerStack
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.min


class MessScreenHandler(
        syncId: Int,
        private val playerInventory: PlayerInventory,
        val owner: BrainBlockEntity?,
) : ScreenHandler(HANDLER_TYPE, syncId) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "mess_screen_handler")
        val HANDLER_TYPE: ScreenHandlerType<MessScreenHandler> = ScreenHandlerRegistry.registerExtended(IDENTIFIER, ::MessScreenHandler)

        fun openScreen(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity) {
            state.createScreenHandlerFactory(world, pos).let {
                val blockEntity = world.getBlockEntity(pos) as? BrainBlockEntity
                blockEntity?.updateLimbs()
                player.openHandledScreen(it)
            }
        }
    }

    constructor(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): this(
            syncId,
            playerInventory,
            null
    ) {
        allLimbs = buf.readIntArray().map { int ->
            LimbInventory(Level.values().find { it.size == int }!!, null)
        }.toTypedArray()

        val items = (buf.readCompoundTag()?.get("items") as ListTag)
                .mapNotNull { (it as CompoundTag).deserializeInnerStack() }

        val tabs: Map<Level, Boolean> = Level.values().map {
            Pair(buf.readEnumConstant(Level::class.java), buf.readBoolean())
        }.toMap()

        allLimbs?.forEachIndexed {
            index, limb -> limb.depositStack(items[index])
        }

        tabs.forEach {
            selectedTabs[it.key] = it.value
        }
    }

    val selectedTabs: HashMap<Level, Boolean> = hashMapOf(
            Pair(Level.LOW, true),
            Pair(Level.MID, true),
            Pair(Level.HIGH, true)
    )

    private var scrollPosition = 0.0f
    private var searchString = ""
    private var scrolledRows: Int = 0
    private var tabbedLimbs: Array<LimbInventory> = emptyArray()

    private var allLimbs: Array<LimbInventory>? = null
        get() = if(owner != null)
            owner.limbs?.map(LimbBlockEntity::inventory)?.toTypedArray()
        else
            field

    var limbs: Array<LimbInventory> = emptyArray()
        private set
    var limbsToDisplay: Array<LimbInventory> = emptyArray()
        private set

    override fun canUse(player: PlayerEntity?): Boolean = true
    override fun onSlotClick(index: Int, mouseButton: Int, actionType: SlotActionType, playerEntity: PlayerEntity): ItemStack {
        if(!playerEntity.world.isClient) createNewSlots()
        if(index == -1) return ItemStack.EMPTY

        when(actionType) {
            SlotActionType.QUICK_MOVE -> return transferSlot(playerEntity, index)
            SlotActionType.PICKUP -> return pickup(index, mouseButton, playerEntity)
            else -> super.onSlotClick(index, mouseButton, actionType, playerEntity)
            // TODO:  Implement custom quickcraft or figure out why its not working
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
        if(index < limbsToDisplay.size) {
            val count = min(slotStack.item.maxCount, slotStack.count)
            val itemStackCopy = slotStack.copy()
            itemStackCopy.count = count

            if(!insertItem(itemStackCopy, limbs.size, this.slots.size, true))
                return ItemStack.EMPTY

            slotStack.decrement(count)
        } else {
            val limbWithItems = tabbedLimbs.filter { canStacksCombine(slotStack, it.getStack()) }
            val iterator = limbWithItems.iterator()

            // Fill in inventories that already have items
            while(iterator.hasNext() && !slotStack.isEmpty) {
                (iterator.next()).depositStack(slotStack)
            }

            // Fill empty slot with remainder
            if(!slotStack.isEmpty) {
                val emptyLimb = tabbedLimbs.find { it.isEmpty }
                emptyLimb?.depositStack(slotStack)
            }
        }

        slot.markDirty()
        updateInfo()
        propogateChangesToPlayers()

        return slotStack
    }

    private fun pickup(index: Int, mouseButton: Int, playerEntity: PlayerEntity): ItemStack {
        if (index == -999) {
            val count = if(mouseButton == 0) playerInventory.cursorStack.count else 1
            playerEntity.dropItem(playerInventory.cursorStack.split(count), true)
            return ItemStack.EMPTY
        }

        val slot = slots[index]
        val slotStack = slot.stack
        val cursorStack = playerEntity.inventory.cursorStack

        if(index < limbsToDisplay.size) {
            if(!cursorStack.isEmpty) {
                val count = if(mouseButton == 0) playerInventory.cursorStack.count else 1
                ((slot.inventory) as LimbInventory).depositStack(cursorStack, count)
            } else {
                val count = min(slotStack.item.maxCount, slotStack.count) / (mouseButton + 1)
                playerEntity.inventory.cursorStack = ((slot.inventory) as LimbInventory).withdrawStack(count)
            }
        } else {
            return super.onSlotClick(index, mouseButton, SlotActionType.PICKUP, playerEntity)
        }

        updateInfo()
        propogateChangesToPlayers()

        return ItemStack.EMPTY
    }

    fun updateInfo(searchString: String = this.searchString, scrollPosition: Float = this.scrollPosition) {
        this.searchString = searchString
        this.scrollPosition = scrollPosition

        calculateScrolledRows()
        updateTabbedLimbs()
        updateLimbs()
        updateLimbsToDisplay()

        if(playerInventory.player.world.isClient) {
            syncToServer()
            createNewSlots()
        }
    }

    private fun propogateChangesToPlayers() {
        if(playerInventory.player.world.isClient) return

        playerInventory.player.world?.players
                ?.filter { it != playerInventory.player }
                ?.mapNotNull { it.currentScreenHandler as? MessScreenHandler }
                ?.forEach(MessScreenHandler::updateInfo)
    }

    fun toggleTab(selectedTabLevel: Level) {
        this.selectedTabs[selectedTabLevel] = !this.selectedTabs[selectedTabLevel]!!
    }

    fun updateClientLimbs(limbs: Array<LimbInventory>) {
        allLimbs = limbs
        updateInfo()
    }

    private fun calculateScrolledRows() {
        val numberOfPositions = (this.limbs.size + MessScreen.COLUMNS - 1) / MessScreen.COLUMNS - MessScreen.ROWS
        scrolledRows = (numberOfPositions * scrollPosition + 0.5).toInt()
    }

    private fun updateTabbedLimbs() {
        tabbedLimbs = allLimbs?.sortedBy(LimbInventory::isEmpty)?.filter {
            selectedTabs[it.level] == true
        }?.toTypedArray() ?: emptyArray()
    }

    private fun updateLimbs() {
        limbs = tabbedLimbs.filter { limb ->
            limb.getStack().item.toString().contains(searchString)
        }.toTypedArray()
    }

    private fun updateLimbsToDisplay() {
        limbsToDisplay =  limbs.filterIndexed { index, _ ->
            val min = scrolledRows * MessScreen.COLUMNS
            val max = min(limbs.size, MessScreen.INV_SIZE + min)
            (min until max).contains(index)
        }.toTypedArray()
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

    private fun syncToServer() {
        if(!playerInventory.player.world.isClient) return

        val buffer = PacketByteBuf(Unpooled.buffer())
        buffer.writeFloat(scrollPosition)
        buffer.writeString(searchString)

        selectedTabs.forEach {
            buffer.writeEnumConstant(it.key)
            buffer.writeBoolean(it.value)
        }

        ClientSidePacketRegistry.INSTANCE.sendToServer(Mess.C2S_IDENTIFIER, buffer)
    }
}