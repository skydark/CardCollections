package me.skydark.card_collections.proxy;

import me.skydark.card_collections.data.CardData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public interface IProxy {
    void openCardGui(CardData cardData);
    void openCollectionBookGui(PlayerEntity playerIn, ItemStack handStack, Hand handIn);
}
