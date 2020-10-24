package com.unrelentless.mess.item

import com.unrelentless.mess.Mess
import com.unrelentless.mess.block.HeartBlock
import com.unrelentless.mess.block.entity.HeartBlockEntity
import com.unrelentless.mess.settings.messItemSettings
import com.unrelentless.mess.util.registerItem
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.CompoundTag
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
        if(context.world.isClient) return super.useOnBlock(context)

        (context.world.getBlockEntity(context.blockPos) as? HeartBlockEntity)?.let {heart ->
            context.player?.let { player ->
                if (player.isSneaking) {
                    linkHeart(
                            context.stack.getSubTag("heart") != null,
                            context.world,
                            heart,
                            context.blockPos,
                            player,
                            context.stack
                    )
                }
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
            tooltip.add(Text.of(it.toShortString() ?: "Not linked"))
        }
    }

    private fun linkHeart(
            linked: Boolean,
            world: World,
            heart: HeartBlockEntity,
            blockPos: BlockPos,
            playerEntity: PlayerEntity,
            stack: ItemStack
    ) {
        if(linked) {
            deserializeBlockPosFromStack(stack)?.let {
                (world.getBlockEntity(blockPos) as? HeartBlockEntity)?.let { oldHeart ->
                    stack.removeSubTag("heart")
                    oldHeart.chunkLoad(false)
                }
            }
        }

        val tag = CompoundTag()
        tag.putInt("x", blockPos.x)
        tag.putInt("y", blockPos.y)
        tag.putInt("z", blockPos.z)

        playerEntity.mainHandStack.putSubTag("heart", tag)
        heart.chunkLoad(true)
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