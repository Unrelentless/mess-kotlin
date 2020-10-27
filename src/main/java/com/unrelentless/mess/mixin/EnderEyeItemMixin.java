package com.unrelentless.mess.mixin;

import com.unrelentless.mess.block.HeartBlock;
import com.unrelentless.mess.block.entity.HeartBlockEntity;
import com.unrelentless.mess.item.EnderLinkItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {

    @Inject(at = @At("RETURN"), method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;", cancellable = true)
    public void injectUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        ItemStack stack = context.getStack();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);

        if(!(blockState.getBlock() instanceof HeartBlock) || !player.isSneaking()) {
            cir.setReturnValue(cir.getReturnValue());
            return;
        }

        if(!world.isClient) {
            ItemStack linkedStack = new ItemStack(EnderLinkItem.Companion.getITEM(), stack.getCount());
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", blockPos.getX());
            tag.putInt("y", blockPos.getY());
            tag.putInt("z", blockPos.getZ());

            linkedStack.putSubTag("heart", tag);
            player.setStackInHand(context.getHand(), linkedStack);

            ((HeartBlockEntity) world.getBlockEntity(blockPos)).chunkLoad(true);
        }

        cir.setReturnValue(ActionResult.CONSUME);
    }

    @Inject(at = @At("HEAD"), method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;", cancellable = true)
    public void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack itemStack = user.getStackInHand(hand);
        HitResult hitResult = ItemRaycastInvoker.invokeRaycast(world, user, RaycastContext.FluidHandling.NONE);

        if (hitResult.getType() == HitResult.Type.BLOCK
                && world.getBlockState(((BlockHitResult)hitResult).getBlockPos()).isOf(HeartBlock.Companion.getBLOCK())) {
            cir.setReturnValue(TypedActionResult.pass(itemStack));
        }
    }
}
