package com.unrelentless.mess.client.gui.screen

import com.mojang.blaze3d.systems.RenderSystem
import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.BrainBlock
import com.unrelentless.mess.block.entity.BrainBlockEntity
import com.unrelentless.mess.client.render.item.MessScreenItemRenderer
import com.unrelentless.mess.mixin.MinecraftClientMixin
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper.clamp
import net.minecraft.world.World
import java.lang.Integer.min


class MessScreen(
        handler: MessScreenHandler?,
        inventory: PlayerInventory?,
        title: Text?
) : HandledScreen<MessScreenHandler>(handler, inventory, title) {

    companion object : Clientside {
        val TEXTURE = Identifier(Mess.IDENTIFIER, "textures/gui/brain_blank.png")
        val TEXTURE_ETC = Identifier(Mess.IDENTIFIER, "textures/gui/brain_etc.png")

        const val ROWS = 5
        const val COLUMNS = 9
        const val INV_SIZE = ROWS * COLUMNS
        override fun renderOnClient() {
            ScreenRegistry.register(MessScreenHandler.HANDLER_TYPE, ::MessScreen)
        }
    }

    private var scrolling = false
    private var scrollPosition = 0.0f
    private var ignoreTypedCharacter = false
    private var searchBox: TextFieldWidget? = null

    init {
        this.backgroundHeight = 203
        this.backgroundWidth = 195
        this.titleX = super.titleX
        this.playerInventoryTitleX = super.playerInventoryTitleX
    }

    override fun init(client: MinecraftClient, width: Int, height: Int) {
        super.init(client, width, height)
        this.itemRenderer = MessScreenItemRenderer(
                client.textureManager,
                client.bakedModelManager,
                (client as MinecraftClientMixin).itemColors
        )
    }

    override fun init() {
        super.init()
        client?.keyboard?.setRepeatEvents(true)

        searchBox = TextFieldWidget(
                textRenderer,
                this.x + 82,
                this.y + 6,
                80,
                9,
                TranslatableText("search." + Mess.IDENTIFIER + ".mess")
        )

        searchBox?.setMaxLength(50)
        searchBox?.setHasBorder(false)
        searchBox?.setEditableColor(16777215)
        searchBox?.isVisible = true
        searchBox?.setFocusUnlocked(false)
        searchBox?.setSelected(true)

        children.add(searchBox)
        updateHandler()
    }

    override fun tick() {
        super.tick()
        searchBox?.tick()
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title, titleX.toFloat(), titleY.toFloat(), 4210752)
        textRenderer.draw(
                matrices,
                playerInventory.displayName,
                playerInventoryTitleX.toFloat(),
                playerInventoryTitleY.toFloat() + 38,
                4210752
        )
    }

    override fun drawBackground(matrices: MatrixStack, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client?.textureManager?.bindTexture(TEXTURE)
        val originX = (width - backgroundWidth) / 2
        val originY = (height - backgroundHeight) / 2

        drawTexture(matrices, originX, originY, 0, 0, backgroundWidth, backgroundHeight)
        drawSlots(matrices)
        drawTabs(matrices)
        drawTabIcons(matrices)
        drawScrollbar(matrices)
        searchBox?.render(matrices, mouseX, mouseY, delta)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(button != 0 || !isClickInScrollbar(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button)
        if(isClickInTab(mouseX, mouseY) != null) return true

        scrolling = hasScrollbar()
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            scrolling = false

            val tabLevel = isClickInTab(mouseX, mouseY)
            if(tabLevel != null) {
                scrollPosition = 0.0f
                handler.toggleTab(tabLevel)
                updateHandler()
            }
        }

        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!this.hasScrollbar())
            return false

        val numberOfPositions = (this.handler.limbs.size + COLUMNS - 1) / COLUMNS - ROWS
        scrollPosition = clamp((scrollPosition - amount / numberOfPositions).toFloat(), 0.0f, 1.0f)

        updateHandler()

        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if(!scrolling) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

        //Entering magic number land
        val scrollbarStartY = y + 18
        val scrollbarEndY = scrollbarStartY + 112
        val absoluteScrollPosition = ((mouseY - scrollbarStartY - 7.5f) / ((scrollbarEndY - scrollbarStartY).toFloat() - 42.0f)).toFloat()
        scrollPosition = clamp(absoluteScrollPosition, 0.0f, 1.0f)

        updateHandler()

        return true
    }

    override fun charTyped(chr: Char, keyCode: Int): Boolean {
        val searchBox = this.searchBox as TextFieldWidget
        if(ignoreTypedCharacter) return false
        if(!searchBox.charTyped(chr, keyCode)) return false

        updateHandler()
        return true
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        ignoreTypedCharacter = false
        return super.keyReleased(keyCode, scanCode, modifiers)
    }
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val searchBox = this.searchBox as TextFieldWidget

        ignoreTypedCharacter = false

        val bl2 = InputUtil.fromKeyCode(keyCode, scanCode).method_30103().isPresent
        return if (focusedSlot?.hasStack() == true && bl2 && handleHotbarKeyPressed(keyCode, scanCode)) {
            ignoreTypedCharacter = true
            true
        } else {
            val string = searchBox.text
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                if (string != searchBox.text) {
                    scrollPosition = 0.0f
                    updateHandler()
                }
                true
            } else {
                if (searchBox.isFocused && searchBox.isVisible && keyCode != 256)
                    true
                else
                    super.keyPressed(keyCode, scanCode, modifiers)
            }
        }
    }

    private fun drawSlots(matrices: MatrixStack) {
        client?.textureManager?.bindTexture(TEXTURE_ETC)

        val xPos = x + 8
        val yPos = y + 17
        val totalItemsToShow = handler.limbsToDisplay.size
        val rowTotal = min(1 + totalItemsToShow / COLUMNS, ROWS)

        for (row in 0 until rowTotal) {
            val columnMax = min(totalItemsToShow - COLUMNS * row, COLUMNS)
            for (column in 0 until columnMax) {
                this.drawTexture(
                        matrices,
                        xPos + 18 * column,
                        yPos + 18 * row,
                        0,
                        16,
                        18,
                        18
                )
            }
        }
    }

    private fun drawTabs(matrices: MatrixStack) {
        client?.textureManager?.bindTexture(TEXTURE_ETC)

        val xPos = x + backgroundWidth

        for(tab in handler.selectedTabs) {
            val yOrigin = if(!tab.value) 35 else 64
            val xPos = if(!tab.value) xPos else xPos - 3
            val yPos = y + 18 + (tab.key.displayIndex * 29)
            this.drawTexture(
                    matrices,
                    xPos,
                    yPos,
                    0,
                    yOrigin,
                    32,
                    28
            )
        }
    }
    private fun drawTabIcons(matrices: MatrixStack) {
        val xPos = x + backgroundWidth

        itemRenderer.zOffset = 100.0f

        for(level in handler.selectedTabs.keys) {
            val yPos = y + 18 + (level.displayIndex * 29)

            RenderSystem.enableRescaleNormal()
            val itemStack = ItemStack(level.block, 1)
            itemRenderer.renderInGuiWithOverrides(itemStack, xPos + 4, yPos + 6)
            itemRenderer.renderGuiItemOverlay(textRenderer, itemStack, xPos + 4, yPos + 6)
        }

        itemRenderer.zOffset = 0.0f
    }

    private fun drawScrollbar(matrices: MatrixStack) {
        val xPos = x + backgroundWidth

        client?.textureManager?.bindTexture(TEXTURE_ETC)
        this.drawTexture(
                matrices,
                xPos - 20,
                y + 18 + (73 * scrollPosition).toInt(),
                if (hasScrollbar()) 0 else 12,
                0,
                12,
                15
        )
    }

    private fun hasScrollbar(): Boolean = handler.limbs.size > INV_SIZE
    private fun isClickInScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val xPos = x + backgroundWidth

        val widthRange = (xPos-24..xPos-10)
        val heightRange = (y+18..y+105)

        return widthRange.contains(mouseX.toInt()) && heightRange.contains(mouseY.toInt())
    }

    private fun isClickInTab(mouseX: Double, mouseY: Double): Level? {
        val xPos = x + backgroundWidth
        val widthRange = (xPos..xPos+32)

        Level.values().forEach {
            val yMin = (y + 18 + (it.displayIndex * 29))
            val heightRange = (yMin..yMin+28)

            if(widthRange.contains(mouseX.toInt()) && heightRange.contains(mouseY.toInt())) {
                return it
            }
        }

        return null
    }

    private fun updateHandler() {
        val searchBox = this.searchBox as TextFieldWidget
        handler.updateInfo(searchBox.text, scrollPosition)
    }
}

private val Level.displayIndex: Int
    get() = when(this) {
        Level.LOW -> 0
        Level.MID -> 1
        Level.HIGH -> 2
    }