package com.unrelentless.mess.item

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.HeartBlock
import com.unrelentless.mess.settings.messItemSettings
import com.unrelentless.mess.util.registerItem
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class PortableMessItem : Item(messItemSettings) {

    companion object {
        val IDENTIFIER = Identifier(Mess.IDENTIFIER, "portable_mess")
        val ITEM = registerItem(PortableMessItem(), IDENTIFIER)
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        context.player?.let {player ->
            val blockPos = context.blockPos
            if(player.isSneaking) {
                val tag = CompoundTag()
                tag.putInt("x", blockPos.x)
                tag.putInt("y", blockPos.y)
                tag.putInt("z", blockPos.z)

                player.mainHandStack.putSubTag("heart", tag)

                return ActionResult.SUCCESS
            }
        }

        return super.useOnBlock(context)
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        if(world.isClient) return TypedActionResult.fail(user.getStackInHand(hand))

        deserializeBlockPosFromStack(user.getStackInHand(hand))?.let { it ->
            HeartBlock.openScreen(world.getBlockState(it), world, it, user)
        }

        return TypedActionResult.success(user.getStackInHand(hand))
    }

    override fun hasGlint(stack: ItemStack?): Boolean {
        return stack?.getSubTag("heart") != null
    }

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        deserializeBlockPosFromStack(stack)?.let {
            tooltip.add(LiteralText(it.toShortString() ?: "Not linked"))
        }
    }

    private fun deserializeBlockPosFromStack(stack: ItemStack): BlockPos? {
        val heartTag = stack.getSubTag("heart") ?: return null
        return BlockPos(
                heartTag.getInt("x"),
                heartTag.getInt("y"),
                heartTag.getInt("z")
        )
    }
}