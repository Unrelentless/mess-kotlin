package com.unrelentless.mess.screen

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.entity.BrainBlockEntity
import com.unrelentless.mess.block.entity.LimbBlockEntity
import com.unrelentless.mess.client.gui.screen.MessScreen
import com.unrelentless.mess.util.Level
import com.unrelentless.mess.screen.slot.LimbSlot
import com.unrelentless.mess.extensions.deserializeInnerStack
import com.unrelentless.mess.inventory.LimbInventory
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
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
        val C2S_IDENTIFIER = Identifier(Mess.IDENTIFIER, "sync_server")

        fun openScreen(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity) {
            state.createScreenHandlerFactory(world, pos).let {
                val blockEntity = world.getBlockEntity(pos) as? BrainBlockEntity ?: return
                // Hack to clear search strings and scroll position when new screen
                if(player.currentScreenHandler !is MessScreenHandler) {
                    blockEntity.updateScrollPosition(0.0f, player)
                    blockEntity.updateSearchString("", player)
                }
                player.openHandledScreen(it)
            }
        }

        init {
            ServerPlayNetworking.registerGlobalReceiver(C2S_IDENTIFIER) { minecraftServer, serverPlayerEntity, _, packetByteBuf, _ ->
                val scrollPosition = packetByteBuf.readFloat()
                val searchString = packetByteBuf.readString(Short.MAX_VALUE.toInt())
                val tabs: Map<Level, Boolean> = Level.values().map {
                    Pair(packetByteBuf.readEnumConstant(Level::class.java), packetByteBuf.readBoolean())
                }.toMap()

                minecraftServer.execute {
                    val handler = serverPlayerEntity.currentScreenHandler as? MessScreenHandler ?: return@execute

                    for(tab in tabs) { handler.selectedTabs[tab.key] = tab.value }
                    handler.updateInfo(false, searchString, scrollPosition)
                    handler.owner?.updateTabs(handler.selectedTabs, serverPlayerEntity)
                    handler.owner?.updateSearchString(handler.searchString, serverPlayerEntity)
                    handler.owner?.updateScrollPosition(handler.scrollPosition, serverPlayerEntity)
                }
            }
//            ServerSidePacketRegistry.INSTANCE.register(C2S_IDENTIFIER) { context, buffer ->
//                val scrollPosition = buffer.readFloat()
//                val searchString = buffer.readString(Short.MAX_VALUE.toInt())
//                val tabs: Map<Level, Boolean> = Level.values().map {
//                    Pair(buffer.readEnumConstant(Level::class.java), buffer.readBoolean())
//                }.toMap()
//
//                context.taskQueue.execute {
//                    val handler = context.player.currentScreenHandler as? MessScreenHandler ?: return@execute
//
//                    for(tab in tabs) { handler.selectedTabs[tab.key] = tab.value }
//                    handler.updateInfo(false, searchString, scrollPosition)
//                    handler.owner?.updateTabs(handler.selectedTabs, context.player)
//                    handler.owner?.updateSearchString(handler.searchString, context.player)
//                    handler.owner?.updateScrollPosition(handler.scrollPosition, context.player)
//                }
//            }
        }
    }

    constructor(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): this(
            syncId,
            playerInventory
    ) {
        val sizes = buf.readIntArray()
        val inventories = (buf.readNbt()?.get("items") as NbtList)
                .mapIndexedNotNull { index, tag ->
                    val inv = LimbInventory(Level.values().find { it.size == sizes[index] }!!, null)
                    inv.depositStack((tag as NbtCompound).deserializeInnerStack() ?: ItemStack.EMPTY)

                    inv
                }

        allLimbs.addAll(inventories)

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

    private var tabbedLimbs: List<LimbInventory> = emptyList()
    var limbs: List<LimbInventory> = emptyList()
        private set
    var limbsToDisplay: List<LimbInventory> = emptyList()
        private set

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
    override fun onSlotClick(index: Int, mouseButton: Int, actionType: SlotActionType, playerEntity: PlayerEntity) {
        if(index == -1) return// ItemStack.EMPTY
        if(!playerEntity.world.isClient) { createNewSlots() }

        when(actionType) {
            SlotActionType.QUICK_MOVE -> {
                transferSlot(playerEntity, index)
                return
            }
            SlotActionType.PICKUP -> return pickup(index, mouseButton, playerEntity)
            else -> return super.onSlotClick(index, mouseButton, actionType, playerEntity)
            // TODO:  Implement custom quickcraft or figure out why its not working
//            SlotActionType.QUICK_CRAFT -> return quickCraft(index, mouseButton, playerEntity)
        }

        return super.onSlotClick(index, mouseButton, actionType, playerEntity)
    }

    override fun transferSlot(player: PlayerEntity, index: Int): ItemStack {
        if(!slots[index].hasStack()) return ItemStack.EMPTY

        val slot = slots[index]
        val slotStack = slot.stack

        // Slot clicked in MESS inventory
        if(index < limbsToDisplay.size) {
            val count = min(slotStack.item.maxCount, slotStack.count)
            val itemStackCopy = slotStack.copy()
            itemStackCopy.count = count
            insertItem(itemStackCopy, limbsToDisplay.size, this.slots.size, true)
            slotStack.decrement(count)
        } else {
            val limbWithItems = tabbedLimbs.filter { ItemStack.canCombine(slotStack, it.getStack()) }
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
        updateInfo(false)
        owner?.contentChangedByPlayer(player)

        return ItemStack.EMPTY
    }

    private fun pickup(index: Int, mouseButton: Int, player: PlayerEntity) {
        // Click outside window
        if (index == -999) {
            val count = if(mouseButton == 0) cursorStack.count else 1
            player.dropItem(cursorStack.split(count), true)
            return
        }

        val slot = slots[index]
        val slotStack = slot.stack

        if(index < limbsToDisplay.size) {
            if(!cursorStack.isEmpty) {
                val count = if(mouseButton == 0) cursorStack.count else 1
                ((slot.inventory) as LimbInventory).depositStack(cursorStack, count)
            } else {
                val count = min(slotStack.item.maxCount, slotStack.count) / (mouseButton + 1)
                cursorStack = ((slot.inventory) as LimbInventory).withdrawStack(count)
            }
        } else super.onSlotClick(index, mouseButton, SlotActionType.PICKUP, player)

        slot.markDirty()
        updateInfo(false)
        owner?.contentChangedByPlayer(player)
    }

    fun updateInfo(
            syncToServer: Boolean = true,
            searchString: String = this.searchString,
            scrollPosition: Float = this.scrollPosition
    ) {
        this.searchString = searchString
        this.scrollPosition = scrollPosition

        updateLimbs()

        if(syncToServer) { syncToServer() }
        if(playerInventory.player.world.isClient) { createNewSlots() }
    }

    fun toggleTab(selectedTabLevel: Level) {
        selectedTabs[selectedTabLevel] = !selectedTabs[selectedTabLevel]!!
    }

    private fun updateLimbs() {
        tabbedLimbs = allLimbs.filter { selectedTabs[it.level] == true }

        limbs = tabbedLimbs.filter { limb ->
            //TODO: Find a way to do this on the server
//            val toolTip = limb.getStack().getTooltip(null, TooltipContext.Default.NORMAL)
//                    .map { Formatting.strip(it.string)?.trim()?.toLowerCase() }
//                    .first { !it.isNullOrEmpty() }
                limb.getStack().item.name.string.toLowerCase().contains(searchString.toLowerCase())
        }

        limbsToDisplay = limbs
                .sortedBy { it.isEmpty }
                .filterIndexed { index, _ ->
                    val min = scrolledRows * MessScreen.COLUMNS
                    val max = min(limbs.size, MessScreen.INV_SIZE + min)
                    (min until max).contains(index)
                }
    }

    private fun createNewSlots() {
        slots.clear()

        println(slots.count())
        println(limbsToDisplay.count())

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

        println(slots.count())
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

//        ClientSidePacketRegistry.INSTANCE.sendToServer(C2S_IDENTIFIER, buffer)
        ClientPlayNetworking.send(C2S_IDENTIFIER, buffer);
    }
}