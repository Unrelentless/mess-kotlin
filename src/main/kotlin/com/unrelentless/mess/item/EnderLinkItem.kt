package com.unrelentless.mess.item

import com.unrelentless.mess.Mess
import com.unrelentless.mess.client.gui.screen.MessScreen
import com.unrelentless.mess.entity.EnderLinkEntity
import com.unrelentless.mess.mixin.EyeOfEnderEntityAccessor
import com.unrelentless.mess.settings.enderLinkItemSettings
import com.unrelentless.mess.util.deserializeBlockPos
import com.unrelentless.mess.util.registerItem
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World


class EnderLinkItem : Item(enderLinkItemSettings) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "portable_mess")
        val ITEM = registerItem(EnderLinkItem(), IDENTIFIER)
    }

    override fun hasGlint(stack: ItemStack?): Boolean = true
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        if(world.isClient) return TypedActionResult.fail(user.getStackInHand(hand))
        return doTheThing(world, user)
    }


    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        stack.deserializeBlockPos()?.let {
            tooltip.add(Text.of(it.toShortString() ?: "Not linked"))
        }
    }

    private fun doTheThing(
            world: World,
            player: PlayerEntity
    ): TypedActionResult<ItemStack> {
        val itemStack = player.mainHandStack
        val blockPos = itemStack.deserializeBlockPos() ?: return TypedActionResult.fail(itemStack)

        if (world is ServerWorld) {
            val enderLinkEntity = EnderLinkEntity(world, player.x, player.getBodyY(0.5), player.z)
            enderLinkEntity.setItem(itemStack)
            enderLinkEntity.initTargetPos(blockPos)
            (enderLinkEntity as EyeOfEnderEntityAccessor).setDropsItem(false)
            enderLinkEntity.handler = EnderLinkEntity.OnDestroyHandler {
                MessScreen.openScreen(world, blockPos, player)
            }

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

            if (!player.abilities.creativeMode) {
                itemStack.decrement(1)
            }

            player.swingHand(player.activeHand, true)
            return TypedActionResult.success(itemStack)
        }

        return TypedActionResult.consume(itemStack)
    }
}