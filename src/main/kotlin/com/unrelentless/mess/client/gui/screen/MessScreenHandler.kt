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
        val owner: BrainBlockEntity? = null
) : ScreenHandler(HANDLER_TYPE, syncId) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "mess_screen_handler")
        val HANDLER_TYPE: ScreenHandlerType<MessScreenHandler> = ScreenHandlerRegistry.registerExtended(IDENTIFIER, ::MessScreenHandler)

        fun openScreen(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity) {
            state.createScreenHandlerFactory(world, pos).let {
                // Hack to clear search strings and scroll position when new screen
                if(player.currentScreenHandler !is MessScreenHandler) {
                    val blockEntity = world.getBlockEntity(pos) as? BrainBlockEntity ?: return
                    blockEntity.updateScrollPosition(0.0f, player)
                    blockEntity.updateSearchString("", player)
                }

                player.openHandledScreen(it)
            }
        }
    }

    constructor(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): this(
            syncId,
            playerInventory
    ) {
        val inventories = buf.readIntArray().map { int ->
            LimbInventory(Level.values().find { it.size == int }!!, null)
        }

        allLimbs.addAll(inventories)

        val items = (buf.readCompoundTag()?.get("items") as ListTag)
                .mapNotNull { (it as CompoundTag).deserializeInnerStack() }

        allLimbs.forEachIndexed {
            index, limb -> limb.depositStack(items[index])
        }

        if(buf.readBoolean()) {
            Level.values().map {
                selectedTabs[buf.readEnumConstant(Level::class.java)] = buf.readBoolean()
            }
        }

        scrollPosition = buf.readFloat()
        searchString = buf.readString()
    }

    val selectedTabs: HashMap<Level, Boolean> = hashMapOf(
            Pair(Level.LOW, true),
            Pair(Level.MID, true),
            Pair(Level.HIGH, true)
    )

    private val allLimbs: MutableList<LimbInventory> = mutableListOf()
        get() = owner?.limbs?.map(LimbBlockEntity::inventory)?.toMutableList() ?: field

    private val tabbedLimbs: List<LimbInventory>
        get() = allLimbs.filter { selectedTabs[it.level] == true }

    val limbs: List<LimbInventory>
        get() = tabbedLimbs.filter { limb ->
            //TODO: Find a way to do this on the server
//            val toolTip = limb.getStack().getTooltip(null, TooltipContext.Default.NORMAL)
//                    .map { Formatting.strip(it.string)?.trim()?.toLowerCase() }
//                    .first { !it.isNullOrEmpty() }

            limb.getStack().item.toString().contains(searchString)
        }

    val limbsToDisplay: List<LimbInventory>
        get() = limbs
                .sortedBy { it.isEmpty }
                .filterIndexed { index, _ ->
                    val min = scrolledRows * MessScreen.COLUMNS
                    val max = min(limbs.size, MessScreen.INV_SIZE + min)
                    (min until max).contains(index)
                }

    var scrollPosition = 0.0f
        private set

    var searchString = ""
        private set

    private val scrolledRows: Int
        get() {
            val numberOfPositions = (limbs.size + MessScreen.COLUMNS - 1) / MessScreen.COLUMNS - MessScreen.ROWS
            return (numberOfPositions * scrollPosition + 0.5).toInt()
        }

    override fun canUse(player: PlayerEntity?): Boolean = true
    override fun onSlotClick(index: Int, mouseButton: Int, actionType: SlotActionType, playerEntity: PlayerEntity): ItemStack {
        if(!playerEntity.world.isClient) {
            syncToServer()
            createNewSlots()
        }

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
        val slot = slots[index]

        if(!slot.hasStack()) return ItemStack.EMPTY
        val slotStack = slot.stack

        // Slot clicked in MESS inventory
        if(index < limbsToDisplay.size) {
            val count = min(slotStack.item.maxCount, slotStack.count)
            val itemStackCopy = slotStack.copy()
            itemStackCopy.count = count

            if(!insertItem(itemStackCopy, limbsToDisplay.size, this.slots.size, true))
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
        if(playerInventory.player.world.isClient) createNewSlots()
        owner?.contentChanged(player)

        return slotStack
    }

    private fun pickup(index: Int, mouseButton: Int, player: PlayerEntity): ItemStack {
        if (index == -999) {
            val count = if(mouseButton == 0) playerInventory.cursorStack.count else 1
            player.dropItem(playerInventory.cursorStack.split(count), true)
            return ItemStack.EMPTY
        }

        val slot = slots[index]
        val slotStack = slot.stack
        val cursorStack = player.inventory.cursorStack

        if(index < limbsToDisplay.size) {
            if(!cursorStack.isEmpty) {
                val count = if(mouseButton == 0) playerInventory.cursorStack.count else 1
                ((slot.inventory) as LimbInventory).depositStack(cursorStack, count)
            } else {
                val count = min(slotStack.item.maxCount, slotStack.count) / (mouseButton + 1)
                player.inventory.cursorStack = ((slot.inventory) as LimbInventory).withdrawStack(count)
            }
        } else {
            return super.onSlotClick(index, mouseButton, SlotActionType.PICKUP, player)
        }

        slot.markDirty()
        if(playerInventory.player.world.isClient) createNewSlots()
        owner?.contentChanged(player)

        return ItemStack.EMPTY
    }

    fun updateInfo(
            syncToServer: Boolean = true,
            searchString: String = this.searchString,
            scrollPosition: Float = this.scrollPosition
    ) {
        this.searchString = searchString
        this.scrollPosition = scrollPosition

        if(syncToServer) syncToServer()
        if(playerInventory.player.world.isClient) createNewSlots()
    }

    fun updateClientLimbs(limbs: List<LimbInventory>) {
        allLimbs.clear()
        allLimbs.addAll(limbs)
        createNewSlots()
    }

    fun toggleTab(selectedTabLevel: Level) {
        selectedTabs[selectedTabLevel] = !selectedTabs[selectedTabLevel]!!
    }

    private fun createNewSlots() {
        slots.clear()

        // Magic numbers
        val xOffset = 9
        val yOffsetInv = 18
        val yOffsetPlayerInv = 121
        val yOffsetPlayerHotbar = 179
        val rowTotal = min(1 + limbsToDisplay.size / MessScreen.COLUMNS, MessScreen.ROWS)

        // MESS inv
        for (row in 0 until rowTotal) {
            val columnMax = min(limbsToDisplay.size - MessScreen.COLUMNS * row, MessScreen.COLUMNS)

            for (column in 0 until columnMax) {
                val index = column + row * MessScreen.COLUMNS
                addSlot(LimbSlot(limbsToDisplay[index], index, xOffset + column * 18, yOffsetInv + row * 18))
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