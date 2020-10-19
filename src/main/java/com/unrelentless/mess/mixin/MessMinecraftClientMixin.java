package com.unrelentless.mess.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MessMinecraftClientMixin {

    @Accessor("itemColors")
    public ItemColors getItemColors();
}