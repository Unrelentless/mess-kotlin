package com.unrelentless.mess.client.gui.screen

import com.mojang.blaze3d.systems.RenderSystem
import com.unrelentless.mess.Mess
import com.unrelentless.mess.client.render.item.MessScreenItemRenderer
import com.unrelentless.mess.mixin.MessMinecraftClientMixin
import com.unrelentless.mess.util.Clientside
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.MathHelper.clamp
import java.lang.Integer.min


class MessScreen(
        handler: MessScreenHandler?,
        inventory: PlayerInventory?,
        title: Text?
) : HandledScreen<MessScreenHandler>(handler, inventory, title) {

    companion object: Clientside {
        val TEXTURE = Identifier(Mess.IDENTIFIER, "textures/gui/heart_blank.png")
        val TEXTURE_SLOT = Identifier(Mess.IDENTIFIER, "textures/gui/heart_blank.png")
        val TEXTURE_TABS = Identifier("textures/gui/container/creative_inventory/tabs.png")

        val ROWS = 5
        val COLUMNS = 9
        val INV_SIZE = ROWS * COLUMNS
        override fun renderOnClient() {
            ScreenRegistry.register(MessScreenHandler.HANDLER_TYPE, ::MessScreen)
        }
    }
    var scrolledRows = 0

    private val slotSize = 18
    private val slotOriginX = 238
    private val slotOriginY = 0
    private var scrollPosition = 0f

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
        val i = (width - backgroundWidth) / 2
        val j = (height - backgroundHeight) / 2
        this.drawTexture(matrices, i, j, 0, 0, backgroundWidth, backgroundHeight)

        drawSlots(matrices)
        drawScrollbar(matrices, delta, mouseX, mouseY);
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }


    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!this.hasScrollbar()) return false
        if(this.handler.limbs == null) return false

        val scrollRows = (this.handler.limbs.size + COLUMNS - 1) / COLUMNS - ROWS

        scrollPosition = (scrollPosition - amount / scrollRows).toFloat()
        scrollPosition = clamp(scrollPosition, 0.0f, 1.0f)

        scrolledRows = (scrollRows * scrollPosition + 0.5).toInt()

        handler.scrollItems()
        return true
    }

    private fun drawSlots(matrices: MatrixStack) {
        client?.textureManager?.bindTexture(TEXTURE_SLOT)

        val totalItemsToShow = handler.limbsToDisplay?.size ?: 0

        val xPos = (width - backgroundWidth) / 2 + 8
        val yPos = (height - backgroundHeight) / 2 + 17

        val rowTotal = min(1 + totalItemsToShow / 9, ROWS)

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
    private fun drawScrollbar(matrices: MatrixStack, delta: Float, mouseX: Int, mouseY: Int) {
        client?.textureManager?.bindTexture(TEXTURE_TABS)
        this.drawTexture(
                matrices,
                291,
                36 + (73.toFloat() * scrollPosition).toInt(),
                232 + if (hasScrollbar()) 0 else 12,
                0,
                12,
                15
        )
    }
    private fun hasScrollbar(): Boolean = handler.limbs?.size ?: 0 > INV_SIZE
}