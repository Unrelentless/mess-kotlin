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
import java.lang.Integer.min


class MessScreen(
        handler: MessScreenHandler?,
        inventory: PlayerInventory?,
        title: Text?
) : HandledScreen<MessScreenHandler>(handler, inventory, title) {

    companion object: Clientside {
        val TEXTURE = Identifier(Mess.IDENTIFIER, "textures/gui/heart_blank.png")
        val TEXTURE_SLOT = Identifier(Mess.IDENTIFIER, "textures/gui/heart_blank.png")

        override fun renderOnClient() {
            ScreenRegistry.register(MessScreenHandler.HANDLER_TYPE, ::MessScreen)
        }
    }

    private val slotSize = 18
    private val slotOriginX = 238
    private val slotOriginY = 0

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
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    private fun drawSlots(matrices: MatrixStack) {
        client?.textureManager?.bindTexture(TEXTURE_SLOT)

        val totalItemsToShow = handler.limbs?.size ?: 0

        val xPos = (width - backgroundWidth) / 2 + 8
        val yPos = (height - backgroundHeight) / 2 + 17

        val maxColumns = 9
        val maxRows = 5
        val rowTotal = min(1 + totalItemsToShow / 9, maxRows)

        for (row in 0 until rowTotal) {
            val columnMax = min(totalItemsToShow - maxColumns * row, maxColumns)

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
}