package com.unrelentless.mess.item

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.HeartBlock
import com.unrelentless.mess.mixin.EyeOfEnderEntityMixin
import com.unrelentless.mess.settings.enderLinkItemSettings
import com.unrelentless.mess.util.deserializeBlockPos
import com.unrelentless.mess.util.registerItem
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.EyeOfEnderEntity
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

        return doTheThing(world, user, hand)
        user.getStackInHand(hand).deserializeBlockPos()?.let { it ->
            HeartBlock.openScreen(world.getBlockState(it), world, it, user)
        }

        return TypedActionResult.success(user.getStackInHand(hand))
    }


    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        stack.deserializeBlockPos()?.let {
            tooltip.add(Text.of(it.toShortString() ?: "Not linked"))
        }
    }

    private fun doTheThing(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val itemStack: ItemStack = user.getStackInHand(hand)

        if (world is ServerWorld) {
            val blockPos = itemStack.deserializeBlockPos()
            if (blockPos != null) {
                val eyeOfEnderEntity = EyeOfEnderEntity(world, user.x, user.getBodyY(0.5), user.z)
                eyeOfEnderEntity.setItem(itemStack)
                eyeOfEnderEntity.initTargetPos(blockPos)
                (eyeOfEnderEntity as EyeOfEnderEntityMixin).setDropsItem(false)

                world.spawnEntity(eyeOfEnderEntity)
                world.playSound(null,
                        user.x,
                        user.y,
                        user.z,
                        SoundEvents.ENTITY_ENDER_EYE_LAUNCH,
                        SoundCategory.NEUTRAL,
                        0.5f,
                        0.4f / (RANDOM.nextFloat() * 0.4f + 0.8f))

                if (!user.abilities.creativeMode) {
                    itemStack.decrement(1)
                }

                user.swingHand(hand, true)
                return TypedActionResult.success(itemStack)
            }
        }

        return TypedActionResult.consume(itemStack)
    }
}