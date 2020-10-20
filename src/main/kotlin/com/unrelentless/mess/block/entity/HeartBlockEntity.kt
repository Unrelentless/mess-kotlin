package com.unrelentless.mess.block.entity

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.HeartBlock
import com.unrelentless.mess.client.gui.screen.MessScreenHandler
import com.unrelentless.mess.util.registerBlockEntity
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import java.util.function.Supplier


class HeartBlockEntity: BlockEntity(ENTITY_TYPE), ExtendedScreenHandlerFactory {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "heart_entity")
        val ENTITY_TYPE: BlockEntityType<HeartBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                    .create(Supplier { HeartBlockEntity() }, HeartBlock.BLOCK)
                    .build(null)
        }
    }

    private var limbs: Array<LimbBlockEntity>? = null

    override fun createMenu(
            syncId: Int,
            playerInventory: PlayerInventory,
            player: PlayerEntity
    ): ScreenHandler? = MessScreenHandler(
            syncId,
            playerInventory,
            limbs?.map(LimbBlockEntity::inventory)?.toTypedArray() ?: emptyArray()
    )

    override fun getDisplayName(): Text = TranslatableText("container." + Mess.IDENTIFIER + ".mess")

    override fun writeScreenOpeningData(serverPlayerEntity: ServerPlayerEntity?, packetByteBuf: PacketByteBuf?) {
        val sizes = limbs?.map{it.size}?.toIntArray()
        packetByteBuf?.writeIntArray(sizes)
    }

    fun setLimbs(limbs: Array<LimbBlockEntity>?) {
        this.limbs = limbs
    }
}