package com.unrelentless.mess.client.gui.screen

import com.mojang.blaze3d.systems.RenderSystem
import com.unrelentless.mess.Mess
import com.unrelentless.mess.client.render.item.MessScreenItemRenderer
import com.unrelentless.mess.mixin.MinecraftClientMixin
import com.unrelentless.mess.screen.MessScreenHandler
import com.unrelentless.mess.util.Clientside
import com.unrelentless.mess.util.Level
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper.clamp
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

        override fun renderOnClient() = ScreenRegistry.register(MessScreenHandler.HANDLER_TYPE, ::MessScreen)
    }

    private var scrolling = false
        set(newValue) {
            if(!newValue) { updateHandler() }
            field = newValue
        }
    private var scrolledRows = 0
        set(newValue) {
            if(field != newValue) { updateHandler() }
            field = newValue
        }
    private var ignoreTypedCharacter = false
    private var scrollPosition = 0.0f

    private var searchBox: TextFieldWidget = TextFieldWidget(
            textRenderer,
            x + 82,
            y + 6,
            80,
            9,
            TranslatableText("search." + Mess.IDENTIFIER + ".mess")
    )

    init {
        backgroundHeight = 203
        backgroundWidth = 195
        titleX = super.titleX
        playerInventoryTitleX = super.playerInventoryTitleX
    }

    override fun init(client: MinecraftClient, width: Int, height: Int) {
        super.init(client, width, height)
        itemRenderer = MessScreenItemRenderer(
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
                x + 82,
                y + 6,
                80,
                9,
                TranslatableText("search." + Mess.IDENTIFIER + ".mess")
        )

        searchBox.setMaxLength(50)
        searchBox.setHasBorder(false)
        searchBox.setEditableColor(16777215)
        searchBox.isVisible = true
        searchBox.setFocusUnlocked(false)
        searchBox.setSelected(true)
        children.add(searchBox)
        searchBox.text = handler.searchString
        scrollPosition = handler.scrollPosition

        updateHandler()
    }

    override fun tick() {
        super.tick()
        searchBox.tick()
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

        searchBox.render(matrices, mouseX, mouseY, delta)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(matrices)
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
        if (!hasScrollbar()) return false

        val numberOfPositions = (handler.limbs.size + COLUMNS - 1) / COLUMNS - ROWS
        scrollPosition = clamp((scrollPosition - amount / numberOfPositions).toFloat(), 0.0f, 1.0f)
        scrolledRows = (numberOfPositions * scrollPosition + 0.5).toInt()

        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if(!scrolling) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

        //Entering magic number land
        val scrollbarStartY = y + 18
        val scrollbarEndY = scrollbarStartY + 112
        val absoluteScrollPosition = ((mouseY - scrollbarStartY - 7.5f) / ((scrollbarEndY - scrollbarStartY).toFloat() - 42.0f)).toFloat()
        val numberOfPositions = (handler.limbs.size + COLUMNS - 1) / COLUMNS - ROWS

        scrollPosition = clamp(absoluteScrollPosition, 0.0f, 1.0f)
        scrolledRows = (numberOfPositions * scrollPosition + 0.5).toInt()

        return true
    }

    override fun charTyped(chr: Char, keyCode: Int): Boolean {
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
        ignoreTypedCharacter = false

        val keyPresent = InputUtil.fromKeyCode(keyCode, scanCode).method_30103().isPresent

        if (focusedSlot?.hasStack() == true && keyPresent && handleHotbarKeyPressed(keyCode, scanCode)) {
            ignoreTypedCharacter = true

            return true
        }

        if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            scrollPosition = 0.0f
            updateHandler()

            return true
        }

        return if (keyCode != 256) true else super.keyPressed(keyCode, scanCode, modifiers)
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
                val index = column + (row * COLUMNS)

                drawTexture(
                        matrices,
                        xPos + 18 * column,
                        yPos + 18 * row,
                        0,
                        16,
                        18,
                        18
                )

                if(index < handler.limbsToDisplay.size) {
                    val level = handler.limbsToDisplay[index].level

                    RenderSystem.color4f(1.0f, 1.0f, 1.0f, 0.25f)
                    RenderSystem.enableBlend()

                    drawTexture(
                            matrices,
                            xPos + 1 + 18 * column,
                            yPos + 1 + 18 * row,
                            0 + (level.displayIndex * 16),
                            93,
                            16,
                            16
                    )

                    RenderSystem.disableBlend()
                    RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
                }
            }
        }
    }

    private fun drawTabs(matrices: MatrixStack) {
        client?.textureManager?.bindTexture(TEXTURE_ETC)

        val xPos = x + backgroundWidth

        for(tab in handler.selectedTabs) {
            val yOrigin = if(!tab.value) 35 else 64
            val shiftedXPos = if(!tab.value) xPos else xPos - 3
            val yPos = y + 18 + (tab.key.displayIndex * 29)

            drawTexture(
                    matrices,
                    shiftedXPos,
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
        val scale = 0.8f
        itemRenderer.zOffset = 100.0f

        RenderSystem.pushMatrix()
        RenderSystem.scalef(scale, scale, 1.0f)

        for(level in handler.selectedTabs.keys) {
            val yPos = y + 18 + (level.displayIndex * 29)
            val itemStack = ItemStack(level.block, 1)
            val levelStringWidth = textRenderer.getWidth(level.name)

            itemRenderer.renderInGuiWithOverrides(itemStack, ((xPos + 6) / scale).toInt(), ((yPos + 4) / scale).toInt())
            itemRenderer.renderGuiItemOverlay(textRenderer, itemStack, ((xPos + 6) / scale).toInt(), ((yPos + 4) / scale).toInt())
            textRenderer.draw(matrices, level.name, (xPos + 14 - levelStringWidth/2) / scale, (yPos + 18) / scale, 4210752)
        }

        RenderSystem.popMatrix()

        itemRenderer.zOffset = 0.0f
    }

    private fun drawScrollbar(matrices: MatrixStack) {
        val xPos = x + backgroundWidth

        client?.textureManager?.bindTexture(TEXTURE_ETC)
        drawTexture(
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
    private fun updateHandler(syncToServer: Boolean = true) {
        handler.updateInfo(syncToServer, searchBox.text, scrollPosition)
    }
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

}

private val Level.displayIndex: Int
    get() = when(this) {
        Level.LOW -> 0
        Level.MID -> 1
        Level.HIGH -> 2
    }