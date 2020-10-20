package com.unrelentless.mess.client.gui.screen

import com.mojang.blaze3d.systems.RenderSystem
import com.unrelentless.mess.Mess
import com.unrelentless.mess.client.render.item.MessScreenItemRenderer
import com.unrelentless.mess.mixin.MessMinecraftClientMixin
import com.unrelentless.mess.util.Clientside
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
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
        val TEXTURE = Identifier(Mess.IDENTIFIER, "textures/gui/heart_blank.png")
        val TEXTURE_SLOT = Identifier(Mess.IDENTIFIER, "textures/gui/heart_blank.png")
        val TEXTURE_TABS = Identifier("textures/gui/container/creative_inventory/tabs.png")

        const val ROWS = 5
        const val COLUMNS = 9
        const val INV_SIZE = ROWS * COLUMNS
        override fun renderOnClient() {
            ScreenRegistry.register(MessScreenHandler.HANDLER_TYPE, ::MessScreen)
        }
    }

    private val slotSize = 18
    private val slotOriginX = 238
    private val slotOriginY = 0
    private var scrolling = false

    private var scrollPosition = 0f
    set(newValue) {
        field = newValue
        handler.scrollPosition = newValue
    }

    private var ignoreTypedCharacter = false
    private val searchBox: TextFieldWidget by lazy {
        val textField = TextFieldWidget(
                textRenderer,
                x + 82,
                y + 6,
                80,
                9,
                TranslatableText("search." + Mess.IDENTIFIER + ".mess")
        )

        textField.setMaxLength(50)
        textField.setHasBorder(false)
        textField.isVisible = false
        textField.setEditableColor(16777215)

        textField
    }

    init {
        this.backgroundHeight = 203
        this.backgroundWidth = 195
    }

    override fun init(client: MinecraftClient, width: Int, height: Int) {
        super.init(client, width, height)
        this.itemRenderer = MessScreenItemRenderer(
                client.textureManager,
                client.bakedModelManager,
                (client as MessMinecraftClientMixin).itemColors
        )
    }

    override fun init() {
        super.init()
        client?.keyboard?.setRepeatEvents(true)
        children.add(searchBox)

        searchBox.isVisible = true
        searchBox.setFocusUnlocked(false)
        searchBox.setSelected(true)
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
        val halfWidth = (width - backgroundWidth) / 2
        val halfHeight = (height - backgroundHeight) / 2

        drawTexture(matrices, halfWidth, halfHeight, 0, 0, backgroundWidth, backgroundHeight)
        drawSlots(matrices)
        drawScrollbar(matrices)
        searchBox.render(matrices, mouseX, mouseY, delta)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(button != 0 && !isClickInScrollbar(mouseX, mouseY))
            return super.mouseClicked(mouseX, mouseY, button)

        scrolling = hasScrollbar()
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0)
            scrolling = false

        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!this.hasScrollbar())
            return false

        val numberOfPositions = (this.handler.limbs.size + COLUMNS - 1) / COLUMNS - ROWS
        scrollPosition = clamp((scrollPosition - amount / numberOfPositions).toFloat(), 0.0f, 1.0f)

        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if(!scrolling) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

        //Entering magic number land
        val scrollbarStartY = y + 18
        val scrollbarEndY = scrollbarStartY + 112
        val absoluteScrollPosition = ((mouseY - scrollbarStartY - 7.5f) / ((scrollbarEndY - scrollbarStartY).toFloat() - 42.0f)).toFloat()
        scrollPosition = clamp(absoluteScrollPosition, 0.0f, 1.0f)

        return true
    }

    override fun charTyped(chr: Char, keyCode: Int): Boolean {
        if(ignoreTypedCharacter) return false
        if(!searchBox.charTyped(chr, keyCode)) return false
        val string = searchBox.text
        search()
        return true
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        ignoreTypedCharacter = false
        return super.keyReleased(keyCode, scanCode, modifiers)
    }
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        ignoreTypedCharacter = false

        val bl2 = InputUtil.fromKeyCode(keyCode, scanCode).method_30103().isPresent
        return if (focusedSlot?.hasStack() == true && bl2 && handleHotbarKeyPressed(keyCode, scanCode)) {
            ignoreTypedCharacter = true
            true
        } else {
            val string = searchBox.text
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                if (string != searchBox.text) {
                    search()
                }
                true
            } else {
                if (searchBox.isFocused && searchBox.isVisible && keyCode !== 256) true else super.keyPressed(keyCode, scanCode, modifiers)
            }
        }
    }

    private fun drawSlots(matrices: MatrixStack) {
        client?.textureManager?.bindTexture(TEXTURE_SLOT)


        val xPos = (width - backgroundWidth) / 2 + 8
        val yPos = (height - backgroundHeight) / 2 + 17
        val totalItemsToShow = handler.limbsToDisplay.size
        val rowTotal = min(1 + totalItemsToShow / COLUMNS, ROWS)

        for (row in 0 until rowTotal) {
            val columnMax = min(totalItemsToShow - COLUMNS * row, COLUMNS)

            for (column in 0 until columnMax) {
                this.drawTexture(
                        matrices,
                        xPos + slotSize * column,
                        yPos + slotSize * row,
                        slotOriginX,
                        slotOriginY,
                        slotSize,
                        slotSize
                )
            }
        }
    }
    private fun drawScrollbar(matrices: MatrixStack) {
        client?.textureManager?.bindTexture(TEXTURE_TABS)
        this.drawTexture(
                matrices,
                291,
                36 + (73 * scrollPosition).toInt(),
                232 + if (hasScrollbar()) 0 else 12,
                0,
                12,
                15
        )
    }

    private fun hasScrollbar(): Boolean = handler.limbs.size > INV_SIZE
    private fun isClickInScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val k = x + 175
        val l = y + 18
        val m = k + 14
        val n = l + 112
        return mouseX >= k.toDouble()
                && mouseY >= l.toDouble()
                && mouseX < m.toDouble()
                && mouseY < n.toDouble()
    }

    private fun search() {
        scrollPosition = 0.0f
        handler.searchString = searchBox.text
    }
}