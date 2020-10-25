package com.unrelentless.mess.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.entity.EyeOfEnderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EyeOfEnderEntity.class)
public interface EyeOfEnderEntityMixin {

    @Accessor("dropsItem")
    public void setDropsItem(boolean dropsItem);
}