package com.unrelentless.mess.util

import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun registerBlock(
    block: Block,
    id: Identifier
): Block =
    Registry.register(Registry.BLOCK, id, block)

fun registerBlockItem(
    block: Block,
    id: Identifier,
    settings: Item.Settings = Item.Settings().group(ItemGroup.MISC)
): BlockItem = Registry.register(Registry.ITEM, id, BlockItem(block, settings))

//fun <T : BlockEntity> registerBlockEntity(
//    id: Identifier,
//    blockEntityType: () -> BlockEntityType<T>
//): BlockEntityType<T> = Registry.register(Registry.BLOCK_ENTITY_TYPE, id, blockEntityType())