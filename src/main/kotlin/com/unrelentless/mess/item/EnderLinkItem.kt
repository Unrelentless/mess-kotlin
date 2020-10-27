package com.unrelentless.mess.item

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.entity.BrainBlockEntity
import com.unrelentless.mess.client.gui.screen.MessScreen
import com.unrelentless.mess.entity.EnderLinkEntity
import com.unrelentless.mess.mixin.EyeOfEnderEntityAccessor
import com.unrelentless.mess.settings.enderLinkItemSettings
import com.unrelentless.mess.util.deserializeBrainBlockPos
import com.unrelentless.mess.util.registerItem
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


class EnderLinkItem : Item(enderLinkItemSettings) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "ender_link")
        val ITEM = registerItem(EnderLinkItem(), IDENTIFIER)
    }

    override fun hasGlint(stack: ItemStack?): Boolean = true
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        return if(world.isClient)
            TypedActionResult.fail(user.getStackInHand(hand))
        else
            openScreenIfPossible(world, user)
    }

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        stack.deserializeBrainBlockPos()?.let {
            tooltip.add(Text.of(it.toShortString() ?: "Not linked"))
        }
    }

    private fun openScreenIfPossible(
            world: World,
            player: PlayerEntity
    ): TypedActionResult<ItemStack> {
        if(world.isClient) return TypedActionResult.consume(player.mainHandStack)

        val itemStack = player.mainHandStack
        val blockPos = itemStack.deserializeBrainBlockPos() ?: return TypedActionResult.fail(itemStack)

        spawnEnderEntity(world, player, itemStack, blockPos) {
            if(world.getBlockEntity(blockPos) !is BrainBlockEntity) {
                player.inventory.main.forEachIndexed { index, itemStack ->
                    if(itemStack.item == ITEM && itemStack.deserializeBrainBlockPos() == blockPos) {
                        player.inventory.main[index] = ItemStack(Items.ENDER_EYE, itemStack.count)
                        player.inventory.markDirty()
                    }
                }

                player.sendMessage(TranslatableText("message." + Mess.IDENTIFIER + ".link_broken"), false)
            } else{
                MessScreen.openScreen(world, blockPos, player)
            }
        }

        if (!player.abilities.creativeMode) itemStack.decrement(1)
        player.swingHand(player.activeHand, true)

        return TypedActionResult.success(itemStack)
    }

    private fun spawnEnderEntity(
            world: World,
            player: PlayerEntity,
            itemStack: ItemStack,
            blockPos: BlockPos,
            handler: EnderLinkEntity.OnDestroyHandler
    ) {
        val enderLinkEntity = EnderLinkEntity(world, player.x, player.getBodyY(0.5), player.z, handler)
        enderLinkEntity.setItem(itemStack)
        enderLinkEntity.initTargetPos(blockPos)
        (enderLinkEntity as EyeOfEnderEntityAccessor).setDropsItem(false)

        world.spawnEntity(enderLinkEntity)
        world.playSound(
                null,
                player.x,
                player.y,
                player.z,
                SoundEvents.ENTITY_ENDER_EYE_LAUNCH,
                SoundCategory.NEUTRAL,
                0.5f,
                0.4f / (RANDOM.nextFloat() * 0.4f + 0.8f)
        )
    }
}