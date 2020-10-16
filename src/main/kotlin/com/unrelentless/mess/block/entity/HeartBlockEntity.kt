package com.unrelentless.mess.block.entity

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.HeartBlock
import com.unrelentless.mess.client.gui.screen.MessScreenHandler
import com.unrelentless.mess.util.LimbInventory
import com.unrelentless.mess.util.registerBlockEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*
import java.util.function.Supplier


class HeartBlockEntity: BlockEntity(ENTITY_TYPE), NamedScreenHandlerFactory {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "heart_entity")
        val ENTITY_TYPE: BlockEntityType<HeartBlockEntity> = registerBlockEntity(IDENTIFIER) {
            BlockEntityType.Builder
                    .create(Supplier { HeartBlockEntity() }, HeartBlock.BLOCK)
                    .build(null)
        }
    }

    override fun createMenu(
            syncId: Int,
            playerInventory: PlayerInventory,
            player: PlayerEntity
    ): ScreenHandler? = MessScreenHandler(
            syncId,
            playerInventory,
            Array<Inventory>(9) {LimbInventory(8, null)}
//            findLimbs(world, pos, null)?.map(LimbBlockEntity::inventory).toTypedArray()
    )

    override fun getDisplayName(): Text = TranslatableText("container." + Mess.IDENTIFIER + ".mess");

    private fun findLimbs(world: World?, pos: BlockPos, oldSet: HashSet<LimbBlockEntity>?): HashSet<LimbBlockEntity> {
        val set = oldSet ?: hashSetOf()

        val posArray = arrayOf(
                BlockPos(1, 0, 0),
                BlockPos(0, 1, 0),
                BlockPos(0, 0, 1),
                BlockPos(-1, 0, 0),
                BlockPos(0, -1, 0),
                BlockPos(0, 0, -1)
        )

        // Recursion? - Why not!
        for (i in posArray.indices) {
            val nextPos = pos.add(posArray[i])
            val nextBlock = world?.getBlockEntity(nextPos)
            if (nextBlock is LimbBlockEntity && !set.contains(nextBlock)) {
                set.add(nextBlock)
                findLimbs(world, nextPos, set)
            }
        }

        return set
    }
}